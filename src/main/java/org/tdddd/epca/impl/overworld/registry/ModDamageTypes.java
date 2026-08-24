package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import org.tdddd.epca.impl.epca;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> MINIMUM =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(epca.MODID, "minimum"));
}