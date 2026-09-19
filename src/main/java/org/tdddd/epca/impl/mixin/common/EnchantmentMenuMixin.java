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

/**
 * 用虫染青金石附魔时额外附加两条诅咒。
 *
 * <p><b>26.1.2 重写</b>（附魔在 26.1.2 是数据包注册表）：
 * <ul>
 *   <li>{@code Enchantment} 引用一律变成 {@code Holder<Enchantment>}；</li>
 *   <li>{@code EnchantmentHelper.getEnchantments(ItemStack)} 返回 {@code Map<Enchantment,Integer>}
 *       已删除，改为 {@code ItemEnchantments}（不可变）；
 *       {@code setEnchantments(Map, ItemStack)} 已删除，改为
 *       {@code updateEnchantments(ItemStack, Consumer<ItemEnchantments.Mutable>)}；</li>
 *   <li>诅咒列表不再来自 {@code BuiltInRegistries.ENCHANTMENTS}（该注册表在 26.1.2 不存在），
 *       而是按标签 {@code minecraft:curse} 取 {@code HolderSet}（与原版
 *       {@code Enchantment#isCurse()} 的定义一致）。</li>
 * </ul>
 * 行为保留：只有副手槽是虫染青金石时才触发；随机选 1~2 条诅咒；已存在的诅咒不重复写等级。
 */
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
        // 26.1.2: 附魔是数据包注册表，Registry#get(TagKey) 返回 Optional<HolderSet.Named>
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
