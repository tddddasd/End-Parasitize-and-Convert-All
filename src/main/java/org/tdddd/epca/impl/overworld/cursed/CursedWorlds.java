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

public final class CursedWorlds {

    private static final ResourceKey<DimensionType> CURSED_OVERWORLD = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation("epca", "cursed_overworld")
    );

    private CursedWorlds() {}

    public static boolean isCursed(ServerLevel level) {
        if (level == null) return false;

        if (level.dimensionTypeRegistration().unwrapKey().map(CURSED_OVERWORLD::equals).orElse(false)) {
            return true;
        }

        return parasiteBiomeOrNull(level) != null;
    }

    public static Holder<Biome> parasiteBiomeOrNull(ServerLevel level) {
        if (level == null) return null;
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof FixedBiomeSource)) return null;

        for (Holder<Biome> biome : source.possibleBiomes()) {
            if (isParasiteBiome(biome)) return biome;
        }
        return null;
    }

    public static Holder<Biome> parasiteBiome(ServerLevel level) {
        if (level == null) return null;
        Holder<Biome> fromGenerator = parasiteBiomeOrNull(level);
        if (fromGenerator != null) return fromGenerator;

        return level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolder(ParasiteBiome.PARASITE_BIOME)
                .orElse(null);
    }

    public static boolean isParasiteBiome(Holder<Biome> biome) {
        if (biome == null) return false;

        return biome.unwrapKey().map(key -> key.equals(ParasiteBiome.PARASITE_BIOME)).orElse(false);
    }
}
