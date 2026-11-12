package org.tdddd.epca.impl.client;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.entity.PartEntity;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class PartHitResultHandler {

    private PartHitResultHandler() {
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.hitResult == null) {
            return;
        }
        double reach = mc.gameMode == null ? 3.0 : mc.gameMode.getPickRange();
        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 end = start.add(mc.player.getViewVector(1.0F).scale(reach));
        EntityHitResult best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!entity.isMultipartEntity() || !entity.isAlive()) {
                continue;
            }
            if (entity.distanceToSqr(start) > (reach + 10.0) * (reach + 10.0)) {
                continue;
            }
            PartEntity<?>[] parts = entity.getParts();
            if (parts == null) {
                continue;
            }
            for (PartEntity<?> part : parts) {
                if (part == null || part.isRemoved()) {
                    continue;
                }
                Optional<Vec3> hit = part.getBoundingBox().inflate(0.3).clip(start, end);
                if (hit.isPresent()) {
                    double dist = hit.get().distanceToSqr(start);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = new EntityHitResult(entity, hit.get());
                    }
                }
            }
        }
        if (best == null) {
            return;
        }
        if (mc.hitResult.getType() != HitResult.Type.ENTITY
                || bestDist < mc.hitResult.getLocation().distanceToSqr(start)) {
            mc.hitResult = best;
        }
    }
}