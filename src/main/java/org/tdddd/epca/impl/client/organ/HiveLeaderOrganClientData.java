package org.tdddd.epca.impl.client.organ;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

public final class HiveLeaderOrganClientData {

    private static HiveLeaderOrganData data;
    private static HiveLeaderOrganGate.Gates gates = HiveLeaderOrganGate.Gates.CLOSED;

    private static ItemStack predictedCarried = ItemStack.EMPTY;

    private record PendingSlotAction(int slot, ItemStack expectedSlot, ItemStack carriedBefore) {
    }

    private static final Deque<PendingSlotAction> PENDING_SLOT_ACTIONS = new ArrayDeque<>();

    private static final int MAX_PENDING_SLOT_ACTIONS = 8;

    private HiveLeaderOrganClientData() {
    }

    public static void onSync(CompoundTag organData, boolean torsoInnerOpen, boolean headInnerOpen,
                              boolean responseToAction) {
        data = HiveLeaderOrganData.fromNbt(organData == null ? new CompoundTag() : organData);
        gates = new HiveLeaderOrganGate.Gates(torsoInnerOpen, headInnerOpen);
        if (responseToAction) {
            reconcilePendingSlotAction();
        }
    }

    private static void reconcilePendingSlotAction() {
        PendingSlotAction pending = PENDING_SLOT_ACTIONS.pollFirst();
        if (pending == null) return;
        ItemStack actual = (data == null || pending.slot() < 0 || pending.slot() >= data.size())
                ? ItemStack.EMPTY : data.getItem(pending.slot());
        if (!ItemStack.matches(actual, pending.expectedSlot())) {
            predictedCarried = pending.carriedBefore().copy();
            PENDING_SLOT_ACTIONS.clear();
        }
    }

    public static void onSync(CompoundTag organData, boolean torsoInnerOpen, boolean headInnerOpen) {
        onSync(organData, torsoInnerOpen, headInnerOpen, false);
    }

    public static HiveLeaderOrganData data() {
        HiveLeaderOrganData current = data;
        return current == null ? HiveLeaderOrganData.createEmpty() : current;
    }

    public static boolean hasData() {
        return data != null;
    }

    public static HiveLeaderOrganGate.Gates gates() {
        return gates;
    }

    public static ItemStack carried() {
        return predictedCarried;
    }

    public static boolean hasPendingLocalCarried() {
        return !predictedCarried.isEmpty();
    }

    public static void predictOrganSlotAction(int slot, ItemStack expectedSlot,
                                              ItemStack carriedBefore, ItemStack carriedAfter) {
        PENDING_SLOT_ACTIONS.addLast(new PendingSlotAction(slot,
                expectedSlot == null ? ItemStack.EMPTY : expectedSlot.copy(),
                carriedBefore == null ? ItemStack.EMPTY : carriedBefore.copy()));
        while (PENDING_SLOT_ACTIONS.size() > MAX_PENDING_SLOT_ACTIONS) {
            PENDING_SLOT_ACTIONS.pollFirst();
        }
        predictedCarried = carriedAfter == null ? ItemStack.EMPTY : carriedAfter;
    }

    public static void predictCarried(@Nullable ItemStack carriedAfter) {
        PENDING_SLOT_ACTIONS.clear();
        predictedCarried = carriedAfter == null ? ItemStack.EMPTY : carriedAfter;
    }

    public static void setCarried(ItemStack stack) {
        predictCarried(stack);
    }

    public static void clearCarried() {
        predictCarried(ItemStack.EMPTY);
    }

    public static void clear() {
        data = null;
        gates = HiveLeaderOrganGate.Gates.CLOSED;
        predictedCarried = ItemStack.EMPTY;
        PENDING_SLOT_ACTIONS.clear();
    }

    public static void openScreen() {
        Minecraft.getInstance().setScreen(new HiveLeaderOrganScreen());
    }

    public static boolean isScreenOpen() {
        return Minecraft.getInstance().screen instanceof HiveLeaderOrganScreen;
    }
}
