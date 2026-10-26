package org.tdddd.epca.impl.overworld.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A "like biome": a biome that is <b>layered on top of</b> the real one for one chunk unit, without ever
 * replacing it.
 *
 * <p>Only the parts the player asked for are taken from the like biome; everything else keeps coming from
 * the real biome. The like biome is always resolved by id at read time, so a data pack that removes the
 * biome silently stops the overlay instead of leaving a dangling colour behind.</p>
 *
 * @param biomeId        the biome used as the overlay, or {@code null} when the chunk has no like biome
 * @param overrideColors take water, fog, grass/foliage and sky colours from the like biome
 * @param overrideRain   take rain and snow behaviour from the like biome
 * @param spawnMode      what happens to mob spawning, see {@link #SPAWN_* }
 */
public record LikeBiome(ResourceLocation biomeId, boolean overrideColors, boolean overrideRain, int spawnMode) {

    /** The real biome's spawns are replaced by the like biome's. */
    public static final int SPAWN_OVERRIDE = 0;
    /** Both biomes spawn; the like biome adds to the real one. */
    public static final int SPAWN_COEXIST = 1;
    /** The like biome contributes no spawns at all. */
    public static final int SPAWN_NONE = 2;

    private static final String KEY_BIOME = "Biome";
    private static final String KEY_COLORS = "Colors";
    private static final String KEY_RAIN = "Rain";
    private static final String KEY_SPAWN = "Spawn";

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_BIOME, this.biomeId.toString());
        tag.putBoolean(KEY_COLORS, this.overrideColors);
        tag.putBoolean(KEY_RAIN, this.overrideRain);
        tag.putInt(KEY_SPAWN, this.spawnMode);
        return tag;
    }

    public static LikeBiome load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY_BIOME));
        if (id == null) {
            return null;
        }
        return new LikeBiome(id, tag.getBoolean(KEY_COLORS), tag.getBoolean(KEY_RAIN),
                tag.getInt(KEY_SPAWN));
    }

    public static String spawnModeName(int mode) {
        return switch (mode) {
            case SPAWN_COEXIST -> "coexist";
            case SPAWN_NONE -> "none";
            default -> "override";
        };
    }
}
