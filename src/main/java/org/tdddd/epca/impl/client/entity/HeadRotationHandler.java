package org.tdddd.epca.impl.client.entity;

import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side handler for IHeadRotatable entity head rotation.
 *
 * <p>Called from {@link EpcaGeoRenderer} while it builds the render pass. Each entity gets a
 * persistent {@link HeadState} that tracks the smoothed yaw. When the entity
 * has no look target or stops moving, the yaw gradually returns to 0.</p>
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>GeckoLib 4 applied this from {@code GeoModel#setCustomAnimations} by grabbing the bone
 * through {@code GeoModel#getAnimationProcessor()} and calling {@code bone.setRotY(...)}.
 * In GeckoLib 5 bones are immutable during rendering: the pose is expressed as a
 * {@code BoneSnapshot} produced by a {@code RenderPassInfo.BoneUpdater}. So this class no
 * longer touches bones at all — it only computes the smoothed yaw in radians, and
 * {@link EpcaGeoRenderer} installs the value into the head bone's snapshot.</p>
 */
public class HeadRotationHandler {

    private static final Map<Integer, HeadState> STATES = new ConcurrentHashMap<>();

    /** How many ticks of no rotation target before starting to reset to centre. */
    private static final float RESET_DELAY = 5f;

    /**
     * Advance the smoothed head yaw for one entity and return the new head rotation.
     *
     * @return the head bone Y rotation in <b>radians</b> (same convention as the old
     *         {@code CoreGeoBone#setRotY} call this replaces)
     */
    public static float computeHeadRotation(
            int entityId, IHeadRotatable rotatable,
            float currentTime, float partialTick, float bodyYaw) {

        HeadState s = STATES.computeIfAbsent(entityId, k -> new HeadState());

        if (rotatable.shouldRotateHead()) {
            s.idleTicks = 0;

            float target = rotatable.getHeadYawTarget(partialTick);
            if (target == Float.MAX_VALUE) {
                s.idleTicks = RESET_DELAY; // trigger reset
            } else {
                // target is an absolute world-space yaw (same convention as entity yaw)
                // Convert to head rotation relative to the entity's body
                float desired = Mth.clamp(
                        Mth.wrapDegrees(target - bodyYaw),
                        -rotatable.getMaxHeadYaw(), rotatable.getMaxHeadYaw());
                s.yawTarget = desired;
            }
        } else {
            s.idleTicks++;
        }

        float dt = Math.max(currentTime - s.time, 0.001f);
        float speed = rotatable.getHeadRotationSpeed();
        float step = speed * dt;

        // When we've been idle long enough, ease back to centre; otherwise chase target
        float goal = (s.idleTicks >= RESET_DELAY) ? 0f : s.yawTarget;
        float diff = Mth.wrapDegrees(goal - s.yaw);
        s.yaw += Mth.clamp(diff, -step, step);

        if (Math.abs(s.yaw) < 0.05f) s.yaw = 0f;
        s.time = currentTime;

        return s.yaw * Mth.DEG_TO_RAD;
    }

    public static void removeEntity(int entityId) {
        STATES.remove(entityId);
    }

    private static class HeadState {
        float yaw;
        float yawTarget;
        float time;
        float idleTicks = RESET_DELAY;
    }
}
