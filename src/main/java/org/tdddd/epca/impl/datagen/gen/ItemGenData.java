package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;

import java.util.Set;


public class ItemGenData extends ItemModelProvider {

    public ItemGenData(PackOutput output, ExistingFileHelper efh) {
        super(output, epca.MODID, efh);
    }

    
    private static final Set<String> MANUAL_WHITELIST = Set.of(
            
            "wooden_spear", "stone_spear", "flint_spear", "copper_spear",
            "iron_spear", "golden_spear", "diamond_spear", "netherite_spear",

            
            "erosion_clock",
            "biomass_count_icon",
            "epca_icon",
            "swallow_cyst",

            
            "feeding_module_i", "flesh_armor_module_i",
            "netherite_module_i", "flight_module_i",

            "infested_carved_pumpkin"
    );

    
    private static final Set<String> EXTRA_HANDHELD = Set.of(
            "endless_wand"
    );

    @Override
    protected void registerModels() {
        ForgeRegistries.ITEMS.getValues().forEach(item -> {
            ResourceLocation loc = ForgeRegistries.ITEMS.getKey(item);
            if (loc != null && loc.getNamespace().equals(epca.MODID)) {
                String path = loc.getPath();
                
                if (item instanceof BlockItem || MANUAL_WHITELIST.contains(path)) return;
                try {
                    
                    if (isHandheld(item) || EXTRA_HANDHELD.contains(path)) {
                        handheldItem(path);
                    } else {
                        this.basicItem(item);
                    }
                } catch (Exception e) {
                    MANUAL_WHITELIST_DYNAMIC.add(path);
                    epca.LOGGER.warn("Skipping item model for {} (no texture): {}", path, e.getMessage());
                }
            }
        });
    }

    
    private static final Set<String> MANUAL_WHITELIST_DYNAMIC = new java.util.HashSet<>();

    private boolean isHandheld(Item item) {
        return item instanceof SwordItem || item instanceof PickaxeItem
                || item instanceof AxeItem || item instanceof ShovelItem || item instanceof HoeItem;
    }

    protected void handheldItem(String path) {
        withExistingParent(path, mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/" + path));
    }
}
