package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3fc;

/**
 * Turns one of the <b>item's own baked quads</b> into the overlay's vertex stream.
 *
 * <h2>Why the geometry comes from the item, not from a constant</h2>
 * The first working revision emitted a hand-built plane in a fixed unit space (x/y 0..16, z = 8 +/- 0.5).
 * That rendered <b>too large and in the wrong place</b>: the space a quad has to be expressed in depends on
 * the pose the emit hook is handed, which is the item's post-display-transform pose, and the item's real
 * geometry does not have to be a 16x16 box at z = 8 at all (block models, per-layer local transforms,
 * custom model shapes and the display-context transform all change it). Any new constant would have been
 * another guess.
 *
 * <p>So the overlay is emitted from the item's own quads instead: <b>same positions, same UVs</b>, through
 * the corruption render type, under the same pose the item used. Size and placement then match by
 * construction in every display context, because it is literally the item's geometry.</p>
 *
 * <h2>Reading the quads</h2>
 * Verified with {@code javap -p -s -c} on the 26.1.2 patched jar:
 * <ul>
 *   <li>{@code ItemStackRenderState$LayerRenderState} is a <b>public</b> class with
 *       {@code private final List<BakedQuad> quads} and a <b>public</b>
 *       {@code List<BakedQuad> prepareQuadList()};</li>
 *   <li>its {@code submit} reads that same {@code quads} field (bytecode offset 101) and passes it to
 *       {@code SubmitNodeCollector#submitItem} (offset 108) <em>before</em> the {@code popPose()} the emit
 *       hook is anchored to, so the list the hook sees is exactly the list the item was drawn from;</li>
 *   <li>{@code BakedQuad} exposes {@code position(int) -> Vector3fc}, {@code packedUV(int) -> long} (unpacked
 *       with {@code UVPair.unpackU/unpackV}) and {@code materialInfo()}, whose {@code sprite()} gives the
 *       atlas rect the quad's UVs are relative to.</li>
 * </ul>
 *
 * <h2>UV mapping</h2>
 * The quad's UVs are <b>atlas</b> coordinates (the model was baked against the stitched sprite), while the
 * mask is bound as a directly loaded texture. So each UV is first normalised into the quad's own sprite rect
 * ({@code (uv - spriteMin) / (spriteMax - spriteMin)}), which maps it onto the raw texture's 0..1 space, and
 * then V is banded into the current animation frame. Both steps are required: without the normalisation the
 * sample would land wherever the sprite sits in the atlas, and without the banding a multi-frame strip would
 * smear (the shipped mask, {@code ender_blade_scrap.png}, is 16x384 = 24 frames).
 *
 * <p>The normalisation assumes the sprite rect covers exactly <b>one</b> frame, which is how atlas animation
 * works in vanilla: the sprite keeps a frame-sized rect and the animation uploads successive frames into it.
 * If that assumption ever failed, the visible symptom would be V being compressed rather than smeared, and
 * {@code frames == 1} saves the single-frame case.</p>
 */
public final class ItemShaderBakery {

    /** Floats per vertex: x, y, z, u, v. */
    public static final int VERTEX_STRIDE = 5;
    /** Vertices per quad. */
    public static final int VERTICES_PER_QUAD = 4;

    /**
     * Coplanar-drawing nudge: vertex positions are scaled by this about the quad's centroid, i.e. pushed
     * about 0.1% outwards. The pipeline draws with {@code LESS_THAN_OR_EQUAL} and no depth write, so coplanar
     * geometry normally wins, but a sub-pixel inflation removes any z-fighting risk on drivers that resolve
     * equal depths differently. In model units a 16-unit quad moves by ~0.008 units = 0.0005 block, which is
     * far below one pixel at any normal view distance.
     */
    public static final float OVERLAY_INFLATE = 1.001f;

    private ItemShaderBakery() {
    }

    /**
     * Writes one of the item's quads as an overlay quad.
     *
     * <p>The vertices are the quad's own positions, transformed by {@code pose} exactly as the item's were, so
     * the overlay lands on the item regardless of the display context.</p>
     *
     * @param consumer    the sink handed to the custom-geometry callback
     * @param pose        the item's pose at the emit hook (the same one the item was submitted with)
     * @param quad        one of the item's baked quads
     * @param frames      animation frames in the mask strip (&gt;= 1)
     * @param frame       the frame to show, 0-based, already wrapped
     * @param tintR       corruption tint red
     * @param tintG       corruption tint green
     * @param tintB       corruption tint blue
     * @param intensity16 decay strength, 16-bit fixed point
     * @param split16     RGB split strength, 16-bit fixed point
     * @param animClock   the animation clock in game ticks
     */
    public static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                int frames, int frame,
                                float tintR, float tintG, float tintB,
                                int intensity16, int split16, float animClock) {
        if (quad == null) {
            return;
        }

        // The quad's UVs are atlas coordinates; find the rect they are relative to.
        float uMin = 0.0f;
        float vMin = 0.0f;
        float uSpan = 1.0f;
        float vSpan = 1.0f;
        BakedQuad.MaterialInfo material = quad.materialInfo();
        TextureAtlasSprite sprite = material == null ? null : material.sprite();
        if (sprite != null) {
            float su = sprite.getU0();
            float sv = sprite.getV0();
            float du = sprite.getU1() - su;
            float dv = sprite.getV1() - sv;
            if (du > 1.0e-6f && dv > 1.0e-6f) {
                uMin = su;
                vMin = sv;
                uSpan = du;
                vSpan = dv;
            }
        }

        int safeFrames = Math.max(1, frames);
        float vScale = 1.0f / safeFrames;
        float vOffset = Math.max(0, frame) * vScale;

        // Centroid, for the tiny outward inflation.
        float cx = 0.0f;
        float cy = 0.0f;
        float cz = 0.0f;
        for (int i = 0; i < VERTICES_PER_QUAD; i++) {
            Vector3fc p = quad.position(i);
            cx += p.x();
            cy += p.y();
            cz += p.z();
        }
        cx /= VERTICES_PER_QUAD;
        cy /= VERTICES_PER_QUAD;
        cz /= VERTICES_PER_QUAD;

        for (int i = 0; i < VERTICES_PER_QUAD; i++) {
            Vector3fc p = quad.position(i);
            long packed = quad.packedUV(i);

            // Atlas UV -> the raw texture's own 0..1 space -> the current frame band.
            float u = (UVPair.unpackU(packed) - uMin) / uSpan;
            float v = (UVPair.unpackV(packed) - vMin) / vSpan;

            float x = cx + (p.x() - cx) * OVERLAY_INFLATE;
            float y = cy + (p.y() - cy) * OVERLAY_INFLATE;
            float z = cz + (p.z() - cz) * OVERLAY_INFLATE;

            // Element order and writers must match ITEM_LAYER_VERTEX_FORMAT exactly: Position (addVertex
            // applies the pose), Color, Uv, Params (UV1), AnimClock (LINE_WIDTH). Every declared element is
            // written, which 26.1.2 requires.
            consumer.addVertex(pose, x, y, z)
                    .setColor(tintR, tintG, tintB, 1.0f)
                    .setUv(u, vOffset + v * vScale)
                    .setUv1(intensity16, split16)
                    .setLineWidth(animClock);
        }
    }
}
