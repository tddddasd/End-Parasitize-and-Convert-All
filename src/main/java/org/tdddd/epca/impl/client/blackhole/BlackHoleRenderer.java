package org.tdddd.epca.impl.client.blackhole;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BlackHoleEntity;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class BlackHoleRenderer {

    private static final float SHADOW_FACTOR = 2.5980762F;
    private static final float QUAD_FACTOR = 9.6F;
    private static final float ORB_RADIUS = 0.317F;

    private static final float[] CORNER = {-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F};
    private static final float[] UV = {0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F};

    private BlackHoleRenderer() { }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        boolean afterWeather = event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER;
        boolean beforeLens = event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES;
        if (!afterWeather && !beforeLens) {
            return;
        }
        ShaderInstance shader = BlackHoleShaders.blackHole();
        Minecraft minecraft = Minecraft.getInstance();
        if (shader == null || minecraft.level == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        float time = (float) (minecraft.level.getGameTime() % 100000L) + partialTick;

        for (Entity candidate : minecraft.level.entitiesForRendering()) {
            if (!(candidate instanceof BlackHoleEntity entity)) {
                continue;
            }
            double px = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double py = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double pz = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            double dx = px - camera.x;
            double dy = py - camera.y;
            double dz = pz - camera.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (!(distance > 1.0e-3) || distance > 640.0) {
                continue;
            }

            float orbRadius = Math.max(entity.radius() * ORB_RADIUS, 0.02F);
            float rs = Math.max(orbRadius / SHADOW_FACTOR, 0.008F);
            double half = rs * QUAD_FACTOR;
            if (distance < half * 1.15D) {
                half = distance * 0.90D;
            }

            Matrix3f viewRot = new Matrix3f(RenderSystem.getInverseViewRotationMatrix()).invert();
            Vector3f holeView = viewRot.transform(new Vector3f((float) dx, (float) dy, (float) dz));
            Vector3f upView = viewRot.transform(new Vector3f(0.0F, 1.0F, 0.0F));
            if (upView.lengthSquared() < 1.0E-6F) {
                upView.set(0.0F, 1.0F, 0.0F);
            }
            upView.normalize();

            RenderSystem.setShader(() -> shader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            shader.safeGetUniform("uHole").set(holeView.x, holeView.y, holeView.z);
            shader.safeGetUniform("uUp").set(upView.x, upView.y, upView.z);
            shader.safeGetUniform("uRadius").set(rs);
            shader.safeGetUniform("uTime").set(time);
            shader.safeGetUniform("uSpin").set((entity.getId() % 97) * 0.41F + 0.37F);
            shader.safeGetUniform("uDisk").set(afterWeather ? 0.0F : 1.0F);
            shader.safeGetUniform("uStars").set(afterWeather ? 0.0F : 1.0F);
            shader.safeGetUniform("ColorModulator").set(1.0F, 1.0F, 1.0F, 1.0F);

            PoseStack modelView = RenderSystem.getModelViewStack();
            modelView.pushPose();
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();

            try {
                BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                for (int i = 0; i < 4; i++) {
                    float ox = CORNER[i * 2];
                    float oy = CORNER[i * 2 + 1];
                    float x = holeView.x + ox * (float) half;
                    float y = holeView.y + oy * (float) half;
                    buffer.vertex(x, y, holeView.z).uv(UV[i * 2], UV[i * 2 + 1]).endVertex();
                }
                BufferUploader.drawWithShader(buffer.end());
            } finally {
                modelView.popPose();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.depthMask(true);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
            }
        }
    }
}