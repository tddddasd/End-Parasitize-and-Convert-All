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

public final class ArayaFireRenderer {

    private static final BlockState FIRE_STATE = Blocks.FIRE.defaultBlockState();

    private static List<BakedQuad> cachedQuads;

    private ArayaFireRenderer() {
    }

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

            return false;
        }
    }
}
