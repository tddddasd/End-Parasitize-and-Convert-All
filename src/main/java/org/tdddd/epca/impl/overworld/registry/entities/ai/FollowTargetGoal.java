package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class FollowTargetGoal extends Goal {
    private final Mob mob;
    private final IParasite parasite;
    private final double speed;
    private final int range;
    @Nullable
    private LivingEntity target;

    public FollowTargetGoal(Mob mob, double speed, int range) {
        this.mob = mob;
        this.parasite = (IParasite) mob;
        this.speed = speed;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (parasite.getFollowTarget() == null) return false;
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) return false;
        var followUuid = parasite.getFollowTarget();
        var entity = serverLevel.getEntity(followUuid);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            parasite.setFollowTarget(null);
            return false;
        }
        if (mob.distanceToSqr(living) > range * range) {
            return false;
        }
        this.target = living;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        var current = parasite.getFollowTarget();
        if (current == null || !current.equals(target.getUUID())) return false;
        return mob.distanceToSqr(target) <= (range * range * 4);
    }

    @Override
    public void tick() {
        if (target != null) {
            mob.getNavigation().moveTo(target, speed);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        target = null;
    }
}