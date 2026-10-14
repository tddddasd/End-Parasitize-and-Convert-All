package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Vanilla ground move control plus the parasite gap jump.
 *
 * <p>{@code super.tick()} is called first and unchanged, so ordinary walking is byte for byte the
 * vanilla behaviour. The leap is a throttled extra on top of it and performs no block lookup on the
 * vast majority of ticks; see {@link ParasiteGapJump}.</p>
 *
 * <h2>The leap is always evaluated; the operation no longer gates it</h2>
 * <p>An earlier revision only let the leap run when the move control had a {@code MOVE_TO} step, and a
 * later one widened that to a "blocked" test. That was still too narrow: a mob that has acquired a
 * target but whose navigation can produce no path sits in {@code Operation.WAIT} and nothing ever
 * asked for a leap - which is exactly the reported "target acquired, not moving, still no jump".
 * {@link ParasiteGapJump} now decides for itself from {@code operation}, the target and the navigation
 * state (see {@link ParasiteGapJump#isBlocked()}), so this control simply hands over every tick.</p>
 */
public class ParasiteMoveControl extends MoveControl {

    private final ParasiteGapJump gapJump;

    public ParasiteMoveControl(Mob mob, int maxGapJumpWidth) {
        super(mob);
        this.gapJump = new ParasiteGapJump(maxGapJumpWidth);
    }

    @Override
    public void tick() {
        // super.tick() consumes the MOVE_TO operation, so the state and the wanted position have to be
        // read before it runs. wantsToMove is reported to the leap as "the move control was actually
        // asked to take a step this tick"; it is deliberately no longer a precondition for anything.
        boolean wantsToMove = this.operation == Operation.MOVE_TO;
        double wantedX = this.wantedX;
        double wantedY = this.wantedY;
        double wantedZ = this.wantedZ;

        super.tick();

        this.gapJump.tick(this.mob, wantsToMove, wantedX, wantedY, wantedZ);
    }
}
