package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.network.chat.Component;
import org.tdddd.epca.impl.overworld.registry.items.LivingArmorBoxModuleItem;

public class AutomaticFeedingModuleI extends LivingArmorBoxModuleItem {
    public AutomaticFeedingModuleI(Properties properties) {
        super(properties);
    }

    @Override
    public String getModuleDescriptionId() {
        return "tooltip.epca.module_description";
    }

    @Override
    protected Component getDefenseDescription() {
        return Component.translatable("tooltip.epca.defense", "█");
    }

    @Override
    protected Component getAttackDescription() {
        return Component.translatable("tooltip.epca.attack", " ");
    }

    @Override
    protected Component getEnergyConsumptionDescription() {
        return Component.translatable("tooltip.epca.biomass", "██▓");
    }

    @Override
    protected Component getSpecialDescription() {
        return Component.translatable("tooltip.epca.negative", " ");
    }
}