package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class PriorityTargetGoal extends Goal {
    private final Mob mob;
    private final IParasite parasite;          
    private final double targetSearchRange;
    private final TargetingConditions targetConditions;
    @Nullable
    private LivingEntity target;

    public PriorityTargetGoal(Mob mob, double range) {
        this.mob = mob;
        this.parasite = (IParasite) mob;
        this.targetSearchRange = range;
        this.targetConditions = TargetingConditions.forCombat().range(range);
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        LivingEntity forcedTarget = getForcedTarget();
        if (forcedTarget != null && forcedTarget.isAlive() && canTarget(forcedTarget)) {
            this.target = forcedTarget;
            return true;
        }
        this.target = getNearestValidTarget();
        
        if (this.target != null && !canTarget(this.target)) {
            this.target = null;
        }
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) return false;
        
        if (!canTarget(this.target)) {
            return false;
        }
        if (getForcedTarget() != null && getForcedTarget() == this.target) {
            return mob.distanceToSqr(this.target) <= (targetSearchRange * targetSearchRange) + 4;
        }
        return mob.distanceToSqr(this.target) <= (targetSearchRange * targetSearchRange);
    }

    @Override
    public void start() {
        mob.setTarget(this.target);
    }

    @Override
    public void stop() {
        this.target = null;
        
    }

    @Nullable
    private LivingEntity getForcedTarget() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return null;
        return parasite.getForcedTarget(serverLevel);
    }

    private boolean canTarget(LivingEntity target) {
        return target != null
                && !IParasite.isParasiteByTagOrInterface(target)  
                && !(target instanceof Creeper)
                && target.isAlive()
                && mob.hasLineOfSight(target)
                && !(target instanceof Player && (((Player) target).isCreative() || ((Player) target).isSpectator()))
                && !(target instanceof ArmorStand);
    }

    @Nullable
    private LivingEntity getNearestValidTarget() {
        AABB searchBox = mob.getBoundingBox().inflate(targetSearchRange);
        List<LivingEntity> candidates = mob.level().getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e != mob && !IParasite.isParasiteByTagOrInterface(e) && e.isAlive()
        );
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (!canTarget(candidate)) continue;
            double distSq = mob.distanceToSqr(candidate);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = candidate;
            }
        }
        return nearest;
    }
}