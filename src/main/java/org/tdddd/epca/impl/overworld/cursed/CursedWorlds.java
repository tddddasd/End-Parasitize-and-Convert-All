package org.tdddd.epca.impl.overworld.cursed;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import org.tdddd.epca.impl.overworld.registry.ParasiteBiome;

/**
 * Shared "is this dimension the cursed world?" test.
 *
 * <p>The {@code epca:cursed_world} world preset declares the overworld dimension with the dimension type
 * {@code epca:cursed_overworld}, which is a copy of the vanilla {@code minecraft:overworld} dimension type
 * values. The level therefore behaves exactly like a normal overworld, while its dimension type key is a
 * stable and unambiguous marker for this feature.
 *
 * <p>The biome source of that preset is the normal {@code minecraft:multi_noise} overworld preset on
 * purpose: a chunk must first be generated exactly like a normal overworld (noise terrain, carvers, and the
 * real biome's features and structures) and only then be converted by {@link CursedChunkConverter}. Because
 * the generator no longer holds a fixed parasite biome, the dimension type key is what identifies the world.
 *
 * <p>Worlds created by the earlier build of this feature used a fixed parasite biome source and the plain
 * {@code minecraft:overworld} dimension type. Those levels have no {@code epca:cursed_overworld} dimension
 * type, so the old fixed-biome-source test is kept as a secondary condition and such existing saves keep
 * working.
 *
 * <p>A level whose dimension type is anything else (a normal overworld, the nether, the end, a flat world,
 * another mod's preset) is never cursed, so no existing save or other world type can be affected by the
 * chunk conversion.
 */
public final class CursedWorlds {

    /**
     * The dimension type of the cursed overworld, declared by
     * {@code data/epca/dimension_type/cursed_overworld.json}.
     */
    private static final ResourceKey<DimensionType> CURSED_OVERWORLD = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation("epca", "cursed_overworld")
    );

    private CursedWorlds() {}

    /**
     * @return {@code true} when {@code level} is the cursed world: its dimension type is
     *         {@code epca:cursed_overworld}, or - for a world created by the earlier build of this feature -
     *         its generator is a fixed biome source whose single biome is {@code epca:parasite_biome}
     */
    public static boolean isCursed(ServerLevel level) {
        if (level == null) return false;
        // unwrapKey() is empty for a dimension type that is not registered under a resource key.
        if (level.dimensionTypeRegistration().unwrapKey().map(CURSED_OVERWORLD::equals).orElse(false)) {
            return true;
        }
        // Secondary check: the previous build of this feature declared no custom dimension type and used a
        // fixed parasite biome source instead, so keep recognising those worlds.
        return parasiteBiomeOrNull(level) != null;
    }

    /**
     * The parasite biome {@link Holder} of {@code level} when the level still uses the old fixed parasite
     * biome source, otherwise {@code null}. The holder then comes from the generator's own biome source, so
     * it is the exact holder the world uses.
     */
    public static Holder<Biome> parasiteBiomeOrNull(ServerLevel level) {
        if (level == null) return null;
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof FixedBiomeSource)) return null;
        // A FixedBiomeSource always holds exactly one biome, so this loop runs at most once.
        for (Holder<Biome> biome : source.possibleBiomes()) {
            if (isParasiteBiome(biome)) return biome;
        }
        return null;
    }

    /**
     * The parasite biome {@link Holder} a converted chunk must be filled with.
     *
     * <p>A world from the earlier build returns the holder its fixed biome source already uses. Otherwise -
     * the normal case now - the biome is read from the level's biome registry, because the generator
     * produces real overworld biomes and has no parasite biome to hand over.
     *
     * @return the parasite biome holder, or {@code null} when the level's datapack does not define it
     */
    public static Holder<Biome> parasiteBiome(ServerLevel level) {
        if (level == null) return null;
        Holder<Biome> fromGenerator = parasiteBiomeOrNull(level);
        if (fromGenerator != null) return fromGenerator;
        // getHolder returns an empty Optional instead of throwing when the biome is missing, so a level
        // whose datapack does not define epca:parasite_biome stays harmless.
        return level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolder(ParasiteBiome.PARASITE_BIOME)
                .orElse(null);
    }

    /** Whether this biome holder is {@code epca:parasite_biome}. */
    public static boolean isParasiteBiome(Holder<Biome> biome) {
        if (biome == null) return false;
        // unwrapKey() is empty for an unregistered/direct holder, which is never our biome.
        return biome.unwrapKey().map(key -> key.equals(ParasiteBiome.PARASITE_BIOME)).orElse(false);
    }
}
