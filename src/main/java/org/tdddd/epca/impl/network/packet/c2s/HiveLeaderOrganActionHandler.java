package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganCarry;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotUnlock;

public final class HiveLeaderOrganActionHandler {

    private HiveLeaderOrganActionHandler() {
    }

    public static void handle(ServerPlayer player, HiveLeaderOrganData data,
                              HiveLeaderOrganActionPacket msg) {
        if (player == null || data == null || msg == null) return;

        if (msg.action() == HiveLeaderOrganActionPacket.Action.RETURN_CARRIED) {

            HiveLeaderOrganCarry.returnToPlayer(player);
            return;
        }

        if (msg.action() == HiveLeaderOrganActionPacket.Action.INVENTORY_SLOT) {

            if (msg.button() == HiveLeaderOrganActionPacket.BUTTON_MIDDLE) {
                quickMoveFromInventory(player, data, msg.slot());
            } else {
                clickInventory(player, data, msg.slot(), msg.button());
            }
            return;
        }

        int slot = msg.slot();
        if (slot < 0 || slot >= data.size()) {
            return;
        }
        OrganSlotGroup group = OrganSlotGroup.byGlobalIndex(slot);
        if (group == null) {
            return;
        }

        switch (msg.action()) {
            case CLICK -> click(player, data, slot, group, msg.button());
            case QUICK_MOVE -> quickMove(player, data, slot, group);
            case CLEAR -> clear(player, data, slot, group);
            case INVENTORY_SLOT -> {

            }
            default -> {

            }
        }
    }

    private static void clickInventory(ServerPlayer player, HiveLeaderOrganData data, int slot, int button) {
        Inventory inventory = player.getInventory();

        if (slot < 0 || slot >= HiveLeaderOrganActionPacket.INVENTORY_SLOT_COUNT
                || slot >= inventory.getNonEquipmentItems().size()) {
            return;
        }

        ItemStack carried = HiveLeaderOrganCarry.get(player);
        ItemStack inSlot = inventory.getItem(slot);
        boolean rightClick = button == HiveLeaderOrganActionPacket.BUTTON_RIGHT;

        if (carried.isEmpty()) {
            if (inSlot.isEmpty()) return;
            int amount = rightClick ? (inSlot.getCount() + 1) / 2 : inSlot.getCount();
            ItemStack taken = inSlot.copyWithCount(amount);
            if (amount >= inSlot.getCount()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            } else {
                inSlot.shrink(amount);
            }
            inventory.setChanged();
            HiveLeaderOrganCarry.set(player, taken);
            return;
        }

        if (inSlot.isEmpty()) {
            int amount = rightClick ? 1 : carried.getCount();
            inventory.setItem(slot, carried.copyWithCount(amount));
            inventory.setChanged();
            shrinkCarried(player, carried, amount);
            return;
        }

        if (ItemStack.isSameItemSameComponents(inSlot, carried)) {
            int space = inSlot.getMaxStackSize() - inSlot.getCount();
            if (space <= 0) return;
            int amount = rightClick ? Math.min(1, space) : Math.min(space, carried.getCount());
            if (amount <= 0) return;
            inSlot.grow(amount);
            inventory.setChanged();
            shrinkCarried(player, carried, amount);
            return;
        }

        HiveLeaderOrganCarry.set(player, inSlot.copy());
        inventory.setItem(slot, carried.copy());
        inventory.setChanged();
    }

    private static void quickMoveInventoryToOrgan(ServerPlayer player, HiveLeaderOrganData data, int fromSlot) {
        Inventory inventory = player.getInventory();
        if (fromSlot < 0 || fromSlot >= HiveLeaderOrganActionPacket.INVENTORY_SLOT_COUNT
                || fromSlot >= inventory.getNonEquipmentItems().size()) {
            return;
        }
        ItemStack inSlot = inventory.getItem(fromSlot);
        if (inSlot.isEmpty()) return;

        boolean moved = false;
        for (OrganSlotGroup group : OrganSlotGroup.ALL) {
            if (inSlot.isEmpty()) break;
            for (int local = 0; local < group.size() && !inSlot.isEmpty(); local++) {
                int index = group.globalIndex(local);
                if (!canInsertIntoOrgan(player, data, index, group)) continue;

                ItemStack target = data.getItem(index);
                if (target.isEmpty()) {

                    int amount = Math.min(inSlot.getCount(), inSlot.getMaxStackSize());
                    data.setItem(index, inSlot.copyWithCount(amount));
                    inSlot.shrink(amount);
                    moved = true;
                    continue;
                }
                if (!ItemStack.isSameItemSameComponents(target, inSlot)) continue;
                int space = target.getMaxStackSize() - target.getCount();
                if (space <= 0) continue;
                int amount = Math.min(space, inSlot.getCount());
                target.grow(amount);
                inSlot.shrink(amount);
                moved = true;
            }
        }

        if (moved) {
            if (inSlot.isEmpty()) {
                inventory.setItem(fromSlot, ItemStack.EMPTY);
            }
            inventory.setChanged();
        }
    }

    private static void quickMoveFromInventory(ServerPlayer player, HiveLeaderOrganData data, int slot) {
        if (!HiveLeaderOrganCarry.get(player).isEmpty()) {
            HiveLeaderOrganCarry.returnToPlayer(player);
            return;
        }
        quickMoveInventoryToOrgan(player, data, slot);
    }

    private static boolean canInsertIntoOrgan(ServerPlayer player, HiveLeaderOrganData data,
                                              int index, OrganSlotGroup group) {
        if (group.kind().isInnerGrid()) {
            return HiveLeaderOrganGate.canModifySlot(player, data, index);
        }
        return data.isUnlocked(index);
    }

    private static void click(ServerPlayer player, HiveLeaderOrganData data, int slot,
                              OrganSlotGroup group, int button) {
        ItemStack carried = HiveLeaderOrganCarry.get(player);
        ItemStack inSlot = data.getItem(slot);

        if (carried.isEmpty()) {

            boolean lockedOuterSlot = group.kind().needsUnlock() && !data.isUnlocked(slot);
            if (inSlot.isEmpty() && lockedOuterSlot) {

                tryUnlockSlot(player, data, slot, group);
                return;
            }

            if (inSlot.isEmpty()) return;
            if (!canModify(player, data, slot, group, false)) return;

            int amount = button == HiveLeaderOrganActionPacket.BUTTON_RIGHT
                    ? Math.max(1, inSlot.getCount() / 2)
                    : inSlot.getCount();
            ItemStack taken = inSlot.copyWithCount(amount);
            if (amount >= inSlot.getCount()) {
                data.clearItem(slot);
            } else {
                inSlot.shrink(amount);
            }
            HiveLeaderOrganCarry.set(player, taken);
            return;
        }

        if (!canModify(player, data, slot, group, true)) return;
        if (!HiveLeaderOrganData.acceptsItem(carried)) {
            player.sendSystemMessage(Component.literal("这个槽位只能放入「器官部位」标签的物品"), true);
            return;
        }

        if (inSlot.isEmpty()) {
            int amount = button == HiveLeaderOrganActionPacket.BUTTON_RIGHT ? 1 : carried.getCount();
            data.setItem(slot, carried.copyWithCount(amount));
            shrinkCarried(player, carried, amount);
            return;
        }

        boolean mergeable = ItemStack.isSameItemSameComponents(inSlot, carried)
                && inSlot.getCount() < inSlot.getMaxStackSize();
        if (mergeable) {
            int space = inSlot.getMaxStackSize() - inSlot.getCount();
            int amount = button == HiveLeaderOrganActionPacket.BUTTON_RIGHT
                    ? Math.min(1, space) : Math.min(space, carried.getCount());
            if (amount <= 0) return;
            inSlot.grow(amount);
            shrinkCarried(player, carried, amount);
            return;
        }

        if (!HiveLeaderOrganData.acceptsItem(inSlot)) {
            return;
        }
        ItemStack old = inSlot.copy();
        data.setItem(slot, carried.copy());
        HiveLeaderOrganCarry.set(player, old);
    }

    private static void quickMove(ServerPlayer player, HiveLeaderOrganData data, int slot,
                                  OrganSlotGroup group) {
        ItemStack carried = HiveLeaderOrganCarry.get(player);

        if (carried.isEmpty()) {
            ItemStack inSlot = data.getItem(slot);
            if (inSlot.isEmpty()) return;
            if (!canModify(player, data, slot, group, false)) return;
            ItemStack taken = inSlot.copy();
            data.clearItem(slot);
            HiveLeaderOrganCarry.giveBack(player, taken);
            if (player.getInventory() != null) {
                player.getInventory().setChanged();
            }
            return;
        }

        if (!canModify(player, data, slot, group, true)) return;
        if (!HiveLeaderOrganData.acceptsItem(carried)) {
            player.sendSystemMessage(Component.literal("这个槽位只能放入「器官部位」标签的物品"), true);
            return;
        }

        ItemStack inSlot = data.getItem(slot);
        if (inSlot.isEmpty()) {
            data.setItem(slot, carried.copy());
            HiveLeaderOrganCarry.set(player, ItemStack.EMPTY);
            return;
        }
        if (!ItemStack.isSameItemSameComponents(inSlot, carried)) {

            return;
        }
        int space = inSlot.getMaxStackSize() - inSlot.getCount();
        if (space <= 0) return;
        int amount = Math.min(space, carried.getCount());
        inSlot.grow(amount);
        shrinkCarried(player, carried, amount);
    }

    private static void clear(ServerPlayer player, HiveLeaderOrganData data, int slot,
                              OrganSlotGroup group) {
        ItemStack inSlot = data.getItem(slot);
        if (inSlot.isEmpty()) return;
        if (!canModify(player, data, slot, group, false)) return;
        ItemStack taken = inSlot.copy();
        data.clearItem(slot);
        HiveLeaderOrganCarry.giveBack(player, taken);
    }

    private static boolean canModify(ServerPlayer player, HiveLeaderOrganData data, int slot,
                                     OrganSlotGroup group, boolean wantsInsert) {
        if (group.kind().isInnerGrid()) {
            if (HiveLeaderOrganGate.canModifySlot(player, data, slot)) {
                return true;
            }
            player.sendSystemMessage(Component.literal(group == OrganSlotGroup.TORSO_INNER
                    ? "7×7×7 格内需要任意召唤柱才能修改躯干内部槽"
                    : "7×7×7 格内需要二阶召唤柱才能修改头部内部槽"), true);
            return false;
        }

        if (data.isUnlocked(slot)) {
            return true;
        }
        if (!wantsInsert) {

            return false;
        }

        return tryUnlockSlot(player, data, slot, group);
    }

    private static boolean tryUnlockSlot(ServerPlayer player, HiveLeaderOrganData data, int slot,
                                         OrganSlotGroup group) {
        OrganSlotUnlock.Result result = OrganSlotUnlock.tryUnlock(player, data, slot);
        if (!result.success()) {
            OrganSlotUnlock.sendFailure(player, result);
            return false;
        }
        player.sendSystemMessage(Component.literal(
                group.displayName() + "槽已解锁（-" + result.cost()
                        + " 生物质，剩余 " + result.remainingPoints() + "）"), true);
        return true;
    }

    private static void shrinkCarried(ServerPlayer player, ItemStack carried, int amount) {
        carried.shrink(amount);
        if (carried.isEmpty()) {
            HiveLeaderOrganCarry.set(player, ItemStack.EMPTY);
        }
    }
}
