package org.tdddd.epca.impl.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.tdddd.epca.impl.overworld.registry.items.item.KillStick;

/**
 * The 天杀 counter of the Alayavijnana staff: the small piece of logic shared by the kill path, the
 * client sync and the aura sweep.
 *
 * <h2>Storage</h2>
 * <p>The counter lives on the <b>stack</b>, in the stack's NBT under {@link ArayaConstants#TIANSHA_KEY}.
 * 26.1.2 has no per-stack tag any more, so the NBT is reached through the {@code minecraft:custom_data}
 * component - the same route {@code CorruptionPulse}, {@code LivingArmorAdaptation} and
 * {@code WingChestItem} already take for their per-stack numbers, and the exact twin of the 1.20.1
 * {@code stack.getOrCreateTag()}. Storing it on the stack is what makes it survive everything the
 * request has to survive: moving the staff between slots, dropping it, putting it in a chest and taking
 * it out again, and logging out.</p>
 *
 * <h2>Counting</h2>
 * <p>Only a kill of <b>another player</b> counts ("每通过该穷尽灭杖击杀一名非自身的玩家增加1"). The victim is
 * compared by identity and by UUID, so neither a self-kill nor a copy of the killer can raise the
 * counter.</p>
 */
public final class ArayaTiansha {

    private ArayaTiansha() {
    }

    /** The 天杀 value of a stack, or 0 when the stack carries no counter. */
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

    /** Writes the 天杀 value onto a stack. */
    public static void set(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(ArayaConstants.TIANSHA_KEY, Math.max(0, value)));
    }

    /** Adds one kill to a stack and returns the new value. */
    public static int increment(ItemStack stack) {
        int next = get(stack) + 1;
        set(stack, next);
        return next;
    }

    /** True while the stack's counter is at or above the threshold the aura needs. */
    public static boolean hasAura(ItemStack stack) {
        return get(stack) >= ArayaConstants.TIANSHA_THRESHOLD;
    }

    /**
     * True when the victim is somebody else: a comparison by identity and by UUID, so neither a
     * self-kill nor a kill of a copy of the killer can ever count.
     */
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

    /**
     * The named staff that carries the aura, main hand first, or {@link ItemStack#EMPTY}. The counter is
     * read from this stack, so the aura follows the hand that actually holds it.
     */
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
