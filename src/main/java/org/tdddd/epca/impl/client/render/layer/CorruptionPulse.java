package org.tdddd.epca.impl.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Quaternionf;

/**
 * Corruption pulse - the source of the corruption layer's strength, and the geometric "twitch".
 *
 * <p>Ported from the reference project's {@code ItemTwitchHelper}: an item enters a ~0.25 s burst
 * window roughly every 3 s, and inside the window the strength follows
 * {@code (1 - p^2) * (0.5 + 0.5 * |sin(10*pi*p)|)}, which reads as "flashes for a moment and then
 * recovers". Different items are phase-offset by their own hashCode so they do not all twitch at
 * once.</p>
 *
 * <h2>Two drive modes</h2>
 * <ol>
 *   <li><b>Burst (default)</b>: automatically pulses on the period above.</li>
 *   <li><b>NBT constant</b>: writing the float {@code epca_corruption} (0..1) into the item's custom
 *       data makes that value the permanent strength, for example
 *       {@code /data modify entity @s SelectedItem.components."minecraft:custom_data".epca_corruption set value 0.6f}.
 *       This suits "permanently corrupted while infected" item states.</li>
 * </ol>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * {@code ItemStack#getTag()} no longer exists: 26.1.2 item data lives in components, and the
 * equivalent of the old raw tag is the {@code minecraft:custom_data}
 * {@link CustomData} component. The NBT key and the 0..1 semantics are unchanged, so the command
 * above is the only thing that has to change for users.
 *
 * <p>The jitter magnitudes and frequency are unchanged from 1.20.1 ({@code JITTER_FREQ = 55},
 * translate 0.30, non-uniform scale 0.40, rotate 0.55 rad).</p>
 */
public final class CorruptionPulse {

    /** NBT key: when present its value (0..1) is the constant strength and overrides the burst. */
    public static final String NBT_INTENSITY = "epca_corruption";

    /** Interval between two bursts (ticks). */
    private static final float BURST_INTERVAL_TICKS = 60f;   // ~3s
    /** Duration of one burst (ticks). */
    private static final float BURST_DURATION_TICKS = 5f;    // ~0.25s
    /** High frequency oscillation of the sub-frame jitter. */
    private static final float JITTER_FREQ = 55f;

    // -- jitter magnitudes --
    private static final float MAX_TRANSLATE = 0.30f;
    private static final float MAX_SCALE = 0.40f;
    private static final float MAX_ROTATION_RAD = 0.55f;

    private CorruptionPulse() {
    }

    // -- strength -------------------------------------------------------------

    /**
     * Current corruption strength (0 = none).
     *
     * @param gameTime {@code level.getGameTime()}
     */
    public static float intensity(ItemStack stack, long gameTime) {
        if (stack == null || stack.isEmpty()) {
            return 0f;
        }
        // 26.1.2: the old ItemStack#getTag() is gone; the equivalent raw data is the custom_data
        // component. Read it defensively so a malformed tag cannot break rendering.
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.isEmpty() && tag.contains(NBT_INTENSITY)) {
            return Mth.clamp(tag.getFloatOr(NBT_INTENSITY, 0f), 0f, 1f);
        }
        return burstIntensity(stack, gameTime);
    }

    /** The burst part only (ignores the NBT constant override). */
    public static float burstIntensity(ItemStack stack, long gameTime) {
        float t = (float) (gameTime % Integer.MAX_VALUE);
        int itemSeed = stack.getItem().hashCode();

        // Phase-offset each item type.
        float phaseOffset = Math.abs(itemSeed % 9973) / 9973f * BURST_INTERVAL_TICKS;
        float phase = ((t + phaseOffset) % BURST_INTERVAL_TICKS) / BURST_INTERVAL_TICKS;

        float burstFraction = BURST_DURATION_TICKS / BURST_INTERVAL_TICKS;
        if (phase >= burstFraction) {
            return 0f; // outside the burst window
        }

        float burstProgress = phase / burstFraction;
        float spike = (float) Math.abs(Math.sin(burstProgress * Math.PI * 10));
        return (1f - burstProgress * burstProgress) * (0.5f + 0.5f * spike);
    }

    /** Whether the item is inside its burst window. */
    public static boolean isBursting(ItemStack stack, long gameTime) {
        return intensity(stack, gameTime) >= 0.005f;
    }

    // -- geometric jitter -----------------------------------------------------

    /**
     * Adds the twitch to the pose: position jumps, non-uniform scale stretch and fast rotation.
     *
     * <p>Safe to call every frame; returns immediately outside the burst window.</p>
     */
    public static void applyTwitch(PoseStack poseStack, ItemStack stack, long gameTime) {
        applyTwitch(poseStack, stack, gameTime, 1.0f);
    }

    /**
     * As above, multiplied by an extra strength factor (from {@link
     * org.tdddd.epca.impl.client.render.ItemLayerConfig#strength()}).
     */
    public static void applyTwitch(PoseStack poseStack, ItemStack stack, long gameTime,
                                   float strengthScale) {
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

        // 1. position jumps
        float jx = (float) Math.sin(jitterPhase + s0) * intensity * MAX_TRANSLATE;
        float jy = (float) Math.cos(jitterPhase * 1.37f + s1) * intensity * MAX_TRANSLATE;
        float jz = (float) Math.sin(jitterPhase * 0.73f + s2) * intensity * MAX_TRANSLATE * 0.5f;
        poseStack.translate(jx, jy, jz);

        // 2. non-uniform scale stretch
        float sx = 1f + (float) Math.sin(jitterPhase * 1.71f + s0 * 0.7f) * intensity * MAX_SCALE;
        float sy = 1f + (float) Math.cos(jitterPhase * 2.13f + s1 * 1.1f) * intensity * MAX_SCALE;
        float sz = 1f + (float) Math.sin(jitterPhase * 1.33f + s2 * 1.9f) * intensity * MAX_SCALE * 0.6f;
        poseStack.scale(sx, sy, sz);

        // 3. fast rotation
        float rx = (float) Math.sin(jitterPhase * 0.91f + s0 * 2.1f) * intensity * MAX_ROTATION_RAD;
        float ry = (float) Math.cos(jitterPhase * 1.53f + s1 * 0.3f) * intensity * MAX_ROTATION_RAD;
        float rz = (float) Math.sin(jitterPhase * 1.17f + s2 * 3.3f) * intensity * MAX_ROTATION_RAD * 0.5f;
        // 26.1.2: com.mojang.math.Axis was removed; the equivalent rotation is built from
        // org.joml.Quaternionf, which PoseStack#mulPose takes.
        if (Math.abs(rx) > 0.0001f) {
            poseStack.mulPose(new Quaternionf().rotationX(rx));
        }
        if (Math.abs(ry) > 0.0001f) {
            poseStack.mulPose(new Quaternionf().rotationY(ry));
        }
        if (Math.abs(rz) > 0.0001f) {
            poseStack.mulPose(new Quaternionf().rotationZ(rz));
        }
    }
}
