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

/**
 *  GUI
 *  /  GUI
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
 * {@code ServerPlayer#drop} </p>
 *
 * <p>1.20.1 -&gt; 26.1.2{@code Player#getInventory().add(...)} / {@code Player#drop}
 *  {@code ItemStack#save(CompoundTag)} / {@code ItemStack#of(CompoundTag)}  26.1.2
 * <b></b>_tmp_26src {@code ItemStack.java}  {@code save}  {@code of}
 *  NBT  {@link ItemStack#CODEC} {@link NbtOps#INSTANCE}</p>
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
        // Player#getInventory().add(ItemStack)
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
        // 26.1.2ItemStack#save(CompoundTag)  ItemStack.CODEC + NbtOps
        CompoundTag tag = new CompoundTag();
        Tag encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null);
        if (encoded instanceof CompoundTag compound) {
            tag = compound;
        }
        return stack.getCount() + "x " + tag;
    }
}

