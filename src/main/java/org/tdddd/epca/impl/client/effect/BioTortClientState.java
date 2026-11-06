package org.tdddd.epca.impl.client.effect;

import net.minecraft.util.Mth;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BioTortSkillConstants;

public final class BioTortClientState {

    public static final float MARK_ANIMATION_MILLIS = 250.0F;

    private static int hotbarMask;

    private static long markAnimationStartMillis;

    private static long markExpireAtMillis;

    private static boolean corruptionActive;

    private static long corruptionStartNanos;

    private static float corruptionWindowSeconds;

    private static float corruptionDecaySeconds;

    private static long frameToken;

    private BioTortClientState() {
    }

    public static void applyHotbarMark(int slotMask, int durationTicks) {
        hotbarMask = slotMask & ((1 << (BioTortSkillConstants.MARKED_SLOT_MAX + 1)) - 1);
        markAnimationStartMillis = System.currentTimeMillis();
        markExpireAtMillis = markAnimationStartMillis + Math.max(1, durationTicks) * 50L;
        if (hotbarMask == 0) {

            markExpireAtMillis = markAnimationStartMillis;
        }
    }

    public static void clearHotbarMark() {
        hotbarMask = 0;
    }

    public static int hotbarMask() {
        return hotbarMask;
    }

    public static boolean hasHotbarMark() {
        return hotbarMask != 0 && System.currentTimeMillis() < markExpireAtMillis;
    }

    public static boolean isSlotMarked(int slot) {
        return slot >= 0 && slot <= BioTortSkillConstants.MARKED_SLOT_MAX
                && (hotbarMask & (1 << slot)) != 0;
    }

    public static float markAnimationProgress() {
        long elapsed = System.currentTimeMillis() - markAnimationStartMillis;
        return Mth.clamp(elapsed / MARK_ANIMATION_MILLIS, 0.0F, 1.0F);
    }

    public static float markRemainingFraction() {
        long total = Math.max(1L, markExpireAtMillis - markAnimationStartMillis);
        long remaining = markExpireAtMillis - System.currentTimeMillis();
        return Mth.clamp((float) remaining / (float) total, 0.0F, 1.0F);
    }

    public static void startCorruption(int corruptionTicks, int decayTicks) {
        corruptionActive = true;
        corruptionStartNanos = System.nanoTime();
        corruptionWindowSeconds = Math.max(0, corruptionTicks) / 20.0F;
        corruptionDecaySeconds = Math.max(0, decayTicks) / 20.0F;
    }

    public static void stopCorruption() {
        corruptionActive = false;
    }

    public static boolean corruptionActive() {
        return corruptionActive;
    }

    public static float corruptionElapsedSeconds() {
        if (!corruptionActive) {
            return 0.0F;
        }
        return (float) ((System.nanoTime() - corruptionStartNanos) / 1_000_000_000.0D);
    }

    public static float corruptionWindowSeconds() {
        return corruptionWindowSeconds;
    }

    public static float corruptionDecaySeconds() {
        return corruptionDecaySeconds;
    }

    public static float corruptionTotalSeconds() {
        return corruptionWindowSeconds + corruptionDecaySeconds;
    }

    public static long nextFrameToken() {
        return ++frameToken;
    }

    public static void clientTick() {
        if (hotbarMask != 0 && System.currentTimeMillis() >= markExpireAtMillis) {
            hotbarMask = 0;
        }
        if (corruptionActive && corruptionElapsedSeconds() >= corruptionTotalSeconds()) {
            corruptionActive = false;
        }
    }

    public static void clear() {
        hotbarMask = 0;
        markExpireAtMillis = 0L;
        corruptionActive = false;
        corruptionWindowSeconds = 0.0F;
        corruptionDecaySeconds = 0.0F;
    }
}
