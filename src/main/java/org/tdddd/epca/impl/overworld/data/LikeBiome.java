package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/**
 * A "like biome": a biome layered on top of the real one for one 4x4x4 section, never replacing it.
 *
 * <p>The biome is stored by id and resolved again on every read, so a data pack that removes that biome
 * silently cancels the overlay.</p>
 */
public record LikeBiome(Identifier biomeId, boolean overrideColors, boolean overrideRain, int spawnMode) {

    /** The real biome's spawns are replaced by the like biome's. */
    public static final int SPAWN_OVERRIDE = 0;
    /** Both biomes spawn. */
    public static final int SPAWN_COEXIST = 1;
    /** The like biome contributes no spawns. */
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
