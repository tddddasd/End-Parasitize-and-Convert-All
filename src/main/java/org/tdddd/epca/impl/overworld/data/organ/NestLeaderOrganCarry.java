package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *  GUI
 * STAGE 2  /  GUI
 *
 * <h2> Container</h2>
 * <p> {@code AbstractContainerMenu} {@code MenuType}
 *  {@code Slot} 52/
 *  UUID
 * //
 *  {@code NestLeaderOrganActionPacket} / {@code NestLeaderOrganActionHandler}</p>
 *
 * <h2></h2>
 * <p> /  {@code NestLeaderOrganEvents}
 * {@code ServerPlayer#drop} {@code Player#drop(ItemStack, boolean)}1.20.1
 * </p>
 */
public final class NestLeaderOrganCarry {

    private static final Map<UUID, ItemStack> CARRIED = new HashMap<>();

    private NestLeaderOrganCarry() {
    }

    /**  null {@link ItemStack#EMPTY} */
    public static ItemStack get(ServerPlayer player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack stack = CARRIED.get(player.getUUID());
        return stack == null ? ItemStack.EMPTY : stack;
    }

    /** {@code null}  */
    public static void set(ServerPlayer player, @Nullable ItemStack stack) {
        if (player == null) return;
        if (stack == null || stack.isEmpty()) {
            CARRIED.remove(player.getUUID());
        } else {
            CARRIED.put(player.getUUID(), stack);
        }
    }

    /**  */
    public static void returnToPlayer(ServerPlayer player) {
        if (player == null) return;
        ItemStack carried = CARRIED.remove(player.getUUID());
        if (carried == null || carried.isEmpty()) return;
        giveBack(player, carried);
    }

    /**  */
    public static void giveBack(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;
        ItemStack remainder = stack.copy();
        // Player#getInventory().add(ItemStack)Inventory  238
        boolean added = player.getInventory().add(remainder);
        if (!added && !remainder.isEmpty()) {
            player.drop(remainder, false);
        }
    }

    /**  /  GUI  */
    public static void onDimensionChanged(ServerPlayer player) {
        returnToPlayer(player);
    }

    /** / NBT  */
    public static String describe(ServerPlayer player) {
        ItemStack stack = get(player);
        if (stack.isEmpty()) return "empty";
        CompoundTag tag = stack.save(new CompoundTag());
        return stack.getCount() + "x " + tag;
    }
}

