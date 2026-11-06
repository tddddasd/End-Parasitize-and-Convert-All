package org.tdddd.epca.impl.overworld.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record LikeBiome(ResourceLocation biomeId, boolean overrideColors, boolean overrideRain, int spawnMode) {

    public static final int SPAWN_OVERRIDE = 0;

    public static final int SPAWN_COEXIST = 1;

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
