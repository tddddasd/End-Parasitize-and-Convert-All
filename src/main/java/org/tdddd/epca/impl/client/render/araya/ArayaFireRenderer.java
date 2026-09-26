package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the render-only vanilla fire blocks the aura spawns around its holder.
 *
 * <h2>It really is the vanilla fire block</h2>
 * <p>The block itself is never placed. The renderer asks the vanilla {@code minecraft:fire} block's own
 * {@link BlockStateModel} for its parts and their {@link BakedQuad}s, and submits those very quads through
 * {@code VertexConsumer#putBakedQuad} - the same data the chunk renderer would submit. The only thing this
 * renderer decides is the pose: the quad's block-local position, scaled by the fire's height, moved to the
 * fire's position. The texture is the vanilla animated fire sprite (each quad carries its own
 * {@code MaterialInfo#sprite()}), so the animation is vanilla and costs nothing extra. There is no copied
 * model JSON in this mod that could drift from vanilla.</p>
 *
 * <h2>Render only</h2>
 * <p>Nothing is written into the level: the server sends positions, this draws pictures. No block is
 * placed, no light is emitted, no entity is hurt and no fire particle exists - a fire particle belongs to
 * the fire block, and there is no fire block.</p>
 *
 * <h2>Lifetime</h2>
 * <p>Height and lifetime come from the server batch ({@code SyncArayaFirePacket}) and the cache drops a
 * fire once its lifetime is over; this renderer only reads. The fade over
 * {@code ArayaClientCache.FADE_TICKS} stops a re-rolled field from popping in or out.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>The 1.20.1 twin handed the vanilla {@code BlockRenderDispatcher#renderBatched} a {@code PoseStack}
 * and let it do everything. 26.1.2 has no such method on this path: the block model is a
 * {@code BlockStateModel} whose {@code collectParts} fills a list of {@code BlockStateModelPart}s, and a
 * quad is submitted with {@code putBakedQuad(PoseStack.Pose, BakedQuad, QuadInstance)}. Because the quads
 * are already in block-local 0..1 coordinates, the height scale is applied by the pose rather than by
 * rewriting vertex data. Full brightness is {@code Brightness.FULL_BRIGHT.pack()}: the
 * {@code LightTexture} constant the 1.20.1 twin used does not exist here.</p>
 */
public final class ArayaFireRenderer {

    private static final BlockState FIRE_STATE = Blocks.FIRE.defaultBlockState();

    /** Quads of the vanilla fire model, flattened once so the per-frame loop does no allocation. */
    private static List<BakedQuad> cachedQuads;

    private ArayaFireRenderer() {
    }

    /**
     * Entry point, from the level-stage listener at
     * {@code RenderLevelStageEvent.AfterTranslucentParticles}.
     */
    public static void renderGeometry(RenderLevelStageEvent.AfterTranslucentParticles event) {
        if (!ArayaClientCache.hasFires()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RenderType renderType = ArayaFireRenderType.get();
        if (renderType == null) {
            // The pipeline is not registered yet; drawing with it would be worse than one frame without
            // the fires.
            return;
        }
        if (!ensureModel(minecraft)) {
            return;
        }

        long gameTime = level.getGameTime();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(renderType);
        QuadInstance quadInstance = new QuadInstance();
        // Full brightness: a fire is emissive, and the world light around it is not its business.
        quadInstance.setLightCoords(Brightness.FULL_BRIGHT.pack());

        try {
            for (ArayaClientCache.Fire fire : ArayaClientCache.fires()) {
                float opacity = fire.opacity(gameTime);
                if (opacity <= 0.01F) {
                    continue;
                }
                double x = fire.position().getX() - camera.x;
                double y = fire.position().getY() - camera.y;
                double z = fire.position().getZ() - camera.z;
                if (x * x + y * y + z * z
                        > ArayaConstants.RENDER_DISTANCE * ArayaConstants.RENDER_DISTANCE) {
                    continue;
                }
                // The fade is part of the vertex colour; the pipeline blends it straight through.
                int alpha = Math.max(1, Math.min(255, Math.round(opacity * 255.0F)));
                quadInstance.setColor((alpha << 24) | 0x00FFFFFF);
                float scale = fire.height();
                poseStack.pushPose();
                try {
                    poseStack.translate(x, y, z);
                    poseStack.scale(scale, scale, scale);
                    PoseStack.Pose pose = poseStack.last();
                    for (BakedQuad quad : cachedQuads) {
                        consumer.putBakedQuad(pose, quad, quadInstance);
                    }
                } finally {
                    poseStack.popPose();
                }
            }
        } finally {
            buffers.endBatch(renderType);
        }
    }

    /**
     * Resolves the vanilla fire model's quads once.
     *
     * <p>The model is asked for through the model manager for the block's own default state, so whatever
     * resource pack is loaded is what gets drawn.</p>
     *
     * @return true when the quads are available
     */
    private static boolean ensureModel(Minecraft minecraft) {
        if (cachedQuads != null) {
            return true;
        }
        try {
            BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(FIRE_STATE);
            if (model == null) {
                return false;
            }
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(42L), parts);
            if (parts.isEmpty()) {
                return false;
            }
            List<BakedQuad> quads = new ArrayList<>();
            for (BlockStateModelPart part : parts) {
                quads.addAll(part.getQuads(null));
                for (Direction direction : Direction.values()) {
                    quads.addAll(part.getQuads(direction));
                }
            }
            if (quads.isEmpty()) {
                return false;
            }
            cachedQuads = quads;
            return true;
        } catch (Throwable throwable) {
            // A resource reload in the middle of a frame can leave the model temporarily unavailable; the
            // next frame tries again rather than drawing garbage.
            return false;
        }
    }
}
