package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

public class ParasiteMoveControl extends MoveControl {

    private final ParasiteGapJump gapJump;

    public ParasiteMoveControl(Mob mob, int maxGapJumpWidth) {
        super(mob);
        this.gapJump = new ParasiteGapJump(maxGapJumpWidth);
    }

    @Override
    public void tick() {

        boolean wantsToMove = this.operation == Operation.MOVE_TO;
        double wantedX = this.wantedX;
        double wantedY = this.wantedY;
        double wantedZ = this.wantedZ;

        super.tick();

        this.gapJump.tick(this.mob, wantsToMove, wantedX, wantedY, wantedZ);
    }
}
