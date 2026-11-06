package org.tdddd.epca.impl.client.entity;

import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HeadRotationHandler {

    private static final Map<Integer, HeadState> STATES = new ConcurrentHashMap<>();

    private static final float RESET_DELAY = 5f;

    public static float computeHeadRotation(
            int entityId, IHeadRotatable rotatable,
            float currentTime, float partialTick, float bodyYaw) {

        HeadState s = STATES.computeIfAbsent(entityId, k -> new HeadState());

        if (rotatable.shouldRotateHead()) {
            s.idleTicks = 0;

            float target = rotatable.getHeadYawTarget(partialTick);
            if (target == Float.MAX_VALUE) {
                s.idleTicks = RESET_DELAY;
            } else {

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
