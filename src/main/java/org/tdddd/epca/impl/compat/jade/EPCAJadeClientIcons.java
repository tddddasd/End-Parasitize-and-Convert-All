package org.tdddd.epca.impl.compat.jade;

import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.ModItems;


public final class EPCAJadeClientIcons {

    private static ItemStack biomassCountIcon;

    private EPCAJadeClientIcons() {
    }

    
    public static ItemStack biomassCountIcon() {
        if (biomassCountIcon == null) {
            biomassCountIcon = new ItemStack(ModItems.BIOMASS_COUNT_ICON.get());
        }
        return biomassCountIcon;
    }
}
