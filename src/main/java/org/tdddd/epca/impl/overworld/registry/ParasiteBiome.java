package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.*;
import org.tdddd.epca.impl.epca;

public class ParasiteBiome {
    public static final ResourceKey<Biome> PARASITE_BIOME = ResourceKey.create(
            Registries.BIOME,
            Identifier.fromNamespaceAndPath(epca.MODID, "parasite_biome")
    );
}