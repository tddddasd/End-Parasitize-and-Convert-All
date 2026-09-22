package org.tdddd.epca.impl.overworld.cursed;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import org.tdddd.epca.impl.overworld.registry.ParasiteBiome;

/**
 * Shared "is this dimension the cursed world?" test.
 *
 * <p>Since the cursed world must generate real overworld terrain AND the real per-biome features, its
 * overworld dimension no longer uses a fixed parasite biome source. It therefore gets its own dimension
 * type, {@code epca:cursed_overworld}, which is an exact copy of the vanilla {@code minecraft:overworld}
 * dimension type. That registration key is the primary marker of the world type.
 *
 * <p>The previous fixed-parasite-biome-source test is kept as a secondary condition so worlds created by an
 * older build of this feature (whose preset still declared
 * {@code "biome_source": {"type": "minecraft:fixed", "biome": "epca:parasite_biome"}}) keep working.
 *
 * <p>A level whose dimension type is not {@code epca:cursed_overworld} and whose generator reports anything
 * else (a normal {@code multi_noise} overworld, the nether, the end, a flat world, another mod's preset) is
 * never cursed, so no existing save or other world type can be affected by the chunk conversion.
 */
public final class CursedWorlds {

    /** The dimension type declared by the cursed world preset's overworld entry. */
    private static final ResourceKey<DimensionType> CURSED =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath("epca", "cursed_overworld"));

    private CursedWorlds() {}

    /**
     * @return {@code true} when {@code level} is the cursed world: a level whose dimension type key is
     *         {@code epca:cursed_overworld}, or - for worlds made by an older build - a fixed-biome world
     *         whose single biome is {@code epca:parasite_biome}
     */
    public static boolean isCursed(ServerLevel level) {
        if (level == null) return false;
        // unwrapKey() is empty for a direct (unregistered) holder, which is never our dimension type.
        if (level.dimensionTypeRegistration().unwrapKey().map(key -> key.equals(CURSED)).orElse(false)) {
            return true;
        }
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof FixedBiomeSource)) return false;
        for (Holder<Biome> biome : source.possibleBiomes()) {
            if (isParasiteBiome(biome)) return true;
        }
        return false;
    }

    /** Whether this biome holder is {@code epca:parasite_biome}. */
    public static boolean isParasiteBiome(Holder<Biome> biome) {
        if (biome == null) return false;
        // unwrapKey() is empty for an unregistered/direct holder, which is never our biome.
        return biome.unwrapKey().map(key -> key.equals(ParasiteBiome.PARASITE_BIOME)).orElse(false);
    }
}
