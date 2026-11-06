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

public final class CursedWorlds {

    private static final ResourceKey<DimensionType> CURSED =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath("epca", "cursed_overworld"));

    private CursedWorlds() {}

    public static boolean isCursed(ServerLevel level) {
        if (level == null) return false;

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

    public static boolean isParasiteBiome(Holder<Biome> biome) {
        if (biome == null) return false;

        return biome.unwrapKey().map(key -> key.equals(ParasiteBiome.PARASITE_BIOME)).orElse(false);
    }
}
