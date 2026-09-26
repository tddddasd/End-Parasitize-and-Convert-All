package org.tdddd.epca.impl.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.items.item.KillStick;

/**
 * The 天杀 counter of the Alayavijnana staff: the small piece of logic shared by the kill path, the
 * client sync and the aura sweep.
 *
 * <h2>Storage</h2>
 * <p>The counter lives on the <b>stack</b>, in the stack's own NBT under
 * {@link ArayaConstants#TIANSHA_KEY} (26.1.2: the same key inside the
 * {@code minecraft:custom_data} component, which is that version's equivalent of a stack tag - the same
 * mechanism {@code CorruptionPulse} and {@code LivingArmorAdaptation} already use for their per-stack
 * numbers). Storing it on the stack is what makes it survive everything the request has to survive:
 * moving the staff between slots, dropping it, putting it in a chest and taking it out again, and
 * logging out. Nothing is cached per player, so there is no state to lose or to desynchronise.</p>
 *
 * <h2>Counting</h2>
 * <p>Only a kill of <b>another player</b> counts ("每通过该穷尽灭杖击杀一名非自身的玩家增加1"). The victim
 * is compared by identity and by UUID, so neither a self-kill nor a kill of a copy of the killer can
 * ever raise the counter.</p>
 */
public final class ArayaTiansha {

    private ArayaTiansha() {
    }

    /** The 天杀 value of a stack, or 0 when the stack carries no counter. */
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

    /** Writes the 天杀 value onto a stack, removing the key when it reaches zero. */
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
     * True when the victim is somebody else: same dimension-independent comparison by UUID, falling back
     * to identity for entities that have no UUID yet (a player always has one, so this is belt and
     * braces).
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

    /** True when the player carries a named staff in either hand, i.e. the aura can be active at all. */
    public static boolean carriesNamedStaff(Player player) {
        if (player == null) {
            return false;
        }
        return KillStick.isAlayavijnana(player.getMainHandItem())
                || KillStick.isAlayavijnana(player.getOffhandItem());
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
