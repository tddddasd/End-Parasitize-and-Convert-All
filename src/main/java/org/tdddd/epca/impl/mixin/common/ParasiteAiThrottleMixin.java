package org.tdddd.epca.impl.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

/**
 * Throttles parasite AI when nobody is around to see it.
 *
 * <p>Verified against the 26.1.2 jar: {@code Mob.serverAiStep()} takes no arguments (it is final, which
 * does not affect injection). A parasite with no live target and no player within {@link #RADIUS} blocks
 * only runs its goal selector on one tick in {@link #INTERVAL}; despawn, damage and effects keep running
 * every tick, so behaviour is unchanged as soon as a player comes back.</p>
 */
@Mixin(Mob.class)
public abstract class ParasiteAiThrottleMixin {

    private static final double RADIUS = 16.0D;
    private static final int INTERVAL = 4;

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void epca$throttleIdleAi(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!(self instanceof IParasite) || !(self.level() instanceof ServerLevel level)) {
            return;
        }
        if (self.getTarget() != null && self.getTarget().isAlive()) {
            return;
        }
        Player nearby = level.getNearestPlayer(self, RADIUS);
        if (nearby != null && !nearby.isSpectator()) {
            return;
        }
        if (self.tickCount % INTERVAL != 0) {
            ci.cancel();
        }
    }
}
