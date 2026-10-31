package org.tdddd.epca.impl.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Client-only visual settings that are exposed as sliders inside the vanilla Video Settings screen
 * (see {@code VideoSettingsScreenMixin} / {@link EpcaVideoOptions}).
 *
 * <p>Two knobs, both about the dismemberment gib burst:</p>
 * <ul>
 *   <li>{@link #getGibLimit()} - how many gib pieces a single death may produce. Slider range is
 *       {@code 0 .. 64} plus a final "Max" step ({@link #GIB_LIMIT_UNLIMITED}) that means "use every
 *       body part the model has, no cap". Default {@link #GIB_LIMIT_DEFAULT} = 7, which is the value
 *       this used to be hard-coded to.</li>
 *   <li>{@link #getGibMultiplier()} - debug multiplier applied on top of the piece count, {@code 1 .. 500}.
 *       Default 1. Raising it multiplies the pieces of every burst, which is how you can stress-test the
 *       renderer without touching the limit slider.</li>
 * </ul>
 *
 * <p>On 1.20.1 the pieces are spawned by the bundled {@code epca_dismemberment} mod, so every change is
 * pushed into its {@code DismemberSettings} too; {@link #syncToCompanion()} does the initial push once the
 * client is up.</p>
 *
 * <p>Values live in {@code config/epca-client.json} next to the vanilla {@code options.txt}, so they
 * survive a restart without adding a second config system to the mod. Everything is guarded: a missing,
 * unreadable or corrupt file falls back to the defaults instead of throwing during screen construction.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EpcaClientSettings {
    /** Top slider step: "Max" (unlimited). Kept above 64 so the slider can offer it as its own step. */
    public static final int GIB_LIMIT_UNLIMITED = 65;
    /** Default piece limit (the value this used to be hard-coded to). */
    public static final int GIB_LIMIT_DEFAULT = 7;
    public static final int GIB_LIMIT_MIN = 0;
    /** Default debug multiplier. */
    public static final int GIB_MULTIPLIER_DEFAULT = 1;
    public static final int GIB_MULTIPLIER_MAX = 500;
    public static final int GIB_MULTIPLIER_MIN = 1;

    /**
     * Hard safety valve on the final piece count. A 500x multiplier on a big model can ask for tens of
     * thousands of render objects; clamping here keeps an accidental extreme setting from freezing the
     * client outright.
     */
    public static final int SAFETY_MAX_PIECES = 4096;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "epca-client.json";
    private static final String KEY_GIB_LIMIT = "gibLimit";
    private static final String KEY_GIB_MULTIPLIER = "gibMultiplier";

    private static boolean loaded;
    private static int gibLimit = GIB_LIMIT_DEFAULT;
    private static int gibMultiplier = GIB_MULTIPLIER_DEFAULT;

    private EpcaClientSettings() { }

    // ────────── Values ──────────

    /** 0..64, or {@link #GIB_LIMIT_UNLIMITED} for "no cap". */
    public static int getGibLimit() {
        ensureLoaded();
        return gibLimit;
    }

    public static void setGibLimit(int value) {
        ensureLoaded();
        gibLimit = clamp(value, GIB_LIMIT_MIN, GIB_LIMIT_UNLIMITED);
        save();
        applyToCompanion();
    }

    public static boolean isGibLimitUnlimited() {
        return getGibLimit() >= GIB_LIMIT_UNLIMITED;
    }

    public static int getGibMultiplier() {
        ensureLoaded();
        return gibMultiplier;
    }

    public static void setGibMultiplier(int value) {
        ensureLoaded();
        gibMultiplier = clamp(value, GIB_MULTIPLIER_MIN, GIB_MULTIPLIER_MAX);
        save();
        applyToCompanion();
    }

    /**
     * Resolves how many gib pieces a single burst should contain.
     *
     * <p>{@code baseParts} is the number of body parts the model offered. The two sliders have
     * deliberately different rules:</p>
     * <ul>
     *   <li>the <b>limit</b> slider never exceeds the part count - it selects how much of the model is
     *       used, so "Max" means "every body part, one piece each", and no part is ever repeated;</li>
     *   <li>the <b>multiplier</b> is applied on top of that budget and may overshoot the part count -
     *       it is the debug knob, so it duplicates parts to reach the requested amount.</li>
     * </ul>
     *
     * <p>Default 7 x 1 therefore behaves exactly like the old hard-coded cap.</p>
     *
     * @return the piece count, clamped to {@code [0, }{@link #SAFETY_MAX_PIECES}{@code ]}
     */
    public static int resolvePieceCount(int baseParts) {
        if (baseParts <= 0) return 0;
        int budget = isGibLimitUnlimited() ? baseParts : Math.min(getGibLimit(), baseParts);
        if (budget <= 0) return 0;
        long wanted = (long) budget * getGibMultiplier();
        return (int) Math.min(wanted, SAFETY_MAX_PIECES);
    }

    /** Pushes the current values into the bundled dismemberment mod (no-op when it is missing). */
    public static void syncToCompanion() {
        ensureLoaded();
        applyToCompanion();
    }

    private static void applyToCompanion() {
        try {
            me.ichun.mods.mobdismemberment.common.core.DismemberSettings.maxPieces =
                    gibLimit >= GIB_LIMIT_UNLIMITED
                            ? me.ichun.mods.mobdismemberment.common.core.DismemberSettings.UNLIMITED
                            : gibLimit;
            me.ichun.mods.mobdismemberment.common.core.DismemberSettings.pieceMultiplier = gibMultiplier;
        } catch (Throwable ignored) {
            // Companion mod absent (or an older build): EPCA keeps working on its own defaults.
        }
    }

    // ────────── Persistence ──────────

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        File file = settingsFile();
        if (file == null || !file.isFile()) {
            applyToCompanion();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has(KEY_GIB_LIMIT)) {
                gibLimit = clamp(json.get(KEY_GIB_LIMIT).getAsInt(), GIB_LIMIT_MIN, GIB_LIMIT_UNLIMITED);
            }
            if (json.has(KEY_GIB_MULTIPLIER)) {
                gibMultiplier = clamp(json.get(KEY_GIB_MULTIPLIER).getAsInt(), GIB_MULTIPLIER_MIN, GIB_MULTIPLIER_MAX);
            }
        } catch (Throwable ignored) {
            // Corrupt/unreadable file: keep the defaults rather than failing to open Video Settings.
        }
        applyToCompanion();
    }

    private static void save() {
        File file = settingsFile();
        if (file == null) return;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return;
            JsonObject json = new JsonObject();
            json.addProperty(KEY_GIB_LIMIT, gibLimit);
            json.addProperty(KEY_GIB_MULTIPLIER, gibMultiplier);
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Throwable ignored) {
        }
    }

    private static File settingsFile() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameDirectory == null) return null;
            return new File(new File(mc.gameDirectory, "config"), FILE_NAME);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }
}
