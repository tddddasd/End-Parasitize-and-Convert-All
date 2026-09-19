package org.tdddd.epca.impl.overworld.registry.items;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class LivingArmorBoxModuleItem extends Item implements ILivingArmorBoxStorable {

    public LivingArmorBoxModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    // 26.1.2: appendHoverText(ItemStack, Level, List<Component>, TooltipFlag) is gone; the    // tooltip lines are now pushed into a Consumer and the level became a TooltipContext.
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        
        tooltip.accept(Component.translatable(this.getModuleDescriptionId()));

        
        Component defenseDesc = this.getDefenseDescription();
        if (defenseDesc != null) {
            tooltip.accept(defenseDesc.copy().withStyle(ChatFormatting.DARK_AQUA));
        }

        
        Component attackDesc = this.getAttackDescription();
        if (attackDesc != null) {
            tooltip.accept(attackDesc.copy().withStyle(ChatFormatting.GOLD));
        }

        
        Component energyDesc = this.getEnergyConsumptionDescription();
        if (energyDesc != null) {
            tooltip.accept(energyDesc.copy().withStyle(ChatFormatting.WHITE));
        }

        
        Component specialDesc = this.getSpecialDescription();
        if (specialDesc != null) {
            tooltip.accept(specialDesc.copy().withStyle(ChatFormatting.DARK_PURPLE));
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