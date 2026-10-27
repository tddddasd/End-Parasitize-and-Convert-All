package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

/**
 * Skips rendering a parasite the camera cannot see.
 *
 * <p>Verified against the 26.1.2 jar:
 * {@code EntityRenderDispatcher.shouldRender(E, Frustum, double, double, double)}. Pure client side: no
 * entity data is touched and the server is unaffected, so despawn, targeting and hitboxes behave exactly
 * as before.</p>
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class ParasiteOcclusionMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void epca$cullHiddenParasite(Entity entity, Frustum frustum,
                                         double camX, double camY, double camZ,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof IParasite)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.level != entity.level()) {
            return;
        }
        if (entity == minecraft.player || minecraft.player.getVehicle() == entity) {
            return;
        }
        Vec3 from = new Vec3(camX, camY, camZ);
        Vec3 to = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        if (from.distanceToSqr(to) < 4.0D) {
            return;
        }
        HitResult hit = minecraft.level.clip(new ClipContext(
                from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
        if (hit.getType() != HitResult.Type.MISS) {
            cir.setReturnValue(false);
        }
    }
}
