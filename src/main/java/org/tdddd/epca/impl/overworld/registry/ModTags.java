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

    /**
     * {@code epca:organ_part} GUI
     *
     * <p>SPEC  1  D1 {@code } tag
     *  tag
     * {@code ItemAndEntityTagsData.ItemTagsGen} 1.20.1 </p>
     */
    public static final TagKey<Item> ORGAN_PART =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(epca.MODID, "organ_part"));
}