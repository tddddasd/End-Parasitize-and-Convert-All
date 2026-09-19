package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.tdddd.epca.impl.epca;

public class ModTags {
    public static final TagKey<EntityType<?>> INFESTED_UNDEAD =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(epca.MODID, "infested_undead"));
}