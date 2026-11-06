package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class RipperGatherGoal extends Goal {

    private static final double HERD_RADIUS = 16.0D;

    private static final int HERD_MIN = 3;

    private static final float HIGHER_TIER_HEALTH = 40.0F;

    private static final int RECHECK_TICKS = 40;

    private final Ripper ripper;
    private int cooldown = 0;
    private LivingEntity leader = null;

    public RipperGatherGoal(Ripper ripper) {
        this.ripper = ripper;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.cooldown = RECHECK_TICKS;
        List<Ripper> herd = this.ripper.level().getEntitiesOfClass(Ripper.class,
                this.ripper.getBoundingBox().inflate(HERD_RADIUS));
        if (herd.size() < HERD_MIN) {
            return false;
        }

        List<Mob> bigger = this.ripper.level().getEntitiesOfClass(Mob.class,
                this.ripper.getBoundingBox().inflate(HERD_RADIUS * 2.0D));
        this.leader = bigger.stream()
                .filter(mob -> mob != this.ripper)
                .filter(mob -> mob.getMaxHealth() >= HIGHER_TIER_HEALTH)
                .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(this.ripper)))
                .orElse(null);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.ripper.isAlive() && (this.leader != null && this.leader.isAlive()
                || !this.ripper.getNavigation().isDone());
    }

    @Override
    public void start() {
        tick();
    }

    @Override
    public void tick() {
        if (this.leader != null && this.leader.isAlive()) {

            this.ripper.getNavigation().moveTo(this.leader, 1.0D);
            return;
        }

        List<Ripper> herd = this.ripper.level().getEntitiesOfClass(Ripper.class,
                this.ripper.getBoundingBox().inflate(HERD_RADIUS));
        double sx = 0.0D, sy = 0.0D, sz = 0.0D;
        for (Ripper other : herd) {
            sx += other.getX();
            sy += other.getY();
            sz += other.getZ();
        }
        int n = Math.max(1, herd.size());
        this.ripper.getNavigation().moveTo(sx / n, sy / n, sz / n, 1.0D);
    }

    @Override
    public void stop() {
        this.leader = null;
        this.ripper.getNavigation().stop();
    }
}
