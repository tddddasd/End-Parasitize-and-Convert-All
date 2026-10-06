package org.tdddd.epca.impl.client.organ;

import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 *  GUI <b></b>SPEC  1  C1
 *
 * <h2></h2>
 * <p></p>
 * <ol>
 *   <li><b></b> = </li>
 *   <li><b></b> {@code mouseDragged}
 *       </li>
 *   <li><b></b> tick
 *       </li>
 * </ol>
 * <p>dragged / release / update / yaw / pitch
 * </p>
 *
 * <h2>tick</h2>
 * <table border="1">
 *   <caption></caption>
 *   <tr><th></th><th></th><th></th></tr>
 *   <tr><td></td><td>yaw 180pitch 0</td><td> {@code 180 + f * 40} </td></tr>
 *   <tr><td>{@link #YAW_PER_PIXEL}</td><td>0.28/px</td><td> 0.9 100px  90</td></tr>
 *   <tr><td>{@link #PITCH_PER_PIXEL}</td><td>0.22/px</td><td> 0.5</td></tr>
 *   <tr><td>{@link #MAX_YAW_STEP_PER_EVENT}</td><td>16</td><td></td></tr>
 *   <tr><td>{@link #MAX_PITCH_STEP_PER_EVENT}</td><td>12</td><td></td></tr>
 *   <tr><td>{@link #MAX_PITCH}</td><td>62</td><td> 50/</td></tr>
 *   <tr><td>{@link #SMOOTHING_PER_TICK}</td><td>0.30</td><td> tick  30%  8 tick </td></tr>
 *   <tr><td>{@link #MAX_DEGREES_PER_TICK}</td><td>12/tick</td><td></td></tr>
 *   <tr><td>{@link #SNAP_EPSILON_DEGREES}</td><td>0.05</td><td></td></tr>
 *   <tr><td>{@link #INITIAL_NUDGE_DEGREES}</td><td>0.75</td><td></td></tr>
 * </table>
 *
 * <h2> tick </h2>
 * <p> {@code Minecraft#getDeltaFrameTime()}
 * _tmp_vanilla_src/normalized Minecraft.java  2595  tick
 *  20tps60fps  {@code 1/3}
 * {@code 1 - (1 - SMOOTHING_PER_TICK) ^ tickDelta}
 *  30fps  240fps </p>
 *
 * <h2></h2>
 * <p>{@code InventoryScreen#renderEntityInInventoryFollowsAngle}
 * {@code rotateX(f1 * 20)}_tmp_vanilla_src/.../InventoryScreen.java  100122
 * <b></b></p>
 */
@OnlyIn(Dist.CLIENT)
public final class NestLeaderOrganModelRotation {

    /** 180 =  */
    public static final float INITIAL_YAW = 180.0F;
    /** 0 =  */
    public static final float INITIAL_PITCH = 0.0F;

    /**  yaw 0.28   320px  90 */
    public static final float YAW_PER_PIXEL = 0.28F;
    /**  pitch  */
    public static final float PITCH_PER_PIXEL = 0.22F;

    /**  {@code mouseDragged}  yaw  */
    public static final float MAX_YAW_STEP_PER_EVENT = 16.0F;
    /**  {@code mouseDragged}  pitch  */
    public static final float MAX_PITCH_STEP_PER_EVENT = 12.0F;

    /**  62 / */
    public static final float MAX_PITCH = 62.0F;

    /**  tick 0.30 =  tick  30% */
    public static final float SMOOTHING_PER_TICK = 0.30F;
    /**  tick  */
    public static final float MAX_DEGREES_PER_TICK = 12.0F;
    /**  */
    public static final float SNAP_EPSILON_DEGREES = 0.05F;
    /**  */
    public static final float INITIAL_NUDGE_DEGREES = 0.75F;

    /**  yaw 180 */
    private float targetYaw = INITIAL_YAW;
    /**  pitch */
    private float targetPitch = INITIAL_PITCH;
    /**  yaw {@link #targetYaw}  */
    private float yaw = INITIAL_YAW;
    /**  pitch {@link #targetPitch}  */
    private float pitch = INITIAL_PITCH;
    /** / */
    private float dragDistanceX;
    private float dragDistanceY;


    /**  */
    public void beginDrag() {
        this.dragDistanceX = 0.0F;
        this.dragDistanceY = 0.0F;
    }

    /**
     *
     *
     * <p>{@link #MAX_YAW_STEP_PER_EVENT} /
     * {@link #MAX_PITCH_STEP_PER_EVENT}
     *  90 {@link #update(float)} </p>
     *
     * @param deltaX
     * @param deltaY
     */
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

    /**
     *
     * - {@code InventoryScreen}
     *
     */
    public void endDrag() {
        boolean barelyMoved = Math.abs(this.dragDistanceX) < 1.0F
                && Math.abs(this.dragDistanceY) < 1.0F;
        if (barelyMoved) {
            this.targetYaw += INITIAL_NUDGE_DEGREES;
        }
        this.dragDistanceX = 0.0F;
        this.dragDistanceY = 0.0F;
    }


    /**
     *
     *
     * @param tickDelta  tick{@code Minecraft#getDeltaFrameTime()} 0..1
     */
    public void update(float tickDelta) {
        float safeDelta = Mth.clamp(tickDelta, 0.0F, 1.0F);
        float factor = (float) (1.0D - Math.pow(1.0D - SMOOTHING_PER_TICK, safeDelta));

        this.yaw = approach(this.yaw, this.targetYaw, factor, MAX_DEGREES_PER_TICK);
        this.pitch = approach(this.pitch, this.targetPitch, factor, MAX_DEGREES_PER_TICK);
    }

    /** {@code factor} tick  */
    private static float approach(float current, float target, float factor, float maxStep) {
        float difference = target - current;
        if (Math.abs(difference) <= SNAP_EPSILON_DEGREES) return target;
        float step = Mth.clamp(difference * factor, -maxStep, maxStep);
        if (Math.abs(step) > Math.abs(difference)) step = difference;
        return current + step;
    }


    /**  yaw */
    public float yaw() {
        return yaw;
    }

    /**  pitch */
    public float pitch() {
        return pitch;
    }

    /**  yaw/ */
    public float targetYaw() {
        return targetYaw;
    }

    /**  pitch/ */
    public float targetPitch() {
        return targetPitch;
    }

    /**  */
    public boolean settled() {
        return Math.abs(targetYaw - yaw) <= SNAP_EPSILON_DEGREES
                && Math.abs(targetPitch - pitch) <= SNAP_EPSILON_DEGREES;
    }

    /**  {@link #MAX_PITCH}  */
    public static float clampPitch(float pitch) {
        if (pitch > MAX_PITCH) return MAX_PITCH;
        if (pitch < -MAX_PITCH) return -MAX_PITCH;
        return pitch;
    }
}

