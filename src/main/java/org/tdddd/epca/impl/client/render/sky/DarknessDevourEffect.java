package org.tdddd.epca.impl.client.render.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * 黑暗吞噬：天空先黑透，然后结界开始裂，最后大地也被吃掉。
 *
 * <h3>三个阶段</h3>
 * <ol>
 *   <li><b>天黑（独立前置阶段）</b>：黑暗从<b>天顶</b>沿 {@code dir.y} 一路往下扩散，
 *       直到连地平线以下都是黑的。这段时间是<b>额外加在演出最前面</b>的，
 *       期间结界破裂进度恒为 0 —— 所以不会有任何裂缝先冒出来。</li>
 *   <li><b>结界破裂</b>：天全黑之后才开始。裂隙虚空 / 断面 / 亮线 / 冲击波全部
 *       叠在天空着色器的最底层黑暗之上 —— "结界破碎的渲染叠加到黑色上面"。</li>
 *   <li><b>大地（接管雾）</b>：破裂阶段中段，雾再从世界边缘收到玩家脚下，把方块也吃掉。</li>
 * </ol>
 *
 * <h3>为什么天空不能用雾</h3>
 * 雾的距离按"到相机的距离"算，而世界边缘正好在地平线那一圈 ——
 * 于是会出现"边缘黑了、头顶的天空还是亮的"这种怪象。
 * 天空改用方向阈值（{@code dir.y}）来做前沿，就是从头顶盖下来，符合直觉。
 *
 * <h3>时间轴</h3>
 * <pre>
 *   [0, D)                 天黑阶段：darkProgress 0→1，破裂进度恒 0
 *   [D, D+R)               破裂阶段：破裂进度 0→1（大地黑暗在该阶段的 0.28→0.72 吃过来）
 *   D = 天黑时长（1 阶段 2.5s，每阶段 +0.25s，上限 5s）
 *   R = 破裂时长（1 阶段 5s，  每阶段 +2.5s， 上限 25s）
 *   总时长 = D + R（天黑是额外加在前面的）
 * </pre>
 */
public final class DarknessDevourEffect {

    /** 大地黑暗（雾）在**破裂阶段**里的时间窗：天黑完之后再从世界边缘吃过来。 */
    private static final float GROUND_RISE_START = 0.28f;
    private static final float GROUND_RISE_END = 0.72f;
    private static final float GROUND_FALL_START = 0.84f;
    private static final float GROUND_FALL_END = 1.00f;

    private DarknessDevourEffect() {
    }

    // ── 天空：从顶部扩散下来（独立的前置阶段）────────────────────────

    /**
     * 天空黑暗前沿的推进程度（0 = 完全没开始，1 = 连地平线以下都黑了）。
     *
     * <p>直接取 {@link SkyRuptureEffect#darkProgress()}：天黑是一个**独立的前置阶段**，
     * 它的时长加在结界破裂之前，所以天空会先黑透，然后才开始裂。</p>
     */
    public static float skyProgress() {
        return SkyRuptureEffect.darkProgress();
    }

    /**
     * 天空黑暗的不透明度：比前沿稍微滞后一点，让"变黑"有个渐浓的过程，
     * 而不是前沿一到就瞬间死黑。天黑阶段结束后恒为 1（天空一直是黑的），
     * 直到整段演出在末尾由 {@code fade()} 统一淡出。
     */
    public static float skyOpacity() {
        return Mth.clamp(skyProgress() * 1.15f, 0f, 1f);
    }

    // ── 大地：接管雾 ─────────────────────────────────────────────────

    /**
     * 大地被吞噬的程度：0 = 没有、1 = 黑暗已经完全压到玩家身上。
     */
    public static float devour() {
        return window(GROUND_RISE_START, GROUND_RISE_END, GROUND_FALL_START, GROUND_FALL_END);
    }

    /**
     * 接管地形雾。由 {@code FogRendererDevourMixin} 在 {@code FogRenderer.setupFog}
     * 的末尾调用（每帧会被调用多次，这里是幂等的）。
     *
     * @param renderDistance 原版传进来的渲染距离（世界边缘的位置）
     */
    public static void applyFog(float renderDistance) {
        float d = devour();
        if (d <= 0.002f) {
            // 不接管：保持原版雾（下界、水下、药水效果雾都不受影响）
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.isUnderWater() || mc.player.isInLava())) {
            // 水下/岩浆有自己的雾，别跟它抢
            return;
        }

        float far = Math.max(renderDistance, 24.0f);
        // 前沿：从世界边缘(≈far)一路收到 **负值**。
        // 负的 fogStart 很关键：原版雾是"距离越小越不受影响"，只有把起点压到负值，
        // 才能连脚下的方块、乃至手里的手都一起吃掉 —— 否则总有一小圈地面是亮的，
        // 达不到"大地也变成黑的"。
        float front = Mth.lerp(d, far * 1.05f, -8.0f);
        // 软边：一开始很宽（远处自然变暗），压到底时收窄（眼前直接黑）
        float softness = Mth.lerp(d, far * 0.30f, 10.0f);

        float start = front;
        float end = start + Math.max(softness, 2.5f);

        RenderSystem.setShaderFogStart(start);
        RenderSystem.setShaderFogEnd(end);
        // 用与天空黑暗、裂隙虚空同一个颜色，让"外面的黑暗"和"裂隙里的虚空"是同一个东西
        RenderSystem.setShaderFogColor(
                SkyRuptureEffect.voidRed(), SkyRuptureEffect.voidGreen(), SkyRuptureEffect.voidBlue());
    }

    /** 时间窗：rise 段升到 1，中间保持，fall 段降回 0。 */
    private static float window(float riseStart, float riseEnd, float fallStart, float fallEnd) {
        if (!SkyRuptureEffect.isActive()) {
            return 0f;
        }
        float u = SkyRuptureEffect.progress();
        float rise = smooth01((u - riseStart) / (riseEnd - riseStart));
        float fall = smooth01((u - fallStart) / (fallEnd - fallStart));
        return rise * (1.0f - fall);
    }

    private static float smooth01(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }
}
