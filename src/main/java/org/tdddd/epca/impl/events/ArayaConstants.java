package org.tdddd.epca.impl.events;

public final class ArayaConstants {

    private ArayaConstants() {
    }

    public static final float DAMAGE = 444.0F;

    public static final float SLASH_LENGTH = 3.0F;

    public static final float SLASH_MIN_WIDTH = 0.05F;

    public static final float SLASH_MAX_WIDTH = 0.20F;

    public static final int SLASH_GROW_TICKS = 10;

    public static final float SLASH_ANGLE_DEGREES = 50.0F;

    public static final int SLASH_HOLD_TICKS = 60;

    public static final int SLASH_FADE_TICKS = 30;

    public static final float SLASH_BAND_WIDTH = 0.05F;

    public static final float SLASH_CORE_ALPHA = 0.95F;

    public static final float SLASH_HALO_ALPHA = 0.30F;

    public static final float SLASH_HALO_WIDTH_FACTOR = 2.70F;

    public static final float SLASH_BAND_ALPHA = 0.55F;

    public static final float SLASH_REFRACTION_RATIO = 0.0625F;

    public static final String TIANSHA_KEY = "epca:tiansha";

    public static final int TIANSHA_THRESHOLD = 9;

    public static final double BGM_RADIUS = 48.0D;

    public static final double BGM_STOP_RADIUS = 56.0D;

    public static final float BGM_VOLUME = 0.85F;

    public static final float BGM_PITCH = 1.0F;

    public static final int AURA_SYNC_INTERVAL_TICKS = 10;

    public static final double AURA_REPORT_RADIUS = BGM_STOP_RADIUS + 16.0D;

    public static final int AURA_MAX_ENTRIES = 16;

    public static final int FIRE_MIN_PER_WAVE = 8;

    public static final int FIRE_MAX_PER_WAVE = 17;

    public static final int FIRE_MAX_ACTIVE = 27;

    public static final double FIRE_RADIUS = 8.0D;

    public static final int FIRE_MIN_LIFETIME_TICKS = 500;

    public static final int FIRE_MAX_LIFETIME_TICKS = 700;

    public static final int FIRE_REROLL_TICKS = FIRE_MAX_LIFETIME_TICKS;

    public static final float FIRE_MIN_HEIGHT = 0.8F;

    public static final float FIRE_MAX_HEIGHT = 1.5F;

    public static final double RENDER_DISTANCE = 96.0D;

    public static float positionHash(long seed, int x, int y, int z) {
        long h = seed;
        h = h * 6364136223846793005L + (x * 0x9E3779B97F4A7C15L);
        h = h * 6364136223846793005L + (y * 0xC2B2AE3D27D4EB4FL);
        h = h * 6364136223846793005L + (z * 0x165667B19E3779F9L);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;

        return (float) ((h >>> 40) & 0xFFFFFFL) / (float) 0x1000000L;
    }

    public static float fireHeight(int x, int y, int z) {
        float t = positionHash(0x41_52_41_59_41_00_01L, x, y, z);
        return FIRE_MIN_HEIGHT + (FIRE_MAX_HEIGHT - FIRE_MIN_HEIGHT) * t;
    }

    public static int fireLifetimeTicks(int x, int y, int z) {
        float t = positionHash(0x41_52_41_59_41_00_02L, x, y, z);
        int span = FIRE_MAX_LIFETIME_TICKS - FIRE_MIN_LIFETIME_TICKS + 1;
        return FIRE_MIN_LIFETIME_TICKS + (int) (t * span);
    }
}
