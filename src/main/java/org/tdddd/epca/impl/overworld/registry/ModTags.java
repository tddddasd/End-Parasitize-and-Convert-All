package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.tdddd.epca.impl.epca;

/**
 *  /
 *
 * <p> JSON
 * {@code src/main/resources/data/epca/tags/...}
 * {@code src/generated/resources/...} {@code ItemAndEntityTagsData} </p>
 */
public class ModTags {


    public static final TagKey<EntityType<?>> INFESTED_UNDEAD =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(epca.MODID, "infested_undead"));


    /**
     * {@code epca:organ_part} GUI
     *
     * <p>SPEC  1  D1 {@code } tag
     *  tag
     * {@code src/generated/resources/data/epca/tags/items/organ_part.json}</p>
     */
    public static final TagKey<Item> ORGAN_PART =
            TagKey.create(Registries.ITEM, new ResourceLocation(epca.MODID, "organ_part"));

    private ModTags() {
    }
}

