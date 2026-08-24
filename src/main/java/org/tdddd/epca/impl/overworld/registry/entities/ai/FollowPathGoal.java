package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedZombie;

public class FollowPathGoal extends Goal {
    private final InfestedZombie mob;

    public FollowPathGoal(InfestedZombie mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return mob.isPathFollower();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.isPathFollower();
    }

    @Override
    public void tick() {
        mob.followPathStep();
    }

    @Override
    public void stop() {
        
        if (mob.isPathFollower()) {
            mob.followPathStep(); 
        }
    }
}