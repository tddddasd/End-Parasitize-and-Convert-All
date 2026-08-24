package org.tdddd.epca.impl.client.entity;

import net.minecraft.util.Mth;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side handler for IHeadRotatable entity head rotation.
 *
 * <p>Call from {@link EpcaGeoModel#setCustomAnimations}. Each entity gets a
 * persistent {@link HeadState} that tracks the smoothed yaw. When the entity
 * has no look target or stops moving, the yaw gradually returns to 0.</p>
 */
public class HeadRotationHandler {

    private static final Map<Integer, HeadState> STATES = new ConcurrentHashMap<>();

    /** How many ticks of no rotation target before starting to reset to centre. */
    private static final float RESET_DELAY = 5f;

    public static void applyHeadRotation(
            int entityId, IHeadRotatable rotatable, GeoModel<?> model,
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

        applyToBone(model, rotatable.getHeadBoneName(), s.yaw * Mth.DEG_TO_RAD);
    }

    private static void applyToBone(GeoModel<?> model, String boneName, float rads) {
        var bone = model.getAnimationProcessor().getBone(boneName);
        if (bone != null) bone.setRotY(rads);
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
