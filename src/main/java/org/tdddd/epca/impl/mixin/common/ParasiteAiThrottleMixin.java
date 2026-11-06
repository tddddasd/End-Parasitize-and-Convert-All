package org.tdddd.epca.impl.mixin.common;

import net.minecraft.server.level.ServerLevel;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class ParasiteAiThrottleMixin {

    private static final double RADIUS = 16.0D;

    private static final int INTERVAL = 4;

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void epca$throttleIdleAi(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!ParasiteEvents.isParasiteEntity(self) || !(self.level() instanceof ServerLevel level)) {
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
