package org.tdddd.epca.impl.events;

/**
 * Every tunable number of the "Alayavijnana / Araya staff" feature, in one place.
 *
 * <p>The feature is: a 穷尽灭杖 renamed by anvil to the name {@code KillStick#isAlayavijnana} already
 * checks kills another player with {@link #DAMAGE} points of {@code yawning_neko_api:minimum} damage,
 * which plays the vanilla trident throw sound and draws a white 50-degree oblique sword slash whose
 * transparent border refracts the scene; each such kill adds one to the staff's 天杀 counter; and from
 * {@link #TIANSHA_THRESHOLD} upwards the holder broadcasts a looping BGM and spawns render-only vanilla
 * fire blocks around itself.</p>
 *
 * <p>This class is deliberately loader-free: both the 1.20.1 tree and the 26.1.2 staging tree carry a
 * byte-identical twin of it apart from the package-independent imports, which is what lets
 * {@code build/javac-check/check-araya-parity.py} compare the two files token by token.</p>
 *
 * <h2>Design decisions recorded here</h2>
 * <ul>
 *   <li><b>BGM radius and the "too far" distance</b>: {@link #BGM_RADIUS} blocks around the holder is
 *       the join radius, {@link #BGM_STOP_RADIUS} the leave radius. They differ on purpose: a single
 *       radius would make the loop flicker on and off for a listener standing exactly on the edge, and
 *       the BGM is a long resource that must not restart every other tick. Between the two the state is
 *       kept as it is (hysteresis).</li>
 *   <li><b>Fire lifetime</b>: {@link #FIRE_MIN_LIFETIME_TICKS} to {@link #FIRE_MAX_LIFETIME_TICKS}.
 *       The server is authoritative for <i>where</i> and <i>when</i>: it rolls the lifetime and the
 *       height per block once and ships both in the sync packet, so every client sees the same fire
 *       appear and disappear at the same instant.</li>
 *   <li><b>Fire count</b>: {@link #FIRE_COUNT} blocks are active per holder at a time; they all
 *       expire together and are re-rolled, so the effect comes in waves rather than flickering block
 *       by block. {@link #FIRE_REROLL_TICKS} is that wave period and is the longest possible lifetime,
 *       so a wave is never cut short by its successor.</li>
 * </ul>
 */
public final class ArayaConstants {

    private ArayaConstants() {
    }

    // ------------------------------------------------------------------ damage

    /**
     * Damage of one hit of the named staff, in health points (2 points = 1 heart, exactly like the
     * {@code hurt(...)} argument vanilla uses).
     *
     * <p>Applied as a {@code yawning_neko_api:minimum} damage source, built the way the rest of the mod
     * builds one, and never through {@code Attributes.ATTACK_DAMAGE}: the "minimum" damage type is the
     * mod's own mechanism for damage that is not reduced by armour, enchantments or adaptation.</p>
     */
    public static final float DAMAGE = 444.0F;

    // ------------------------------------------------------------------ the slash

    /** Length of the slash, in blocks ("剑痕长3格"). */
    public static final float SLASH_LENGTH = 3.0F;

    /** Width of the glowing line at the start of its growth, in blocks. */
    public static final float SLASH_MIN_WIDTH = 0.05F;

    /** Width of the glowing line once it has fully grown, in blocks. */
    public static final float SLASH_MAX_WIDTH = 0.20F;

    /** Ticks the line takes to grow from {@link #SLASH_MIN_WIDTH} to {@link #SLASH_MAX_WIDTH}. */
    public static final int SLASH_GROW_TICKS = 10;

    /**
     * Angle of the slash against the horizontal, in degrees ("斜50°"): the blade rises along its own
     * length by {@code tan(50°) * dx}, i.e. it is a 50-degree oblique cut across the victim rather
     * than a vertical or a horizontal one.
     */
    public static final float SLASH_ANGLE_DEGREES = 50.0F;

    /** Ticks the slash is fully visible before it starts to fade ("持续存在3秒"). */
    public static final int SLASH_HOLD_TICKS = 60;

    /** Ticks the fade-out takes after {@link #SLASH_HOLD_TICKS}. */
    public static final int SLASH_FADE_TICKS = 30;

    /**
     * Width of the transparent, refracting border on each side of the glowing line, in blocks
     * ("剑痕周围0.05格的部分为透明，但会折射透明部分后面的景象").
     */
    public static final float SLASH_BAND_WIDTH = 0.05F;

    /** Opacity of the glowing line at full visibility. */
    public static final float SLASH_CORE_ALPHA = 0.95F;

    /** Opacity of the wide, dim halo drawn behind the glowing line. */
    public static final float SLASH_HALO_ALPHA = 0.30F;

    /** How much wider than the line the halo is. */
    public static final float SLASH_HALO_WIDTH_FACTOR = 2.70F;

    /** Opacity of the transparent refracting border at full visibility. */
    public static final float SLASH_BAND_ALPHA = 0.55F;

    /**
     * The refraction ratio: the frame-copy offset, in normalised device coordinates, per block of
     * distance from the blade's centreline.
     *
     * <p>{@code 1/16}. The border is {@link #SLASH_BAND_WIDTH} wide, so its outer edge shifts by
     * {@code 0.05/16 = 0.0031} in normalised device coordinates - about two pixels at 1080p, i.e. about
     * one band width. The glowing line's own half width is at most
     * {@code SLASH_MAX_WIDTH/2 = 0.1} blocks, so even its edge only shifts by 0.006, and the middle of
     * the blade does not shift at all. The diagonal direction of the shift lives in
     * {@code araya_slash.vsh}, because it is a screen-space direction that no vertex datum can carry.</p>
     */
    public static final float SLASH_REFRACTION_RATIO = 0.0625F;

    // ------------------------------------------------------------------ the 天杀 counter

    /**
     * NBT / custom-data key of the 天杀 counter on the staff stack. Deliberately namespace-free like the
     * neighbouring {@code epca:ender_blade} flag is namespaced - see {@code KillStick} for the existing
     * per-stack keys. The counter lives on the <b>stack</b>, so moving the staff, dropping it, putting
     * it in a chest and taking it out again all keep it.
     */
    public static final String TIANSHA_KEY = "epca:tiansha";

    /** Kills of other players needed before the aura starts ("当该值大于等于9时"). */
    public static final int TIANSHA_THRESHOLD = 9;

    // ------------------------------------------------------------------ the BGM

    /** Blocks within which a listener joins the BGM. */
    public static final double BGM_RADIUS = 48.0D;

    /**
     * Blocks beyond which a listener that already hears the BGM loses it again. Strictly greater than
     * {@link #BGM_RADIUS}, which is the hysteresis that keeps the loop from stuttering on the edge.
     */
    public static final double BGM_STOP_RADIUS = 56.0D;

    /** Volume of the BGM for a listener standing on top of the holder. */
    public static final float BGM_VOLUME = 0.85F;

    /** Pitch of the BGM; 1.0 is the pitch the OGG was authored at. */
    public static final float BGM_PITCH = 1.0F;

    // ------------------------------------------------------------------ the aura

    /** Ticks between two server sweeps that look for holders with an active aura. */
    public static final int AURA_SYNC_INTERVAL_TICKS = 10;

    /**
     * How far beyond {@link #BGM_STOP_RADIUS} the sweep still reports a holder to a listener. A listener
     * that is already hearing the BGM keeps the holder in its cache out to the stop radius, so the
     * server has to resolve the holder for it a little beyond that or the loop would be cut by a
     * reporting gap rather than by the distance.
     */
    public static final double AURA_REPORT_RADIUS = BGM_STOP_RADIUS + 16.0D;

    /** Cap of one aura batch; a normal server has far fewer simultaneous holders than this. */
    public static final int AURA_MAX_ENTRIES = 16;

    // ------------------------------------------------------------------ the fire blocks

    /** How many render-only fire blocks one holder keeps alive at a time. */
    public static final int FIRE_COUNT = 12;

    /** Radius, in blocks, around the holder that the fire blocks are placed in. */
    public static final double FIRE_RADIUS = 8.0D;

    /** Shortest lifetime of one fire block, in ticks (25 s). */
    public static final int FIRE_MIN_LIFETIME_TICKS = 500;

    /** Longest lifetime of one fire block, in ticks (35 s). */
    public static final int FIRE_MAX_LIFETIME_TICKS = 700;

    /** Ticks between two re-rolls of the fire field; the longest lifetime, so a wave is never cut. */
    public static final int FIRE_REROLL_TICKS = FIRE_MAX_LIFETIME_TICKS;

    /** Smallest height of a fire block, in blocks ("高度为0.8~1.5格"). */
    public static final float FIRE_MIN_HEIGHT = 0.8F;

    /** Largest height of a fire block, in blocks. */
    public static final float FIRE_MAX_HEIGHT = 1.5F;

    /** Blocks farther than this from the camera are not drawn at all. */
    public static final double RENDER_DISTANCE = 96.0D;

    /**
     * Deterministic per-position value in {@code [0,1)}.
     *
     * <p>Used for everything a client must reproduce without being told: the height and the lifetime of
     * one fire block, and the phase of its animation. A server-rolled value would have to be shipped per
     * block; a hash of the block position is stable across sessions, dimensions and reloads, so the
     * same block always lights up to the same height for the same length of time on every client.</p>
     */
    public static float positionHash(long seed, int x, int y, int z) {
        long h = seed;
        h = h * 6364136223846793005L + (x * 0x9E3779B97F4A7C15L);
        h = h * 6364136223846793005L + (y * 0xC2B2AE3D27D4EB4FL);
        h = h * 6364136223846793005L + (z * 0x165667B19E3779F9L);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        // 24 bits of mantissa are plenty and keep the result exactly representable as a float.
        return (float) ((h >>> 40) & 0xFFFFFFL) / (float) 0x1000000L;
    }

    /** Height of the fire block at one position, in blocks, inside the documented range. */
    public static float fireHeight(int x, int y, int z) {
        float t = positionHash(0x41_52_41_59_41_00_01L, x, y, z);
        return FIRE_MIN_HEIGHT + (FIRE_MAX_HEIGHT - FIRE_MIN_HEIGHT) * t;
    }

    /** Lifetime of the fire block at one position, in ticks, inside the documented range. */
    public static int fireLifetimeTicks(int x, int y, int z) {
        float t = positionHash(0x41_52_41_59_41_00_02L, x, y, z);
        int span = FIRE_MAX_LIFETIME_TICKS - FIRE_MIN_LIFETIME_TICKS + 1;
        return FIRE_MIN_LIFETIME_TICKS + (int) (t * span);
    }
}
