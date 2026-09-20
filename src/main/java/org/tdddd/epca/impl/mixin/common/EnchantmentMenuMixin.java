package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedLapisLazuli;

import java.util.ArrayList;
import java.util.List;


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

        List<Holder<Enchantment>> curses = new ArrayList<>();
        
        player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(net.minecraft.tags.EnchantmentTags.CURSE)
                .ifPresent(set -> set.forEach(curses::add));

        if (curses.isEmpty()) return;

        List<Holder<Enchantment>> selected = new ArrayList<>();
        if (curses.size() == 1) {
            selected.add(curses.get(0));
        } else {
            Holder<Enchantment> first = curses.get(player.getRandom().nextInt(curses.size()));
            selected.add(first);
            Holder<Enchantment> second;
            do {
                second = curses.get(player.getRandom().nextInt(curses.size()));
            } while (second == first);
            selected.add(second);
        }

        EnchantmentHelper.updateEnchantments(enchantedItem, mutable -> {
            for (Holder<Enchantment> curse : selected) {
                if (mutable.getLevel(curse) == 0) {
                    mutable.set(curse, 1);
                }
            }
        });
    }
}
