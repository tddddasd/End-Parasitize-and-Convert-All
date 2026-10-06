package org.tdddd.epca.impl.client.render.sky;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 *
 *
 * <h3>1  5  25 </h3>
 * <pre>
 *    = clamp(5 + ( - 1)  2.5, 5, 25)
 *
 *    1  5s     4  12.5s    7  20s      10  25s
 *    2  7.5s   5  15s      8  22.5s    13  25s
 *    3  10s    6  17.5s    9  25s     9
 * </pre>
 *
 * <h3></h3>
 * {@code breakAmount = clamp(( + 2) / 15, 0.08, 1)}
 *  -2  13  0.08  1
 * <ul>
 *   <li></li>
 *   <li></li>
 *   <li> 1  3 </li>
 *   <li>  </li>
 *   <li></li>
 * </ul>
 *
 * <h3></h3>
 *  0.25
 *
 */
public final class SkyRuptureEffect {

    /** 1  */
    public static final double BASE_DURATION_SECONDS = 5.0;
    /**  */
    public static final double DURATION_STEP_PER_STAGE = 2.5;
    /**  */
    public static final double MAX_DURATION_SECONDS = 25.0;

    /**
     * ""****
     * <p><b></b>   =  +
     *  1  2.5 + 5 = 7.5 </p>
     */
    public static final double DARK_BASE_SECONDS = 2.5;
    public static final double DARK_STEP_PER_STAGE = 0.25;
    public static final double DARK_MAX_SECONDS = 5.0;

    /**  */
    public static final int FULLY_BROKEN_STAGE = 13;
    /**  100%  */
    public static final int INTACT_STAGE = -2;

    /**  */
    private static final double MAX_FRAME_STEP = 0.25;
    /** 1.0 = **** */
    private static final double FADE_OUT_START = 0.75;

    private static boolean active;
    private static int stage;
    private static double elapsedSeconds;
    /**  =  +  */
    private static double durationSeconds;
    /**  */
    private static double darkSeconds;
    /** =  */
    private static double ruptureSeconds;
    private static float breakAmount;
    private static long lastFrameNanos;

    private static float seed;
    /**  /  */
    private static float patternOffsetX;
    private static float patternOffsetY;

    private SkyRuptureEffect() {
    }


    /** 1  5s +2.5s 25s */
    public static double durationForStage(int stage) {
        double raw = BASE_DURATION_SECONDS + Math.max(0, stage - 1) * DURATION_STEP_PER_STAGE;
        return Mth.clamp(raw, BASE_DURATION_SECONDS, MAX_DURATION_SECONDS);
    }

    /** 1  2.5s +0.25s 5s */
    public static double darkDurationForStage(int stage) {
        double raw = DARK_BASE_SECONDS + Math.max(0, stage - 1) * DARK_STEP_PER_STAGE;
        return Mth.clamp(raw, DARK_BASE_SECONDS, DARK_MAX_SECONDS);
    }

    /** 0..1 */
    public static float breakAmountForStage(int stage) {
        float span = (float) (FULLY_BROKEN_STAGE - INTACT_STAGE);
        return Mth.clamp((stage - INTACT_STAGE) / span, 0.08f, 1.0f);
    }

    //   /

    /**
     *
     */
    public static void trigger(int newStage) {
        trigger(newStage, RandomSource.create());
    }

    /**
     *  /
     */
    public static void trigger(int newStage, RandomSource random) {
        stage = newStage;
        darkSeconds = darkDurationForStage(newStage);
        ruptureSeconds = durationForStage(newStage);
        // ****
        durationSeconds = darkSeconds + ruptureSeconds;
        breakAmount = breakAmountForStage(newStage);
        elapsedSeconds = 0.0;
        lastFrameNanos = System.nanoTime();
        active = true;

        seed = random.nextFloat() * 100.0f;

        // ""
        // Voronoi  +
        patternOffsetX = random.nextFloat() * 64.0f;
        patternOffsetY = random.nextFloat() * 64.0f;

        org.tdddd.epca.impl.epca.LOGGER.info(
                "[epca-render] 世界结界破损触发：阶段 {} → 天黑 {}s + 破裂 {}s（共 {}s），破损程度 {}",
                newStage, String.format("%.1f", darkSeconds), String.format("%.1f", ruptureSeconds),
                String.format("%.1f", durationSeconds), String.format("%.2f", breakAmount));
    }

    /**
     *
     */
    public static void update() {
        long now = System.nanoTime();
        if (!active) {
            lastFrameNanos = now;
            return;
        }
        double dt = (now - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = now;

        if (dt < 0) {
            dt = 0;
        } else if (dt > MAX_FRAME_STEP) {
            dt = MAX_FRAME_STEP;
        }

        elapsedSeconds += dt;
        if (elapsedSeconds >= durationSeconds) {
            active = false;
            elapsedSeconds = durationSeconds;
        }
    }

    /**  /  */
    public static void stop() {
        active = false;
        elapsedSeconds = 0.0;
    }


    public static boolean isActive() {
        return active;
    }

    /**
     * 0..1 ** 0**  ""
     *  /  /  /  0
     */
    public static float progress() {
        if (ruptureSeconds <= 0) {
            return 1f;
        }
        if (elapsedSeconds <= darkSeconds) {
            return 0f;
        }
        return Mth.clamp((float) ((elapsedSeconds - darkSeconds) / ruptureSeconds), 0f, 1f);
    }

    /**
     * 0..1  01 1
     */
    public static float darkProgress() {
        if (darkSeconds <= 0) {
            return 1f;
        }
        return Mth.clamp((float) (elapsedSeconds / darkSeconds), 0f, 1f);
    }

    /** 0..1  75%  */
    public static float fade() {
        float u = progress();
        if (u < FADE_OUT_START) {
            return 1f;
        }
        float k = (u - (float) FADE_OUT_START) / (1f - (float) FADE_OUT_START);
        k = Mth.clamp(k, 0f, 1f);
        // smoothstep
        return 1f - k * k * (3f - 2f * k);
    }

    /**  */
    public static float elapsedSeconds() {
        return (float) elapsedSeconds;
    }

    public static int stage() {
        return stage;
    }

    /** =  +  */
    public static double durationSeconds() {
        return durationSeconds;
    }

    /**  */
    public static double darkDurationSeconds() {
        return darkSeconds;
    }

    /**  */
    public static double ruptureDurationSeconds() {
        return ruptureSeconds;
    }

    /**  */
    public static boolean isDarkening() {
        return active && elapsedSeconds < darkSeconds;
    }

    public static float breakAmount() {
        return breakAmount;
    }

    public static float seed() {
        return seed;
    }

    /**
     *  /  X/Y
     *
     * <p>{@code p = dir.xz / (1 + dir.y)  2.2}
     * |p| = 0  Voronoi
     *   ""
     * </p>
     */
    public static float patternOffsetX() {
        return patternOffsetX;
    }

    public static float patternOffsetY() {
        return patternOffsetY;
    }


    /**    */
    public static float rimRed() {
        return Mth.lerp(breakAmount, 0.55f, 0.85f);
    }

    public static float rimGreen() {
        return Mth.lerp(breakAmount, 0.85f, 0.25f);
    }

    public static float rimBlue() {
        return Mth.lerp(breakAmount, 1.00f, 1.00f);
    }

    /**  */
    public static float voidRed() {
        return Mth.lerp(breakAmount, 0.02f, 0.07f);
    }

    public static float voidGreen() {
        return 0.0f;
    }

    public static float voidBlue() {
        return Mth.lerp(breakAmount, 0.05f, 0.11f);
    }

    /**    */
    public static float flashRed() {
        return Mth.lerp(breakAmount, 0.85f, 0.92f);
    }

    public static float flashGreen() {
        return Mth.lerp(breakAmount, 0.95f, 0.85f);
    }

    public static float flashBlue() {
        return 1.0f;
    }
}

