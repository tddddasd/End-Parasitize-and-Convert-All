package org.tdddd.epca.impl.client.render;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Builds the flat item plane an item shader layer is drawn as, and caches it per sprite.
 *
 * <h2>1.20.1 -&gt; 26.1.2: this is a rewrite, not a port</h2>
 * The 1.20.1 twin baked the plane with
 * {@code ItemModelGenerator#processFrames(0, "layer0", sprite.contents())} plus
 * {@code FaceBakery#bakeQuad(...)}, both in {@code net.minecraft.client.renderer.block.model}.
 * <b>Neither class exists in 26.1.2</b> (verified against the class list of the patched jar), so that
 * code cannot be ported.
 *
 * <p>What 26.1.2 offers instead is a {@link BakedQuad} that is now a <b>record with a public
 * constructor</b>, plus the public {@link UVPair#pack(float, float)} for its UV longs and a public
 * {@code BakedQuad.MaterialInfo} record. The plane is therefore built directly:</p>
 * <ul>
 *   <li>four corners of the item's unit quad in item-model space, which is 1/16 block units, spanning
 *       x 0..16 and y 0..16 with a thin depth slab around z = 8 - the same box
 *       {@code ItemModelGenerator} produced for a flat 2D item texture;</li>
 *   <li>the four UVs are the mask sprite's own rectangle, so the layer samples exactly the item's
 *       silhouette;</li>
 *   <li>{@code MaterialInfo} only has to be non-null and to report {@code lightEmission() == 0}:
 *       it is the sole member {@code VertexConsumer#putBakedQuad} reads.</li>
 * </ul>
 *
 * <p><b>Documented approximation:</b> because the generator is gone, a 3D/block item gets this flat
 * billboard plane rather than a shell around its real geometry. For the flat items this layer is used
 * on (the shipped binding is {@code epca:ender_blade_scrap}, a flat sprite) the result is identical;
 * for a block item the decay would cover its silhouette rather than wrap it.</p>
 *
 * <p>The cache is keyed weakly by sprite, so an atlas reload naturally drops the stale entries.</p>
 */
public final class ItemShaderBakery {

    /** Half-thickness of the plane in item-model units, so it does not z-fight with the item. */
    private static final float PLANE_HALF_DEPTH = 0.5f;
    /** Centre of the plane along z in item-model units (8 = the middle of the 16-unit item box). */
    private static final float PLANE_Z = 8.0f;

    private static final Map<TextureAtlasSprite, List<BakedQuad>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ItemShaderBakery() {
    }

    /**
     * The (cached) flat quads for one mask sprite. The returned list is unmodifiable and safe to reuse
     * on the render thread.
     *
     * @param sprite      the mask sprite
     * @param renderType  what the quads will be drawn with; only used to fill
     *                    {@code MaterialInfo.itemRenderType()}, which {@code putBakedQuad} ignores
     */
    public static List<BakedQuad> quads(TextureAtlasSprite sprite, RenderType renderType) {
        if (sprite == null) {
            return List.of();
        }
        List<BakedQuad> cached = CACHE.get(sprite);
        if (cached != null) {
            return cached;
        }
        List<BakedQuad> baked = Collections.unmodifiableList(bake(sprite, renderType));
        CACHE.put(sprite, baked);
        return baked;
    }

    /** Clears the cache (atlas reload or debugging). */
    public static void invalidate() {
        CACHE.clear();
    }

    private static List<BakedQuad> bake(TextureAtlasSprite sprite, RenderType renderType) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        float z0 = PLANE_Z - PLANE_HALF_DEPTH;
        float z1 = PLANE_Z + PLANE_HALF_DEPTH;

        // Counter-clockwise when seen from -Z, so the quad faces the viewer in the item's own space.
        Vector3f p0 = new Vector3f(0.0f, 0.0f, z1);
        Vector3f p1 = new Vector3f(16.0f, 0.0f, z1);
        Vector3f p2 = new Vector3f(16.0f, 16.0f, z1);
        Vector3f p3 = new Vector3f(0.0f, 16.0f, z1);

        // The vertex order maps to UVs as (bottom-left, bottom-right, top-right, top-left).
        long uv0 = UVPair.pack(u0, v1);
        long uv1 = UVPair.pack(u1, v1);
        long uv2 = UVPair.pack(u1, v0);
        long uv3 = UVPair.pack(u0, v0);

        // ChunkSectionLayer is only metadata here; TRANSLUCENT matches the pipeline's blend state.
        BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(
                sprite, ChunkSectionLayer.TRANSLUCENT, renderType,
                0,      // tintIndex: no biome tint
                false,  // shade: unlit, the shader does its own lighting
                0);     // lightEmission: putBakedQuad folds this into the light coords

        BakedQuad quad = new BakedQuad(p0, p1, p2, p3, uv0, uv1, uv2, uv3,
                Direction.SOUTH, material);
        return List.of(quad, mirror(quad, sprite, renderType));
    }

    /**
     * The back face, so the layer is visible from both sides. The item pipeline relies on culling
     * being off, but a single quad would still be invisible from behind if the driver keeps
     * back-face culling for this pipeline, so a mirrored copy is emitted. Cheap (two quads) and it
     * removes a class of "the effect disappeared" reports.
     */
    private static BakedQuad mirror(BakedQuad quad, TextureAtlasSprite sprite, RenderType renderType) {
        float z0 = PLANE_Z - PLANE_HALF_DEPTH;
        float z1 = PLANE_Z + PLANE_HALF_DEPTH;

        Vector3f p0 = new Vector3f(0.0f, 0.0f, z0);
        Vector3f p1 = new Vector3f(0.0f, 16.0f, z0);
        Vector3f p2 = new Vector3f(16.0f, 16.0f, z0);
        Vector3f p3 = new Vector3f(16.0f, 0.0f, z0);

        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        long uv0 = UVPair.pack(u0, v1);
        long uv1 = UVPair.pack(u0, v0);
        long uv2 = UVPair.pack(u1, v0);
        long uv3 = UVPair.pack(u1, v1);

        BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(
                sprite, ChunkSectionLayer.TRANSLUCENT, renderType, 0, false, 0);
        return new BakedQuad(p0, p1, p2, p3, uv0, uv1, uv2, uv3, Direction.NORTH, material);
    }
}
