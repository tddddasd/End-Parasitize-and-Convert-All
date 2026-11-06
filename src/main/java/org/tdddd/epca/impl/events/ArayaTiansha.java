package org.tdddd.epca.impl.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.items.item.KillStick;

public final class ArayaTiansha {

    private ArayaTiansha() {
    }

    public static int get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ArayaConstants.TIANSHA_KEY)) {
            return 0;
        }
        return Math.max(0, tag.getInt(ArayaConstants.TIANSHA_KEY));
    }

    public static void set(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (value <= 0) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(ArayaConstants.TIANSHA_KEY);
            }
            return;
        }
        stack.getOrCreateTag().putInt(ArayaConstants.TIANSHA_KEY, value);
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

    public static boolean carriesNamedStaff(Player player) {
        if (player == null) {
            return false;
        }
        return KillStick.isAlayavijnana(player.getMainHandItem())
                || KillStick.isAlayavijnana(player.getOffhandItem());
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
