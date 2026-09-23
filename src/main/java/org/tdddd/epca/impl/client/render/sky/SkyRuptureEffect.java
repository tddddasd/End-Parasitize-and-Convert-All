package org.tdddd.epca.impl.client.render.sky;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * State machine of the world barrier rupture: when it breaks, for how long and how hard.
 *
 * <p>Ported 1:1 from the 1.20.1 twin; only the log line and the comments are English now.</p>
 *
 * <h2>Duration: driven by the evolution stage, 5 s at stage 1, capped at 25 s</h2>
 * <pre>
 *   duration = clamp(5 + (stage - 1) * 2.5, 5, 25)   seconds
 *
 *   stage 1  -> 5s     stage 4  -> 12.5s   stage 7  -> 20s     stage 10 -> 25s
 *   stage 2  -> 7.5s   stage 5  -> 15s     stage 8  -> 22.5s   stage 13 -> 25s
 *   stage 3  -> 10s    stage 6  -> 17.5s   stage 9  -> 25s     (capped from stage 9)
 * </pre>
 *
 * <h2>Break amount: the lower the stage, the more broken the barrier</h2>
 * {@code breakAmount = clamp((stage + 2) / 15, 0.08, 1)}, i.e. the integrity range -2 .. 13
 * normalised onto 0.08 .. 1. It drives crack density and width, how much of the sky shatters away,
 * the glow colour (cyan-white to purple-red as the barrier energy is corroded) and how much void
 * leaks through.
 *
 * <h2>Time base</h2>
 * Progress advances with the real elapsed time between rendered frames, clamped to 0.25 s per frame,
 * so pausing the game, tabbing out or a frame spike cannot skip the effect.
 */
public final class SkyRuptureEffect {

    /** Duration of stage 1 (seconds). */
    public static final double BASE_DURATION_SECONDS = 5.0;
    /** Duration added per stage (seconds). */
    public static final double DURATION_STEP_PER_STAGE = 2.5;
    /** Duration ceiling (seconds). */
    public static final double MAX_DURATION_SECONDS = 25.0;

    /**
     * The "darkening" phase: an extra phase appended <em>before</em> the rupture so the sky goes fully
     * dark first and only then starts to crack.
     *
     * <p>It is <b>additional</b> to the rupture, so the total is darkening + rupture: stage 1 is
     * 2.5 + 5 = 7.5 s.</p>
     */
    public static final double DARK_BASE_SECONDS = 2.5;
    public static final double DARK_STEP_PER_STAGE = 0.25;
    public static final double DARK_MAX_SECONDS = 5.0;

    /** Stage at which the barrier integrity reaches zero (fully broken). */
    public static final int FULLY_BROKEN_STAGE = 13;
    /** Stage that corresponds to the start of the phase (barrier 100% intact). */
    public static final int INTACT_STAGE = -2;

    /** Maximum progress advanced by a single frame, so a pause cannot skip the effect. */
    private static final double MAX_FRAME_STEP = 0.25;
    /** Fraction of the <em>rupture</em> phase at which the fade out starts (1.0 = end). */
    private static final double FADE_OUT_START = 0.75;

    private static boolean active;
    private static int stage;
    private static double elapsedSeconds;
    /** Total duration = darkening + rupture. */
    private static double durationSeconds;
    /** Duration of the darkening phase. */
    private static double darkSeconds;
    /** Duration of the rupture phase (the stage's original duration). */
    private static double ruptureSeconds;
    private static float breakAmount;
    private static long lastFrameNanos;

    private static float seed;
    /** Random offset of the crack / noise field, so every trigger has a different shard layout. */
    private static float patternOffsetX;
    private static float patternOffsetY;

    private SkyRuptureEffect() {
    }

    // -- stage -> parameters --------------------------------------------------

    /** Rupture duration (seconds): 5 s at stage 1, +2.5 s per stage, at most 25 s. */
    public static double durationForStage(int stage) {
        double raw = BASE_DURATION_SECONDS + Math.max(0, stage - 1) * DURATION_STEP_PER_STAGE;
        return Mth.clamp(raw, BASE_DURATION_SECONDS, MAX_DURATION_SECONDS);
    }

    /** Darkening duration (seconds): 2.5 s at stage 1, +0.25 s per stage, at most 5 s. */
    public static double darkDurationForStage(int stage) {
        double raw = DARK_BASE_SECONDS + Math.max(0, stage - 1) * DARK_STEP_PER_STAGE;
        return Mth.clamp(raw, DARK_BASE_SECONDS, DARK_MAX_SECONDS);
    }

    /** Break amount of that stage (0..1): the lower the stage, the more broken. */
    public static float breakAmountForStage(int stage) {
        float span = (float) (FULLY_BROKEN_STAGE - INTACT_STAGE);
        return Mth.clamp((stage - INTACT_STAGE) / span, 0.08f, 1.0f);
    }

    // -- trigger / advance ----------------------------------------------------

    /** Triggers one world barrier rupture. Triggering again simply restarts with the new stage. */
    public static void trigger(int newStage) {
        trigger(newStage, RandomSource.create());
    }

    /**
     * Triggers one world barrier rupture with an explicit random source (useful for reproducible
     * debugging).
     */
    public static void trigger(int newStage, RandomSource random) {
        stage = newStage;
        darkSeconds = darkDurationForStage(newStage);
        ruptureSeconds = durationForStage(newStage);
        // The darkening phase is additional and comes first, so the total is the sum.
        durationSeconds = darkSeconds + ruptureSeconds;
        breakAmount = breakAmountForStage(newStage);
        elapsedSeconds = 0.0;
        lastFrameNanos = System.nanoTime();
        active = true;

        seed = random.nextFloat() * 100.0f;

        // The rupture does not start from a few fixed epicentres but from "all directions at once":
        // the fragment shader covers the whole sky with a Voronoi shard network plus a low frequency
        // noise field that decides when each region opens. Java only supplies a random offset so
        // every trigger has a different shard layout. It travels to the shader packed into the
        // Pattern vertex attribute.
        patternOffsetX = random.nextFloat() * 64.0f;
        patternOffsetY = random.nextFloat() * 64.0f;

        org.tdddd.epca.impl.epca.LOGGER.info(
                "[epca-render] sky rupture triggered: stage {} -> darkening {}s + rupture {}s ({}s total), breakAmount {}",
                newStage, String.format("%.1f", darkSeconds), String.format("%.1f", ruptureSeconds),
                String.format("%.1f", durationSeconds), String.format("%.2f", breakAmount));
    }

    /** Advances one frame (called by the renderer). A no-op while inactive. */
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

    /** Ends immediately (on dimension change / leaving the world). */
    public static void stop() {
        active = false;
        elapsedSeconds = 0.0;
    }

    // -- queries --------------------------------------------------------------

    public static boolean isActive() {
        return active;
    }

    /**
     * 0..1 rupture progress. <b>Always 0 during the darkening phase</b>, which is what implements
     * "the sky darkens completely before anything is rendered": every crack, wave front, shock ring
     * and flash is derived from this progress, so during the darkening phase they are all naturally 0.
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
     * 0..1 darkening progress: 0 -&gt; 1 within the darkening phase, then constant 1 (the sky stays
     * dark until the whole performance fades out at the end).
     */
    public static float darkProgress() {
        if (darkSeconds <= 0) {
            return 1f;
        }
        return Mth.clamp((float) (elapsedSeconds / darkSeconds), 0f, 1f);
    }

    /** 0..1 opacity envelope: held until 75% and then faded out with a smoothstep. */
    public static float fade() {
        float u = progress();
        if (u < FADE_OUT_START) {
            return 1f;
        }
        float k = (u - (float) FADE_OUT_START) / (1f - (float) FADE_OUT_START);
        k = Mth.clamp(k, 0f, 1f);
        return 1f - k * k * (3f - 2f * k);
    }

    /** Elapsed seconds (drives the local animation). */
    public static float elapsedSeconds() {
        return (float) elapsedSeconds;
    }

    public static int stage() {
        return stage;
    }

    /** Total duration (seconds) = darkening + rupture. */
    public static double durationSeconds() {
        return durationSeconds;
    }

    /** Darkening duration (seconds). */
    public static double darkDurationSeconds() {
        return darkSeconds;
    }

    /** Rupture duration (seconds). */
    public static double ruptureDurationSeconds() {
        return ruptureSeconds;
    }

    /** True while still in the darkening phase (the rupture has not started). */
    public static boolean isDarkening() {
        return active && elapsedSeconds < darkSeconds;
    }

    public static float breakAmount() {
        return breakAmount;
    }

    /**
     * Per-trigger random seed.
     *
     * <p>Kept because the 1.20.1 effect had it, but note that it is <b>no longer sent to the
     * shader</b>: the 1.20.1 {@code seed} uniform was never read by {@code sky_rupture.fsh}, and it
     * was dropped to save a vertex slot. The retired seed uniform used to be threaded through the
     * renderer, and it is only reported in the diagnostic log now.</p>
     */
    public static float seed() {
        return seed;
    }

    /**
     * Random offset X / Y of the crack and noise fields.
     *
     * <p>The pattern is laid out in "zenith plane parameter space"
     * ({@code p = dir.xz / (1 + dir.y) * 2.2}, |p| = 0 is straight up) and the fragment shader covers
     * it with a Voronoi shard network; a low frequency continuous noise field decides when each region
     * opens, which is why regions break one after another from all directions instead of expanding
     * from a few points. This offset makes the shard layout different on every trigger.</p>
     */
    public static float patternOffsetX() {
        return patternOffsetX;
    }

    public static float patternOffsetY() {
        return patternOffsetY;
    }

    // -- palette (varies with the break amount) -------------------------------

    /** Barrier energy light: cyan-white to purple-red. */
    public static float rimRed() {
        return Mth.lerp(breakAmount, 0.55f, 0.85f);
    }

    public static float rimGreen() {
        return Mth.lerp(breakAmount, 0.85f, 0.25f);
    }

    public static float rimBlue() {
        return Mth.lerp(breakAmount, 1.00f, 1.00f);
    }

    /** Void colour: near black with a purple cast, more purple the more broken. */
    public static float voidRed() {
        return Mth.lerp(breakAmount, 0.02f, 0.07f);
    }

    public static float voidGreen() {
        return 0.0f;
    }

    public static float voidBlue() {
        return Mth.lerp(breakAmount, 0.05f, 0.11f);
    }

    /** Flash colour: cold white to pale purple. */
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
