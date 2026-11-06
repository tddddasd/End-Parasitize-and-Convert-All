package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.tdddd.epca.impl.epca;

public class ModTags {
    public static final TagKey<EntityType<?>> PARASITE =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(epca.MODID, "parasite"));
    public static final TagKey<EntityType<?>> ONESENT =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(epca.MODID, "onesent"));
    public static final TagKey<EntityType<?>> POVERTY =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(epca.MODID, "poverty"));
    public static final TagKey<EntityType<?>> INFESTED =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(epca.MODID, "infested"));
    public static final TagKey<EntityType<?>> RESHAPE =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(epca.MODID, "reshape"));
    public static final TagKey<EntityType<?>> LINK =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(epca.MODID, "link"));

    public static final TagKey<Item> ORGAN_PART =
            TagKey.create(Registries.ITEM, new ResourceLocation(epca.MODID, "organ_part"));

    private ModTags() {
    }
}
