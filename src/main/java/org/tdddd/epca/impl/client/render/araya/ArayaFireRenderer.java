package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;

/**
 * Draws the render-only vanilla fire blocks the aura spawns around its holder.
 *
 * <h2>It really is the vanilla fire block</h2>
 * <p>The block itself is never placed. The renderer asks the vanilla {@code minecraft:fire} block's own
 * baked model - through {@code BlockRenderDispatcher#renderBatched}, the exact call the vanilla chunk
 * renderer makes - and supplies only the pose: translated to the fire's position and scaled so the model
 * is {@code FIRE_MIN_HEIGHT..FIRE_MAX_HEIGHT} blocks tall ("高度为0.8~1.5格"). The texture is the vanilla
 * animated fire sprite out of the block atlas, so the animation is vanilla and costs nothing extra.
 * There is no copied model JSON in this mod that could drift from vanilla.</p>
 *
 * <h2>Render only</h2>
 * <p>Nothing is written into the level: the server sends positions, this draws pictures. No block is
 * placed, no light is emitted, no entity is hurt and no fire particle exists - a fire particle belongs
 * to the fire block, and there is no fire block. The render type is vanilla cutout geometry with culling
 * off and a constant-alpha blend, so the fade is visible (see {@link ArayaFireRenderType}).</p>
 *
 * <h2>Lifetime</h2>
 * <p>Height and lifetime come from the server batch ({@code SyncArayaFirePacket}) and the cache drops a
 * fire once its lifetime is over; this renderer only reads. The fade over
 * {@code ArayaClientCache.FADE_TICKS} stops a re-rolled field from popping in or out.</p>
 */
public final class ArayaFireRenderer {

    private static final BlockState FIRE_STATE = Blocks.FIRE.defaultBlockState();

    private ArayaFireRenderer() {
    }

    /** Entry point, from the level-stage listener at {@code AFTER_PARTICLES}. */
    public static void renderGeometry(RenderLevelStageEvent event) {
        if (!ArayaClientCache.hasFires()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        RandomSource random = RandomSource.create(42L);

        // The render type depends on the fire's opacity, so fires are grouped by it: consecutive fires
        // of the same step share one batch, and the batch is flushed as soon as the step changes.
        RenderType current = null;
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
            RenderType wanted = ArayaFireRenderType.forOpacity(opacity);
            if (wanted != current) {
                if (current != null) {
                    minecraft.renderBuffers().bufferSource().endBatch(current);
                }
                current = wanted;
            }

            poseStack.pushPose();
            try {
                poseStack.translate(x, y, z);
                float scale = fire.height();
                // The model's own height is 1.4375 blocks; scaling x, y and z by the requested height
                // keeps the cross's proportions and lands the top at exactly that many blocks.
                poseStack.scale(scale, scale, scale);
                dispatcher.renderBatched(FIRE_STATE, fire.position(), minecraft.level, poseStack,
                        minecraft.renderBuffers().bufferSource().getBuffer(current), false, random,
                        ModelData.EMPTY, current);
            } finally {
                poseStack.popPose();
            }
        }
        if (current != null) {
            minecraft.renderBuffers().bufferSource().endBatch(current);
        }
    }
}
