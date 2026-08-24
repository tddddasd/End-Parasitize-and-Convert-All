package org.tdddd.epca.impl.overworld.registry.items;


public interface ILivingArmorBoxStorable {
    default String getModuleDescriptionId() {
        return "item.epca.living_armor_box_module";
    }
}