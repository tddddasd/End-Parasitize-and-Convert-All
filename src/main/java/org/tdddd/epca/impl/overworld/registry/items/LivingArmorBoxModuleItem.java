package org.tdddd.epca.impl.overworld.registry.items;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LivingArmorBoxModuleItem extends Item implements ILivingArmorBoxStorable {

    public LivingArmorBoxModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        tooltip.add(Component.translatable(this.getModuleDescriptionId()));

        
        Component defenseDesc = this.getDefenseDescription();
        if (defenseDesc != null) {
            tooltip.add(defenseDesc.copy().withStyle(ChatFormatting.DARK_AQUA));
        }

        
        Component attackDesc = this.getAttackDescription();
        if (attackDesc != null) {
            tooltip.add(attackDesc.copy().withStyle(ChatFormatting.GOLD));
        }

        
        Component energyDesc = this.getEnergyConsumptionDescription();
        if (energyDesc != null) {
            tooltip.add(energyDesc.copy().withStyle(ChatFormatting.WHITE));
        }

        
        Component specialDesc = this.getSpecialDescription();
        if (specialDesc != null) {
            tooltip.add(specialDesc.copy().withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    
    public abstract String getModuleDescriptionId();

    
    @Nullable
    protected Component getDefenseDescription() {
        return null;
    }

    
    @Nullable
    protected Component getAttackDescription() {
        return null;
    }

    
    @Nullable
    protected Component getEnergyConsumptionDescription() {
        return null;
    }

    
    @Nullable
    protected Component getSpecialDescription() {
        return null;
    }
}