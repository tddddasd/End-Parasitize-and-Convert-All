package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;

/**
 * {@link RangedAttackGoal} that stands down for melee when either of two conditions holds:
 *
 * <ol>
 *   <li>the target is standing in one of this mod's infested web blocks, or</li>
 *   <li>the target is within four blocks <b>horizontally</b> ({@code MELEE_HORIZONTAL_RANGE}
 *       on {@link InfestedSpider}).</li>
 * </ol>
 *
 * <p>User requirement: in both cases the spider must stop spitting and instead close in and melee.
 * Suppressing only {@code performRangedAttack} is not enough - the vanilla goal would keep hovering
 * at range and its movement would still win on priority - so this goal reports {@code canUse() ==
 * false} and {@code canContinueToUse() == false} for those states. The spider's {@code
 * MeleeAttackGoal} (already registered at priority 5) then becomes the highest-priority active goal
 * and drives the pursuit and the melee swing itself.</p>
 *
 * <p>No version-specific API is involved: RangedAttackGoal, canUse and canContinueToUse are the same
 * in 1.20.1 and 26.1.2, so this file is identical in both trees.</p>
 */
public class WebAwareRangedAttackGoal extends RangedAttackGoal {

    private final InfestedSpider spider;

    public WebAwareRangedAttackGoal(InfestedSpider spider, double speedModifier,
                                    int attackInterval, float attackRadius) {
        super(spider, speedModifier, attackInterval, attackRadius);
        this.spider = spider;
    }

    /**
     * True when this goal must yield to melee: the target is webbed, or it is inside the horizontal
     * melee radius. The radius itself lives on {@link InfestedSpider} as the single source of truth,
     * so the goal and the {@code performRangedAttack} guard can never drift apart. Every other
     * condition the vanilla goal checks is left to {@code super}.
     */
    private boolean shouldYieldToMelee() {
        LivingEntity target = this.spider.getTarget();
        if (target == null) {
            return false;
        }
        return InfestedSpider.isStandingInInfestedWeb(target)
                || InfestedSpider.isWithinMeleeHorizontalRange(this.spider, target);
    }

    /** Never engages while the target is webbed or already inside melee range. */
    @Override
    public boolean canUse() {
        if (shouldYieldToMelee()) {
            return false;
        }
        return super.canUse();
    }

    /**
     * Aborts an attack already in progress the moment the target steps into a web OR closes inside
     * the 4-block horizontal radius - the same mid-attack behaviour the web case already had.
     */
    @Override
    public boolean canContinueToUse() {
        if (shouldYieldToMelee()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
