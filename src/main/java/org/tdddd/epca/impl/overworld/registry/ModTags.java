package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.tdddd.epca.impl.epca;

public class ModTags {
    public static final TagKey<EntityType<?>> INFESTED_UNDEAD =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(epca.MODID, "infested_undead"));

    public static final TagKey<EntityType<?>> PARASITE =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(epca.MODID, "parasite"));
    public static final TagKey<EntityType<?>> INFESTED =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(epca.MODID, "infested"));
    public static final TagKey<EntityType<?>> ONESENT =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(epca.MODID, "onesent"));
    public static final TagKey<EntityType<?>> POVERTY =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(epca.MODID, "poverty"));
    public static final TagKey<EntityType<?>> RESHAPE =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(epca.MODID, "reshape"));
    public static final TagKey<EntityType<?>> LINK =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(epca.MODID, "link"));

    public static final TagKey<Item> ORGAN_PART =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(epca.MODID, "organ_part"));
}
