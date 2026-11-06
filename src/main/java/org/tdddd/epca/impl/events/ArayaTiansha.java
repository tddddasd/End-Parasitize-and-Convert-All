package org.tdddd.epca.impl.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.tdddd.epca.impl.overworld.registry.items.item.KillStick;

public final class ArayaTiansha {

    private ArayaTiansha() {
    }

    public static int get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(ArayaConstants.TIANSHA_KEY)) {
            return 0;
        }
        return Math.max(0, tag.getIntOr(ArayaConstants.TIANSHA_KEY, 0));
    }

    public static void set(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(ArayaConstants.TIANSHA_KEY, Math.max(0, value)));
    }

    public static int increment(ItemStack stack) {
        int next = get(stack) + 1;
        set(stack, next);
        return next;
    }

    public static boolean hasAura(ItemStack stack) {
        return get(stack) >= ArayaConstants.TIANSHA_THRESHOLD;
    }

    public static boolean isOtherPlayer(Player killer, LivingEntity victim) {
        if (killer == null || victim == null) {
            return false;
        }
        if (!(victim instanceof Player)) {
            return false;
        }
        if (victim == killer) {
            return false;
        }
        return !victim.getUUID().equals(killer.getUUID());
    }

    public static ItemStack activeStaff(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getMainHandItem();
        if (KillStick.isAlayavijnana(main) && hasAura(main)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (KillStick.isAlayavijnana(off) && hasAura(off)) {
            return off;
        }
        return ItemStack.EMPTY;
    }
}
