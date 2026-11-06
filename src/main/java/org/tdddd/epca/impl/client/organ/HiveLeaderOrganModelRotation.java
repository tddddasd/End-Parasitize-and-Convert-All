package org.tdddd.epca.impl.client.organ;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class HiveLeaderOrganModelRotation {

    public static final float INITIAL_YAW = 180.0F;

    public static final float INITIAL_PITCH = 0.0F;

    public static final float YAW_PER_PIXEL = 0.28F;

    public static final float PITCH_PER_PIXEL = 0.22F;

    public static final float MAX_YAW_STEP_PER_EVENT = 16.0F;

    public static final float MAX_PITCH_STEP_PER_EVENT = 12.0F;

    public static final float MAX_PITCH = 62.0F;

    public static final float SMOOTHING_PER_TICK = 0.30F;

    public static final float MAX_DEGREES_PER_TICK = 12.0F;

    public static final float SNAP_EPSILON_DEGREES = 0.05F;

    public static final float INITIAL_NUDGE_DEGREES = 0.75F;

    private float targetYaw = INITIAL_YAW;

    private float targetPitch = INITIAL_PITCH;

    private float yaw = INITIAL_YAW;

    private float pitch = INITIAL_PITCH;

    private float dragDistanceX;
    private float dragDistanceY;

    public void beginDrag() {
        this.dragDistanceX = 0.0F;
        this.dragDistanceY = 0.0F;
    }

    public void dragged(float deltaX, float deltaY) {
        this.dragDistanceX += deltaX;
        this.dragDistanceY += deltaY;

        float yawStep = Mth.clamp(deltaX * YAW_PER_PIXEL,
                -MAX_YAW_STEP_PER_EVENT, MAX_YAW_STEP_PER_EVENT);
        float pitchStep = Mth.clamp(deltaY * PITCH_PER_PIXEL,
                -MAX_PITCH_STEP_PER_EVENT, MAX_PITCH_STEP_PER_EVENT);

        this.targetYaw += yawStep;
        this.targetPitch = clampPitch(this.targetPitch + pitchStep);
    }

    public void endDrag() {
        boolean barelyMoved = Math.abs(this.dragDistanceX) < 1.0F
                && Math.abs(this.dragDistanceY) < 1.0F;
        if (barelyMoved) {
            this.targetYaw += INITIAL_NUDGE_DEGREES;
        }
        this.dragDistanceX = 0.0F;
        this.dragDistanceY = 0.0F;
    }

    public void update(float tickDelta) {
        float safeDelta = Mth.clamp(tickDelta, 0.0F, 1.0F);
        float factor = (float) (1.0D - Math.pow(1.0D - SMOOTHING_PER_TICK, safeDelta));

        this.yaw = approach(this.yaw, this.targetYaw, factor, MAX_DEGREES_PER_TICK);
        this.pitch = approach(this.pitch, this.targetPitch, factor, MAX_DEGREES_PER_TICK);
    }

    private static float approach(float current, float target, float factor, float maxStep) {
        float difference = target - current;
        if (Math.abs(difference) <= SNAP_EPSILON_DEGREES) return target;
        float step = Mth.clamp(difference * factor, -maxStep, maxStep);
        if (Math.abs(step) > Math.abs(difference)) step = difference;
        return current + step;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float targetYaw() {
        return targetYaw;
    }

    public float targetPitch() {
        return targetPitch;
    }

    public boolean settled() {
        return Math.abs(targetYaw - yaw) <= SNAP_EPSILON_DEGREES
                && Math.abs(targetPitch - pitch) <= SNAP_EPSILON_DEGREES;
    }

    public static float clampPitch(float pitch) {
        if (pitch > MAX_PITCH) return MAX_PITCH;
        if (pitch < -MAX_PITCH) return -MAX_PITCH;
        return pitch;
    }
}
