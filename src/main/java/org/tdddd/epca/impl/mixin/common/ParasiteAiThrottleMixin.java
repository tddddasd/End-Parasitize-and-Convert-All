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
 * <p>A parasite with no attack target and no player within {@link #RADIUS} blocks runs its goal selector
 * and its movement only every {@link #INTERVAL}th tick; everything else (despawn, damage, effects) keeps
 * running normally, so behaviour stays correct once a player comes back.</p>
 *
 * <p>The counter is the vanilla {@code tickCount} modulo, so no per entity state is allocated and the
 * throttle cannot desynchronise between clients.</p>
 */
@Mixin(Mob.class)
public abstract class ParasiteAiThrottleMixin {

    /** No player within this many blocks means the AI can be throttled. */
    private static final double RADIUS = 16.0D;
    /** Run the full AI on one tick out of this many while idle. */
    private static final int INTERVAL = 4;

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void epca$throttleIdleAi(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!(self instanceof IParasite) || !(self.level() instanceof ServerLevel level)) {
            return;
        }
        // busy parasites always run at full rate
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
