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

public final class ArayaFireRenderer {

    private static final BlockState FIRE_STATE = Blocks.FIRE.defaultBlockState();

    private ArayaFireRenderer() {
    }

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
