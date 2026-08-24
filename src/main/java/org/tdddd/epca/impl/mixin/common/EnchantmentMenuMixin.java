package org.tdddd.epca.impl.mixin.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedLapisLazuli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Shadow
    private net.minecraft.world.Container enchantSlots;  

    
    @Inject(
            method = "clickMenuButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/Container;getItem(I)Lnet/minecraft/world/item/ItemStack;",
                    shift = At.Shift.AFTER
            )
    )
    private void addCurses(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        ItemStack lapis = this.enchantSlots.getItem(1);
        if (!(lapis.getItem() instanceof InfestedLapisLazuli)) return;

        
        ItemStack enchantedItem = this.enchantSlots.getItem(0);
        if (enchantedItem.isEmpty()) return;

        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(enchantedItem);

        List<Enchantment> curses = new ArrayList<>();
        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
            if (ench.isCurse()) {
                curses.add(ench);
            }
        }

        if (curses.isEmpty()) return;

        List<Enchantment> selected = new ArrayList<>();
        if (curses.size() == 1) {
            selected.add(curses.get(0));
        } else {
            Enchantment first = curses.get(player.getRandom().nextInt(curses.size()));
            selected.add(first);
            Enchantment second;
            do {
                second = curses.get(player.getRandom().nextInt(curses.size()));
            } while (second == first);
            selected.add(second);
        }

        for (Enchantment curse : selected) {
            if (!enchantments.containsKey(curse)) {
                enchantments.put(curse, 1);
            }
        }

        EnchantmentHelper.setEnchantments(enchantments, enchantedItem);
    }
}