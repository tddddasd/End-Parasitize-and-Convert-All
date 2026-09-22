package org.tdddd.epca.impl.client.render.sky;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 世界结界的状态机：什么时候破、破多久、破多狠。
 *
 * <h3>时长：跟着阶段树走，1 阶段 5 秒，上限 25 秒</h3>
 * <pre>
 *   时长 = clamp(5 + (阶段 - 1) × 2.5, 5, 25)   秒
 *
 *   阶段 1 → 5s    阶段 4 → 12.5s   阶段 7 → 20s     阶段 10 → 25s
 *   阶段 2 → 7.5s  阶段 5 → 15s     阶段 8 → 22.5s   阶段 13 → 25s
 *   阶段 3 → 10s   阶段 6 → 17.5s   阶段 9 → 25s     （9 阶段起封顶）
 * </pre>
 *
 * <h3>破损程度：阶段树里越低越破</h3>
 * {@code breakAmount = clamp((阶段 + 2) / 15, 0.08, 1)}，
 * 也就是把 -2 … 13 的结界完整度区间归一化到 0.08 … 1。它同时驱动：
 * <ul>
 *   <li>裂纹密度与宽度（越破越密越宽）</li>
 *   <li>整片碎裂的比例（越破越多种细胞脱落）</li>
 *   <li>震源数量（低阶段只有 1 个，高阶段 3 个一起裂）</li>
 *   <li>辉光颜色（青白 → 紫红，结界能量被腐蚀）</li>
 *   <li>整片天空的虚空渗漏（越破天空越薄越暗）</li>
 * </ul>
 *
 * <h3>时间推进</h3>
 * 进度用「渲染帧之间的真实时间差」推进，并把单帧步进钳到 0.25 秒：
 * 这样暂停游戏、切出窗口、卡顿时不会被一次性跳过去，效果始终看得完整。
 */
public final class SkyRuptureEffect {

    /** 1 阶段的持续时间（秒）。 */
    public static final double BASE_DURATION_SECONDS = 5.0;
    /** 每提升一个阶段增加的时长（秒）。 */
    public static final double DURATION_STEP_PER_STAGE = 2.5;
    /** 时长上限（秒）。 */
    public static final double MAX_DURATION_SECONDS = 25.0;

    /**
     * "天黑"阶段：单独加在结界破裂**之前**，让天空先黑透，再开始裂。
     * <p>这段时间是<b>额外追加</b>的 —— 总时长 = 天黑 + 破裂，
     * 所以 1 阶段现在是 2.5 + 5 = 7.5 秒。</p>
     */
    public static final double DARK_BASE_SECONDS = 2.5;
    public static final double DARK_STEP_PER_STAGE = 0.25;
    public static final double DARK_MAX_SECONDS = 5.0;

    /** 结界完整度归零（完全损坏）对应的阶段。 */
    public static final int FULLY_BROKEN_STAGE = 13;
    /** 相位起点对应的阶段（结界 100% 完好）。 */
    public static final int INTACT_STAGE = -2;

    /** 单帧最多推进的秒数，防止暂停后一次性跳完。 */
    private static final double MAX_FRAME_STEP = 0.25;
    /** 从这一比例开始淡出（1.0 = 结束），比例是相对**破裂阶段**的。 */
    private static final double FADE_OUT_START = 0.75;

    private static boolean active;
    private static int stage;
    private static double elapsedSeconds;
    /** 总时长 = 天黑 + 破裂。 */
    private static double durationSeconds;
    /** 天黑阶段时长。 */
    private static double darkSeconds;
    /** 破裂阶段时长（= 阶段对应的原时长）。 */
    private static double ruptureSeconds;
    private static float breakAmount;
    private static long lastFrameNanos;

    private static float seed;
    /** 裂纹场 / 噪声场的随机偏移：让每次触发的碎片布局都不一样。 */
    private static float patternOffsetX;
    private static float patternOffsetY;

    private SkyRuptureEffect() {
    }

    // ── 阶段 → 参数 ───────────────────────────────────────────────────

    /** 破裂阶段的时长（秒）：1 阶段 5s，每阶段 +2.5s，最多 25s。 */
    public static double durationForStage(int stage) {
        double raw = BASE_DURATION_SECONDS + Math.max(0, stage - 1) * DURATION_STEP_PER_STAGE;
        return Mth.clamp(raw, BASE_DURATION_SECONDS, MAX_DURATION_SECONDS);
    }

    /** 天黑阶段的时长（秒）：1 阶段 2.5s，每阶段 +0.25s，最多 5s。 */
    public static double darkDurationForStage(int stage) {
        double raw = DARK_BASE_SECONDS + Math.max(0, stage - 1) * DARK_STEP_PER_STAGE;
        return Mth.clamp(raw, DARK_BASE_SECONDS, DARK_MAX_SECONDS);
    }

    /** 该阶段的破损程度（0..1）：阶段树越低越破。 */
    public static float breakAmountForStage(int stage) {
        float span = (float) (FULLY_BROKEN_STAGE - INTACT_STAGE);
        return Mth.clamp((stage - INTACT_STAGE) / span, 0.08f, 1.0f);
    }

    // ── 触发 / 推进 ──────────────────────────────────────────────────

    /**
     * 触发一次世界结界破损。重复触发会直接重新开始（新阶段优先）。
     */
    public static void trigger(int newStage) {
        trigger(newStage, RandomSource.create());
    }

    /**
     * 触发一次世界结界破损，使用指定随机源（便于复现 / 调试）。
     */
    public static void trigger(int newStage, RandomSource random) {
        stage = newStage;
        darkSeconds = darkDurationForStage(newStage);
        ruptureSeconds = durationForStage(newStage);
        // 天黑是**额外加在前面**的，所以总时长是两者之和
        durationSeconds = darkSeconds + ruptureSeconds;
        breakAmount = breakAmountForStage(newStage);
        elapsedSeconds = 0.0;
        lastFrameNanos = System.nanoTime();
        active = true;

        seed = random.nextFloat() * 100.0f;

        // 破裂点不是固定的几个震源，而是"十面八方"：着色器里用一张铺满天空的
        // Voronoi 碎片网络 + 低频噪声决定各处开裂时刻。这里只给场一个随机偏移，
        // 让每次触发的碎片布局都不一样。
        patternOffsetX = random.nextFloat() * 64.0f;
        patternOffsetY = random.nextFloat() * 64.0f;

        org.tdddd.epca.impl.epca.LOGGER.info(
                "[epca-render] 世界结界破损触发：阶段 {} → 天黑 {}s + 破裂 {}s（共 {}s），破损程度 {}",
                newStage, String.format("%.1f", darkSeconds), String.format("%.1f", ruptureSeconds),
                String.format("%.1f", durationSeconds), String.format("%.2f", breakAmount));
    }

    /**
     * 每帧推进一次（由渲染器调用）。未激活时是空操作。
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

    /** 立即结束（切维度 / 退出世界时用）。 */
    public static void stop() {
        active = false;
        elapsedSeconds = 0.0;
    }

    // ── 查询 ─────────────────────────────────────────────────────────

    public static boolean isActive() {
        return active;
    }

    /**
     * 0..1 的破碎推进。**天黑阶段恒为 0** —— 这就是"天先黑完再出现结界渲染"的实现：
     * 所有裂纹 / 波前 / 冲击环 / 闪白都基于这个进度，天黑期间它们自然全是 0。
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
     * 0..1 的天黑推进：天黑阶段内 0→1，之后恒为 1（天空一直保持黑，直到整段演出淡出）。
     */
    public static float darkProgress() {
        if (darkSeconds <= 0) {
            return 1f;
        }
        return Mth.clamp((float) (elapsedSeconds / darkSeconds), 0f, 1f);
    }

    /** 0..1 的不透明度包络：保持到 75% 然后淡出。 */
    public static float fade() {
        float u = progress();
        if (u < FADE_OUT_START) {
            return 1f;
        }
        float k = (u - (float) FADE_OUT_START) / (1f - (float) FADE_OUT_START);
        k = Mth.clamp(k, 0f, 1f);
        // smoothstep 淡出
        return 1f - k * k * (3f - 2f * k);
    }

    /** 已流逝秒数（驱动局部动画）。 */
    public static float elapsedSeconds() {
        return (float) elapsedSeconds;
    }

    public static int stage() {
        return stage;
    }

    /** 总时长（秒）= 天黑 + 破裂。 */
    public static double durationSeconds() {
        return durationSeconds;
    }

    /** 天黑阶段时长（秒）。 */
    public static double darkDurationSeconds() {
        return darkSeconds;
    }

    /** 破裂阶段时长（秒）。 */
    public static double ruptureDurationSeconds() {
        return ruptureSeconds;
    }

    /** 是否还在天黑阶段（破裂尚未开始）。 */
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
     * 裂纹场 / 噪声场的随机偏移 X/Y。
     *
     * <p>图案铺在「天顶平面参数空间」里（{@code p = dir.xz / (1 + dir.y) × 2.2}，
     * |p| = 0 是正上方），着色器用一张 Voronoi 碎片网络铺满它，
     * 各处的开裂时刻由一个低频连续噪声场决定 —— 所以是"十面八方"陆续裂开，
     * 而不是从固定几个点扩散。这里的偏移让每次触发的碎片布局都不一样。</p>
     */
    public static float patternOffsetX() {
        return patternOffsetX;
    }

    public static float patternOffsetY() {
        return patternOffsetY;
    }

    // ── 视觉调色（随破损程度变化）────────────────────────────────────

    /** 结界能量光：青白 → 紫红。 */
    public static float rimRed() {
        return Mth.lerp(breakAmount, 0.55f, 0.85f);
    }

    public static float rimGreen() {
        return Mth.lerp(breakAmount, 0.85f, 0.25f);
    }

    public static float rimBlue() {
        return Mth.lerp(breakAmount, 1.00f, 1.00f);
    }

    /** 虚空色：近黑带紫，越破越紫。 */
    public static float voidRed() {
        return Mth.lerp(breakAmount, 0.02f, 0.07f);
    }

    public static float voidGreen() {
        return 0.0f;
    }

    public static float voidBlue() {
        return Mth.lerp(breakAmount, 0.05f, 0.11f);
    }

    /** 闪光色：冷白 → 淡紫。 */
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
