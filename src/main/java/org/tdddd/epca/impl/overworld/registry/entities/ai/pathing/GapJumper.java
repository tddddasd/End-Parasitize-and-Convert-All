package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

/**
 * Legal access to {@code LivingEntity#jumpFromGround()} for the parasite gap jump.
 *
 * <p>{@code jumpFromGround()} is {@code protected}, so {@link ParasiteGapJump} - which only holds a
 * {@code Mob} reference and lives in another package - cannot call it, and reflection or an access
 * widener is not wanted. Each wired mob therefore implements this interface with a one-line bridge
 * whose body runs <em>inside</em> that mob's own class, where {@code super.jumpFromGround()} is
 * legal:</p>
 *
 * <pre>{@code
 * @Override
 * public void epcaJumpFromGround() {
 *     super.jumpFromGround();
 * }
 * }</pre>
 *
 * <p>That bridge is deliberately the whole feature's only requirement on the mob classes: it keeps
 * every mob's own jump strength, its sprint bonus and its own {@code jumpFromGround} override
 * (ReshapeLongarms' {@code +1.0} impulse, {@code Ripper}'s, the slimes') exactly as they are, so the
 * leap stays a normal vanilla-shaped jump instead of a hand-rolled impulse.</p>
 *
 * <p>{@link ParasiteGapJump} checks {@code mob instanceof GapJumper} before calling it, so a mob
 * that is wired to {@code ParasiteMoveControl} without this bridge only loses the leap; it can never
 * throw.</p>
 */
public interface GapJumper {

    /** Bridge to the mob's own (protected) {@code LivingEntity#jumpFromGround()}. */
    void epcaJumpFromGround();
}
