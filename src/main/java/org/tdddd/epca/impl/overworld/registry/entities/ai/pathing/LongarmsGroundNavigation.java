package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class LongarmsGroundNavigation extends ParasiteGroundNavigation {

    public LongarmsGroundNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected int maxGapJumpWidth() {
        return ParasitePathing.MAX_GAP_JUMP_RESHAPE_LONGARMS;
    }
}
