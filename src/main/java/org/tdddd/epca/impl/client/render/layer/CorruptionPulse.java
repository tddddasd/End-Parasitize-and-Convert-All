package org.tdddd.epca.impl.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 崩坏脉冲 —— 崩坏层强度的来源，同时提供几何层面的“鬼畜抖动”。
 *
 * <p>移植自 RottenRuinsSplendiding 的 {@code ItemTwitchHelper}：
 * 物品每隔约 3 秒进入一次约 0.25 秒的爆发窗口，窗口内强度按
 * {@code (1 - p²)·(0.5 + 0.5·|sin(10πp)|)} 起落，于是出现“突然闪一下又恢复”
 * 的崩坏感；不同物品用自身 hashCode 错开相位，避免所有物品同时抽搐。</p>
 *
 * <h3>两种驱动方式</h3>
 * <ol>
 *   <li><b>脉冲（默认）</b>：按上面的周期自动爆发。</li>
 *   <li><b>NBT 常驻</b>：给物品 NBT 写一个 {@code epca_corruption} 浮点（0–1），
 *       该值就直接作为常驻强度，例如
 *       {@code /data modify entity @s SelectedItem.tag.epca_corruption set value 0.6f}。
 *       适合“被感染后永久崩坏”的物品状态。</li>
 * </ol>
 */
public final class CorruptionPulse {

    /** NBT 键：存在时用它的值作为常驻强度（0–1），覆盖脉冲。 */
    public static final String NBT_INTENSITY = "epca_corruption";

    /** 两次爆发之间的间隔（刻）。 */
    private static final float BURST_INTERVAL_TICKS = 60f;   // ~3s
    /** 单次爆发的持续时长（刻）。 */
    private static final float BURST_DURATION_TICKS = 5f;    // ~0.25s
    /** 亚帧抖动的高频振荡。 */
    private static final float JITTER_FREQ = 55f;

    // —— 抖动幅度 ——
    private static final float MAX_TRANSLATE = 0.30f;
    private static final float MAX_SCALE = 0.40f;
    private static final float MAX_ROTATION_RAD = 0.55f;

    private CorruptionPulse() {
    }

    // ── 强度 ──────────────────────────────────────────────────────────

    /**
     * 当前崩坏强度（0 = 无崩坏）。
     *
     * @param gameTime {@code level.getGameTime()}
     */
    public static float intensity(ItemStack stack, long gameTime) {
        if (stack == null || stack.isEmpty()) {
            return 0f;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_INTENSITY)) {
            return Mth.clamp(tag.getFloat(NBT_INTENSITY), 0f, 1f);
        }
        return burstIntensity(stack, gameTime);
    }

    /**
     * 只算脉冲部分（忽略 NBT 常驻覆盖）。
     */
    public static float burstIntensity(ItemStack stack, long gameTime) {
        float t = (float) (gameTime % Integer.MAX_VALUE);
        int itemSeed = stack.getItem().hashCode();

        // 每个物品类型错开相位
        float phaseOffset = Math.abs(itemSeed % 9973) / 9973f * BURST_INTERVAL_TICKS;
        float phase = ((t + phaseOffset) % BURST_INTERVAL_TICKS) / BURST_INTERVAL_TICKS;

        float burstFraction = BURST_DURATION_TICKS / BURST_INTERVAL_TICKS;
        if (phase >= burstFraction) {
            return 0f; // 爆发窗口之外
        }

        float burstProgress = phase / burstFraction;
        float spike = (float) Math.abs(Math.sin(burstProgress * Math.PI * 10));
        return (1f - burstProgress * burstProgress) * (0.5f + 0.5f * spike);
    }

    /** 是否处于爆发窗口内。 */
    public static boolean isBursting(ItemStack stack, long gameTime) {
        return intensity(stack, gameTime) >= 0.005f;
    }

    // ── 几何抖动 ──────────────────────────────────────────────────────

    /**
     * 给物品叠加鬼畜抖动：位置跳变 + 非等比缩放拉伸 + 快速旋转。
     *
     * <p>每帧都可以安全调用；不在爆发窗口内会立刻返回。</p>
     */
    public static void applyTwitch(PoseStack poseStack, ItemStack stack, long gameTime) {
        applyTwitch(poseStack, stack, gameTime, 1.0f);
    }

    /**
     * 同上，额外乘一个强度倍率（来自 {@code ItemLayerConfig.strength()}）。
     */
    public static void applyTwitch(PoseStack poseStack, ItemStack stack, long gameTime, float strengthScale) {
        float intensity = intensity(stack, gameTime) * Mth.clamp(strengthScale, 0f, 4f);
        if (intensity < 0.005f) {
            return;
        }

        float t = (float) (gameTime % Integer.MAX_VALUE);
        int itemSeed = stack.getItem().hashCode();
        float jitterPhase = t * JITTER_FREQ;
        float s0 = itemSeed;
        float s1 = itemSeed * 1.37f + 17f;
        float s2 = itemSeed * 2.71f + 31f;

        // 1. 位置跳变
        float jx = (float) Math.sin(jitterPhase + s0) * intensity * MAX_TRANSLATE;
        float jy = (float) Math.cos(jitterPhase * 1.37f + s1) * intensity * MAX_TRANSLATE;
        float jz = (float) Math.sin(jitterPhase * 0.73f + s2) * intensity * MAX_TRANSLATE * 0.5f;
        poseStack.translate(jx, jy, jz);

        // 2. 非等比缩放拉伸
        float sx = 1f + (float) Math.sin(jitterPhase * 1.71f + s0 * 0.7f) * intensity * MAX_SCALE;
        float sy = 1f + (float) Math.cos(jitterPhase * 2.13f + s1 * 1.1f) * intensity * MAX_SCALE;
        float sz = 1f + (float) Math.sin(jitterPhase * 1.33f + s2 * 1.9f) * intensity * MAX_SCALE * 0.6f;
        poseStack.scale(sx, sy, sz);

        // 3. 快速旋转
        float rx = (float) Math.sin(jitterPhase * 0.91f + s0 * 2.1f) * intensity * MAX_ROTATION_RAD;
        float ry = (float) Math.cos(jitterPhase * 1.53f + s1 * 0.3f) * intensity * MAX_ROTATION_RAD;
        float rz = (float) Math.sin(jitterPhase * 1.17f + s2 * 3.3f) * intensity * MAX_ROTATION_RAD * 0.5f;
        if (Math.abs(rx) > 0.0001f) {
            poseStack.mulPose(Axis.XP.rotation(rx));
        }
        if (Math.abs(ry) > 0.0001f) {
            poseStack.mulPose(Axis.YP.rotation(ry));
        }
        if (Math.abs(rz) > 0.0001f) {
            poseStack.mulPose(Axis.ZP.rotation(rz));
        }
    }
}
