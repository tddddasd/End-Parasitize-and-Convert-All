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

@OnlyIn(Dist.CLIENT)
public final class EpcaClientSettings {

    public static final int GIB_LIMIT_UNLIMITED = 65;

    public static final int GIB_LIMIT_DEFAULT = 7;
    public static final int GIB_LIMIT_MIN = 0;

    public static final int GIB_MULTIPLIER_DEFAULT = 1;
    public static final int GIB_MULTIPLIER_MAX = 500;
    public static final int GIB_MULTIPLIER_MIN = 1;

    public static final int SAFETY_MAX_PIECES = 4096;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "epca-client.json";
    private static final String KEY_GIB_LIMIT = "gibLimit";
    private static final String KEY_GIB_MULTIPLIER = "gibMultiplier";

    private static boolean loaded;
    private static int gibLimit = GIB_LIMIT_DEFAULT;
    private static int gibMultiplier = GIB_MULTIPLIER_DEFAULT;

    private EpcaClientSettings() { }

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

    public static int resolvePieceCount(int baseParts) {
        if (baseParts <= 0) return 0;
        int budget = isGibLimitUnlimited() ? baseParts : Math.min(getGibLimit(), baseParts);
        if (budget <= 0) return 0;
        long wanted = (long) budget * getGibMultiplier();
        return (int) Math.min(wanted, SAFETY_MAX_PIECES);
    }

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

        }
    }

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
