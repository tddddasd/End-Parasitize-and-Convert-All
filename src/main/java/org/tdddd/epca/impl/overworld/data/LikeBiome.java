package org.tdddd.epca.impl.overworld.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * A "like biome": a fake biome painted on top of a small 4x4x4 world unit.
 *
 * <p>The record is serialized both to disk (like biome saved data) and over the network
 * (full snapshot + incremental batches), so every field must stay in sync on both sides.</p>
 *
 * <p>{@code erosionStage} controls how much of the rendering is overridden:
 * below {@link #FULL_COLOR_STAGE} only the water colour is changed, from
 * {@link #FULL_COLOR_STAGE} on water, sky and block colours are changed.
 * {@link #STAGE_INHERIT} means "use the dimension's erosion (evolution) stage".</p>
 */
public record LikeBiome(ResourceLocation biomeId, boolean overrideColors, boolean overrideRain, int spawnMode,
                        int erosionStage) {

    public static final int SPAWN_OVERRIDE = 0;

    public static final int SPAWN_COEXIST = 1;

    public static final int SPAWN_NONE = 2;

    /** Sentinel used by {@code erosionStage}: follow the dimension wide erosion stage. */
    public static final int STAGE_INHERIT = Integer.MIN_VALUE;

    /**
     * Single source of truth for the rendering threshold: an erosion stage below this value only
     * recolours water, from this value on water, sky (incl. fog) and block colours are recoloured.
     */
    public static final int FULL_COLOR_STAGE = 6;

    /** Largest erosion stage that still renders water only, derived from {@link #FULL_COLOR_STAGE}. */
    public static final int WATER_ONLY_MAX_STAGE = FULL_COLOR_STAGE - 1;

    private static final String KEY_BIOME = "Biome";
    private static final String KEY_COLORS = "Colors";
    private static final String KEY_RAIN = "Rain";
    private static final String KEY_SPAWN = "Spawn";
    private static final String KEY_STAGE = "ErosionStage";

    public LikeBiome(ResourceLocation biomeId, boolean overrideColors, boolean overrideRain, int spawnMode) {
        this(biomeId, overrideColors, overrideRain, spawnMode, STAGE_INHERIT);
    }

    public boolean hasExplicitStage() {
        return this.erosionStage != STAGE_INHERIT;
    }

    public LikeBiome withStage(int stage) {
        return new LikeBiome(this.biomeId, this.overrideColors, this.overrideRain, this.spawnMode, stage);
    }

    public LikeBiome withColors(boolean colors) {
        return new LikeBiome(this.biomeId, colors, this.overrideRain, this.spawnMode, this.erosionStage);
    }

    /**
     * @param dimensionStage erosion stage of the dimension this unit lives in
     * @return the stage that should drive rendering for this unit
     */
    public int effectiveStage(int dimensionStage) {
        return this.erosionStage == STAGE_INHERIT ? dimensionStage : this.erosionStage;
    }

    /** @return true when the like biome may also recolor sky and blocks, not only water. */
    public boolean paintsFullColors(int dimensionStage) {
        return effectiveStage(dimensionStage) >= FULL_COLOR_STAGE;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_BIOME, this.biomeId.toString());
        tag.putBoolean(KEY_COLORS, this.overrideColors);
        tag.putBoolean(KEY_RAIN, this.overrideRain);
        tag.putInt(KEY_SPAWN, this.spawnMode);
        tag.putInt(KEY_STAGE, this.erosionStage);
        return tag;
    }

    public static LikeBiome load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY_BIOME));
        if (id == null) {
            return null;
        }
        // Save compatibility: entries written before the erosion stage existed inherit the
        // dimension stage instead of being treated as an explicit stage 0.
        int stage = tag.contains(KEY_STAGE, Tag.TAG_INT) ? tag.getInt(KEY_STAGE) : STAGE_INHERIT;
        return new LikeBiome(id, tag.getBoolean(KEY_COLORS), tag.getBoolean(KEY_RAIN),
                tag.getInt(KEY_SPAWN), stage);
    }

    public static String spawnModeName(int mode) {
        return switch (mode) {
            case SPAWN_COEXIST -> "coexist";
            case SPAWN_NONE -> "none";
            default -> "override";
        };
    }
}
