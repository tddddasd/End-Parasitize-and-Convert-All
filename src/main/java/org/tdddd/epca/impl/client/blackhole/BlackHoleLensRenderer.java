package org.tdddd.epca.impl.client.blackhole;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BlackHoleEntity;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class BlackHoleLensRenderer {


    private static final boolean ENABLED = !Boolean.getBoolean("epca.lens.disabled");

    private BlackHoleLensRenderer() { }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!ENABLED) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ShaderInstance shader = BlackHoleShaders.lens();
        if (shader == null) return;

        Vec3 camera = event.getCamera().getPosition();
        BlackHoleEntity hole = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof BlackHoleEntity candidate)) continue;
            double d = candidate.position().distanceToSqr(camera);
            if (d < best) { best = d; hole = candidate; }
        }
        if (hole == null) return;

        BlackHoleSceneCopy.captureFrame(mc.level.getGameTime());
        if (BlackHoleSceneCopy.wasRecreated()) return;   // skip the frame with a freshly created (empty) texture

        double dx = hole.getX() - camera.x;
        double dy = hole.getY() - camera.y;
        double dz = hole.getZ() - camera.z;
        Matrix3f viewRot = new Matrix3f(RenderSystem.getInverseViewRotationMatrix()).invert();
        Vector3f holeView = viewRot.transform(new Vector3f((float) dx, (float) dy, (float) dz));
        float fov = (float) Math.toRadians(mc.options.fov().get());
        float aspect = (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight();

        if (holeView.z > -0.05F) return;   // hole is behind the camera: nothing to bend
        shader.safeGetUniform("HoleView").set(holeView.x, holeView.y, holeView.z);
        shader.safeGetUniform("HoleRadius").set(Math.max(hole.radius() * 0.317F, 0.02F));
        shader.safeGetUniform("TanHalfFov").set((float) Math.tan(fov * 0.5F));
        shader.safeGetUniform("Aspect").set(aspect);
        shader.safeGetUniform("DeflectK").set(6.0F);
        shader.safeGetUniform("ColorModulator").set(1.0F, 1.0F, 1.0F, 1.0F);

        PoseStack poseStack = new PoseStack();
        Matrix4f matrix = poseStack.last().pose();
        MultiBufferSource.BufferSource buffers =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer consumer = buffers.getBuffer(BlackHoleLensRenderType.LENS);
        consumer.vertex(matrix, -1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, 1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        consumer.vertex(matrix, 1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffers.endBatch(BlackHoleLensRenderType.LENS);
    }
}