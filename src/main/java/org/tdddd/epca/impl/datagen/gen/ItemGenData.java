package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Block;
import org.tdddd.epca.impl.epca;

import java.util.Set;
import java.util.stream.Stream;


public class ItemGenData extends ModelProvider {

    public ItemGenData(PackOutput output) {
        super(output, epca.MODID);
    }

    
    private static final Set<String> MANUAL_WHITELIST = Set.of(
            
            "wooden_spear", "stone_spear", "flint_spear", "copper_spear",
            "iron_spear", "golden_spear", "diamond_spear", "netherite_spear",

            
            "erosion_clock",
            "swallow_cyst",

            
            "feeding_module_i", "flesh_armor_module_i",
            "netherite_module_i", "flight_module_i"
    );

    
    private static final Set<String> EXTRA_HANDHELD = Set.of(
            "endless_wand"
    );

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        
        return Stream.empty();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        
        return BuiltInRegistries.ITEM.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> !(holder.value() instanceof BlockItem))
                .filter(holder -> !MANUAL_WHITELIST.contains(holder.getKey().identifier().getPath()));
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        BuiltInRegistries.ITEM.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> !(holder.value() instanceof BlockItem))
                .filter(holder -> !MANUAL_WHITELIST.contains(holder.getKey().identifier().getPath()))
                .forEach(holder -> {
                    Item item = holder.value();
                    String path = holder.getKey().identifier().getPath();
                    try {
                        if (isHandheld(item) || EXTRA_HANDHELD.contains(path)) {
                            itemModels.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
                        } else {
                            itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
                        }
                    } catch (Exception e) {
                        epca.LOGGER.warn("Skipping item model for {} (no texture): {}", path, e.getMessage());
                    }
                });
    }

    private boolean isHandheld(Item item) {
        if (item instanceof AxeItem || item instanceof ShovelItem || item instanceof HoeItem) {
            return true;
        }
        
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return false;
        String path = key.getPath();
        return path.endsWith("_sword") || path.endsWith("_pickaxe");
    }

    @Override
    public String getName() {
        return "EPCA Item Models";
    }
}
