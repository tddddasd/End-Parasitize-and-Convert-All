package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record LikeBiome(Identifier biomeId, boolean overrideColors, boolean overrideRain, int spawnMode) {

    public static final int SPAWN_OVERRIDE = 0;

    public static final int SPAWN_COEXIST = 1;

    public static final int SPAWN_NONE = 2;

    public static final Codec<LikeBiome> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("biome").forGetter(LikeBiome::biomeId),
            Codec.BOOL.optionalFieldOf("colors", true).forGetter(LikeBiome::overrideColors),
            Codec.BOOL.optionalFieldOf("rain", false).forGetter(LikeBiome::overrideRain),
            Codec.INT.optionalFieldOf("spawn", SPAWN_OVERRIDE).forGetter(LikeBiome::spawnMode)
    ).apply(instance, LikeBiome::new));

    public static String spawnModeName(int mode) {
        return switch (mode) {
            case SPAWN_COEXIST -> "coexist";
            case SPAWN_NONE -> "none";
            default -> "override";
        };
    }
}
