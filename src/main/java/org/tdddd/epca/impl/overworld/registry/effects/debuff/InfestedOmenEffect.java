package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Infested Omen (虫染不详之兆).
 *
 * <p>Counterpart of vanilla Bad Omen: carrying it and walking into a village starts an
 * {@link org.tdddd.epca.impl.overworld.raid.InfestedRaidManager infested raid} instead of a pillager
 * raid. The effect itself only acts as a marker - the trigger lives in the raid manager's player tick.</p>
 *
 * <p>No icon texture is shipped yet (the spec explicitly allows that); the HUD falls back to the
 * missing-texture sprite until one is added at {@code assets/epca/textures/mob_effect/infested_omen.png}.</p>
 */
public class InfestedOmenEffect extends MobEffect {
    /** Colour of the effect's particle cloud / tooltip accent. */
    public static final int COLOR = 0x8B1E4B;

    public InfestedOmenEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }
}
