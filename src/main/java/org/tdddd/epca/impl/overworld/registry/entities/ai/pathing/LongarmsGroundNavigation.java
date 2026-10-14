package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Ground navigation for reshape long-arms: the parasite pathing with the widest gap jump.
 *
 * <p>{@link #maxGapJumpWidth} returns a constant and reads no instance state, which matters because
 * the navigation constructor calls it before subclass fields are initialised.</p>
 */
public final class LongarmsGroundNavigation extends ParasiteGroundNavigation {

    public LongarmsGroundNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected int maxGapJumpWidth() {
        return ParasitePathing.MAX_GAP_JUMP_RESHAPE_LONGARMS;
    }
}
