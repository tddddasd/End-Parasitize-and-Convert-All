package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HiveLeaderOrganCarry {

    private static final Map<UUID, ItemStack> CARRIED = new HashMap<>();

    private HiveLeaderOrganCarry() {
    }

    public static ItemStack get(ServerPlayer player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack stack = CARRIED.get(player.getUUID());
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static void set(ServerPlayer player, @Nullable ItemStack stack) {
        if (player == null) return;
        if (stack == null || stack.isEmpty()) {
            CARRIED.remove(player.getUUID());
        } else {
            CARRIED.put(player.getUUID(), stack);
        }
    }

    public static void returnToPlayer(ServerPlayer player) {
        if (player == null) return;
        ItemStack carried = CARRIED.remove(player.getUUID());
        if (carried == null || carried.isEmpty()) return;
        giveBack(player, carried);
    }

    public static void giveBack(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;
        ItemStack remainder = stack.copy();

        boolean added = player.getInventory().add(remainder);
        if (!added && !remainder.isEmpty()) {
            player.drop(remainder, false);
        }
    }

    public static void onDimensionChanged(ServerPlayer player) {
        returnToPlayer(player);
    }

    public static String describe(ServerPlayer player) {
        ItemStack stack = get(player);
        if (stack.isEmpty()) return "empty";

        CompoundTag tag = new CompoundTag();
        Tag encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null);
        if (encoded instanceof CompoundTag compound) {
            tag = compound;
        }
        return stack.getCount() + "x " + tag;
    }
}
