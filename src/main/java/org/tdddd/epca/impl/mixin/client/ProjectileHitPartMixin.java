package org.tdddd.epca.impl.mixin.client;

import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.entity.projectile.ProjectileUtil.class)
public class ProjectileHitPartMixin {

    @Inject(
            method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("RETURN"),
            cancellable = true)
    private static void epca$hitParts(Level level, Entity projectile, Vec3 start, Vec3 end, AABB box,
                                      Predicate<Entity> filter, float maxDist,
                                      CallbackInfoReturnable<EntityHitResult> cir) {
        EntityHitResult base = cir.getReturnValue();
        double bestDist = base == null ? Double.MAX_VALUE : base.getLocation().distanceToSqr(start);
        EntityHitResult best = base;
        for (Entity candidate : level.getEntities(projectile, box.inflate(maxDist), filter)) {
            if (!candidate.isMultipartEntity()) {
                continue;
            }
            PartEntity<?>[] parts = candidate.getParts();
            if (parts == null) {
                continue;
            }
            for (PartEntity<?> part : parts) {
                if (part == null || part.isRemoved()) {
                    continue;
                }
                Optional<Vec3> hit = part.getBoundingBox().clip(start, end);
                if (hit.isPresent()) {
                    double dist = hit.get().distanceToSqr(start);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = new EntityHitResult(part, hit.get());
                    }
                }
            }
        }
        if (best != base) {
            cir.setReturnValue(best);
        }
    }
}