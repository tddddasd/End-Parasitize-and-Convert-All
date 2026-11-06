package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;

public class WebAwareRangedAttackGoal extends RangedAttackGoal {

    private final InfestedSpider spider;

    public WebAwareRangedAttackGoal(InfestedSpider spider, double speedModifier,
                                    int attackInterval, float attackRadius) {
        super(spider, speedModifier, attackInterval, attackRadius);
        this.spider = spider;
    }

    private boolean shouldYieldToMelee() {
        LivingEntity target = this.spider.getTarget();
        if (target == null) {
            return false;
        }
        return InfestedSpider.isStandingInInfestedWeb(target)
                || InfestedSpider.isWithinMeleeHorizontalRange(this.spider, target);
    }

    @Override
    public boolean canUse() {
        if (shouldYieldToMelee()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (shouldYieldToMelee()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
