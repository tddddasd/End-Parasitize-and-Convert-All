package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.CothEffect;

/**
 * Decides what happens when a creature touches an infested block.
 *
 * <p>Keeping the decision here means blocks only declare "I am an infested block" and this class owns
 * the rule: only the infested silverfish picks up Call of the Hive while walking through one.</p>
 */
public final class InfestedBlockContact {

    /** Effect duration in ticks applied on contact (10 seconds). */
    public static final int CONTACT_DURATION_TICKS = 200;
    /** Effect amplifier applied on contact (level I). */
    public static final int CONTACT_AMPLIFIER = 0;

    private InfestedBlockContact() {
    }

    /** True when this block grants the contact effect to {@code entity}. */
    public static boolean accepts(BlockState state, Entity entity) {
        return state.getBlock() instanceof InfestedBlockInterface
                && entity instanceof org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;
    }

    /** Applies the contact effect when the block and entity pair qualifies. */
    public static void onContact(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (accepts(state, entity)) {
            CothEffect.applyCothEffect(living, CONTACT_DURATION_TICKS, CONTACT_AMPLIFIER);
        }
    }
}
