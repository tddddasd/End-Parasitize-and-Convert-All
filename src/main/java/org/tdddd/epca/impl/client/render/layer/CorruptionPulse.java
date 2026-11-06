package org.tdddd.epca.impl.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class CorruptionPulse {

    public static final String NBT_INTENSITY = "epca_corruption";

    private static final float BURST_INTERVAL_TICKS = 60f;

    private static final float BURST_DURATION_TICKS = 5f;

    private static final float JITTER_FREQ = 55f;

    private static final float MAX_TRANSLATE = 0.30f;
    private static final float MAX_SCALE = 0.40f;
    private static final float MAX_ROTATION_RAD = 0.55f;

    private CorruptionPulse() {
    }

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

    public static float burstIntensity(ItemStack stack, long gameTime) {
        float t = (float) (gameTime % Integer.MAX_VALUE);
        int itemSeed = stack.getItem().hashCode();

        float phaseOffset = Math.abs(itemSeed % 9973) / 9973f * BURST_INTERVAL_TICKS;
        float phase = ((t + phaseOffset) % BURST_INTERVAL_TICKS) / BURST_INTERVAL_TICKS;

        float burstFraction = BURST_DURATION_TICKS / BURST_INTERVAL_TICKS;
        if (phase >= burstFraction) {
            return 0f;
        }

        float burstProgress = phase / burstFraction;
        float spike = (float) Math.abs(Math.sin(burstProgress * Math.PI * 10));
        return (1f - burstProgress * burstProgress) * (0.5f + 0.5f * spike);
    }

    public static boolean isBursting(ItemStack stack, long gameTime) {
        return intensity(stack, gameTime) >= 0.005f;
    }

    public static void applyTwitch(PoseStack poseStack, ItemStack stack, long gameTime) {
        applyTwitch(poseStack, stack, gameTime, 1.0f);
    }

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

        float jx = (float) Math.sin(jitterPhase + s0) * intensity * MAX_TRANSLATE;
        float jy = (float) Math.cos(jitterPhase * 1.37f + s1) * intensity * MAX_TRANSLATE;
        float jz = (float) Math.sin(jitterPhase * 0.73f + s2) * intensity * MAX_TRANSLATE * 0.5f;
        poseStack.translate(jx, jy, jz);

        float sx = 1f + (float) Math.sin(jitterPhase * 1.71f + s0 * 0.7f) * intensity * MAX_SCALE;
        float sy = 1f + (float) Math.cos(jitterPhase * 2.13f + s1 * 1.1f) * intensity * MAX_SCALE;
        float sz = 1f + (float) Math.sin(jitterPhase * 1.33f + s2 * 1.9f) * intensity * MAX_SCALE * 0.6f;
        poseStack.scale(sx, sy, sz);

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
