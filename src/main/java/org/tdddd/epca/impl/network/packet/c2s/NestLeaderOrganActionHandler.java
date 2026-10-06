package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganCarry;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotUnlock;

/**
 * {@link NestLeaderOrganActionPacket} <b></b>
 * STAGE 2  SPEC D
 *
 * <h2></h2>
 * <ol>
 *   <li><b></b>{@code 0 <= slot < data.size()}
 *       {@link OrganSlotGroup#byGlobalIndex(int)} </li>
 *   <li><b></b> {@code data.isUnlocked(slot)}
 *       "" {@link OrganSlotUnlock#tryUnlock}
 *       SPEC D2</li>
 *   <li><b></b>{@link OrganSlotGroup}
 *        {@link NestLeaderOrganGate#canModifySlot}
 *       39  777 33 SPEC D3/D4
 *       SPEC </li>
 *   <li><b></b> {@link NestLeaderOrganData#acceptsItem}
 *       {@code epca:organ_part}SPEC D1</li>
 *   <li><b></b></li>
 * </ol>
 * <p><b></b>
 * {@code NestLeaderOrganActionPacket#handle}
 * </p>
 *
 * <h2></h2>
 * <p> {@link NestLeaderOrganCarry}
 * <b></b>GUI
 *
 *  return </p>
 *
 * <h2>1.20.1 -&gt; 26.1.2  API </h2>
 * <ul>
 *   <li>{@code Inventory#items}1.20.1  {@code NonNullList} 26.1.2
 *       <b></b>_tmp_26src Inventory.java  55
 *       {@code Inventory#getItem(int)} / {@code #setItem(int, ItemStack)}
 *        430 / 377  {@code #getNonEquipmentItems()} 90
 *        {@code inventory.items.get/set(...)}
 *       {@code inventory.getItem(...)} / {@code inventory.setItem(...)}
 *       {@code Inventory#setChanged()} 457 </li>
 *   <li>{@code ItemStack#isSameItemSameTags(a, b)}  26.1.2
 *       {@code ItemStack#isSameItemSameComponents(a, b)}
 *       _tmp_26src ItemStack.java  684  1.20.5  components</li>
 *   <li>{@code Player#displayClientMessage(Component, true)}
 *       {@code ServerPlayer#sendSystemMessage(Component, boolean)} ServerPlayer</li>
 * </ul>
 */
public final class NestLeaderOrganActionHandler {

    private NestLeaderOrganActionHandler() {
    }

    /**
     *
     *
     * @param player
     * @param data
     * @param msg
     */
    public static void handle(ServerPlayer player, NestLeaderOrganData data,
                              NestLeaderOrganActionPacket msg) {
        if (player == null || data == null || msg == null) return;

        if (msg.action() == NestLeaderOrganActionPacket.Action.RETURN_CARRIED) {
            // GUI
            NestLeaderOrganCarry.returnToPlayer(player);
            return;
        }

        if (msg.action() == NestLeaderOrganActionPacket.Action.INVENTORY_SLOT) {
            //  Inventory 035
            //  tag /  / 777  canModify
            // Shift  = INVENTORY_SLOT + BUTTON_MIDDLE
            if (msg.button() == NestLeaderOrganActionPacket.BUTTON_MIDDLE) {
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
                //  =  handle()
                //  clickInventory
            }
            default -> {
                // RETURN_CARRIED
            }
        }
    }

    //  SPEC GUI

    /**
     *  035 39  + 9
     *
     * <p> {@code AbstractContainerMenu#doClick}  {@code ClickType.PICKUP} </p>
     * <ul>
     *   <li> + {@code (count + 1) / 2}</li>
     *   <li> +  1 </li>
     *   <li> +  /  1 </li>
     * </ul>
     * <p><b></b> {@code ServerPlayer#getInventory()}
     *  tick  {@code containerMenu.broadcastChanges()}
     * {@code remoteSlots}
     *  tick  S2C </p>
     */
    private static void clickInventory(ServerPlayer player, NestLeaderOrganData data, int slot, int button) {
        Inventory inventory = player.getInventory();
        // Inventory#INVENTORY_SIZE = 36 30
        if (slot < 0 || slot >= NestLeaderOrganActionPacket.INVENTORY_SLOT_COUNT
                || slot >= inventory.getNonEquipmentItems().size()) {
            return;
        }

        ItemStack carried = NestLeaderOrganCarry.get(player);
        ItemStack inSlot = inventory.getItem(slot);
        boolean rightClick = button == NestLeaderOrganActionPacket.BUTTON_RIGHT;

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
            NestLeaderOrganCarry.set(player, taken);
            return;
        }

        if (inSlot.isEmpty()) {
            int amount = rightClick ? 1 : carried.getCount();
            inventory.setItem(slot, carried.copyWithCount(amount));
            inventory.setChanged();
            shrinkCarried(player, carried, amount);
            return;
        }

        // 26.1.2isSameItemSameTags -> isSameItemSameComponentsItemStack.java  684
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

        //  1
        NestLeaderOrganCarry.set(player, inSlot.copy());
        inventory.setItem(slot, carried.copy());
        inventory.setChanged();
    }

    /**
     * Shift <b></b><b></b>
     *
     * <p>
     *  777
     * </p>
     */
    private static void quickMoveInventoryToOrgan(ServerPlayer player, NestLeaderOrganData data, int fromSlot) {
        Inventory inventory = player.getInventory();
        if (fromSlot < 0 || fromSlot >= NestLeaderOrganActionPacket.INVENTORY_SLOT_COUNT
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
                    // /
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

    /**
     * Shift shift
     *
     * <p>quickMoveStack
     * </p>
     */
    private static void quickMoveFromInventory(ServerPlayer player, NestLeaderOrganData data, int slot) {
        if (!NestLeaderOrganCarry.get(player).isEmpty()) {
            NestLeaderOrganCarry.returnToPlayer(player);
            return;
        }
        quickMoveInventoryToOrgan(player, data, slot);
    }


    /**  */
    private static boolean canInsertIntoOrgan(ServerPlayer player, NestLeaderOrganData data,
                                              int index, OrganSlotGroup group) {
        if (group.kind().isInnerGrid()) {
            return NestLeaderOrganGate.canModifySlot(player, data, index);
        }
        return data.isUnlocked(index);
    }

    //   /

    /**
     *
     * <ul>
     *   <li><b> +  + </b>
     *       SPEC D2</li>
     *   <li> +  /  1</li>
     *   <li> +  /  1 </li>
     *   <li> +  1
     *       </li>
     * </ul>
     *
     * <p><b> return</b>
     * {@code canModify}
     * {@code carried.isEmpty() && inSlot.isEmpty()}
     *  SPEC D2 </p>
     */
    private static void click(ServerPlayer player, NestLeaderOrganData data, int slot,
                              OrganSlotGroup group, int button) {
        ItemStack carried = NestLeaderOrganCarry.get(player);
        ItemStack inSlot = data.getItem(slot);

        if (carried.isEmpty()) {
            //  SPEC D2
            boolean lockedOuterSlot = group.kind().needsUnlock() && !data.isUnlocked(slot);
            if (inSlot.isEmpty() && lockedOuterSlot) {
                //  =
                //  =
                tryUnlockSlot(player, data, slot, group);
                return;
            }
            //   + ""
            if (inSlot.isEmpty()) return;
            if (!canModify(player, data, slot, group, false)) return;

            int amount = button == NestLeaderOrganActionPacket.BUTTON_RIGHT
                    ? Math.max(1, inSlot.getCount() / 2)
                    : inSlot.getCount();
            ItemStack taken = inSlot.copyWithCount(amount);
            if (amount >= inSlot.getCount()) {
                data.clearItem(slot);
            } else {
                inSlot.shrink(amount);
            }
            NestLeaderOrganCarry.set(player, taken);
            return;
        }

        if (!canModify(player, data, slot, group, true)) return;
        if (!NestLeaderOrganData.acceptsItem(carried)) {
            player.sendSystemMessage(Component.literal("这个槽位只能放入「器官部位」标签的物品"), true);
            return;
        }

        if (inSlot.isEmpty()) {
            int amount = button == NestLeaderOrganActionPacket.BUTTON_RIGHT ? 1 : carried.getCount();
            data.setItem(slot, carried.copyWithCount(amount));
            shrinkCarried(player, carried, amount);
            return;
        }

        boolean mergeable = ItemStack.isSameItemSameComponents(inSlot, carried)
                && inSlot.getCount() < inSlot.getMaxStackSize();
        if (mergeable) {
            int space = inSlot.getMaxStackSize() - inSlot.getCount();
            int amount = button == NestLeaderOrganActionPacket.BUTTON_RIGHT
                    ? Math.min(1, space) : Math.min(space, carried.getCount());
            if (amount <= 0) return;
            inSlot.grow(amount);
            shrinkCarried(player, carried, amount);
            return;
        }

        //  1
        // ""
        if (!NestLeaderOrganData.acceptsItem(inSlot)) {
            return;
        }
        ItemStack old = inSlot.copy();
        data.setItem(slot, carried.copy());
        NestLeaderOrganCarry.set(player, old);
    }

    //  Shift

    /**
     * Shift
     *
     * <p> {@code QUICK_MOVE}
     * <b></b>
     *  {@link NestLeaderOrganCarry#giveBack}
     *  Shift  = Shift  = </p>
     */
    private static void quickMove(ServerPlayer player, NestLeaderOrganData data, int slot,
                                  OrganSlotGroup group) {
        ItemStack carried = NestLeaderOrganCarry.get(player);

        if (carried.isEmpty()) {
            ItemStack inSlot = data.getItem(slot);
            if (inSlot.isEmpty()) return;
            if (!canModify(player, data, slot, group, false)) return;
            ItemStack taken = inSlot.copy();
            data.clearItem(slot);
            NestLeaderOrganCarry.giveBack(player, taken);
            if (player.getInventory() != null) {
                player.getInventory().setChanged();
            }
            return;
        }

        if (!canModify(player, data, slot, group, true)) return;
        if (!NestLeaderOrganData.acceptsItem(carried)) {
            player.sendSystemMessage(Component.literal("这个槽位只能放入「器官部位」标签的物品"), true);
            return;
        }

        ItemStack inSlot = data.getItem(slot);
        if (inSlot.isEmpty()) {
            data.setItem(slot, carried.copy());
            NestLeaderOrganCarry.set(player, ItemStack.EMPTY);
            return;
        }
        if (!ItemStack.isSameItemSameComponents(inSlot, carried)) {
            // Shift
            return;
        }
        int space = inSlot.getMaxStackSize() - inSlot.getCount();
        if (space <= 0) return;
        int amount = Math.min(space, carried.getCount());
        inSlot.grow(amount);
        shrinkCarried(player, carried, amount);
    }


    /**  */
    private static void clear(ServerPlayer player, NestLeaderOrganData data, int slot,
                              OrganSlotGroup group) {
        ItemStack inSlot = data.getItem(slot);
        if (inSlot.isEmpty()) return;
        if (!canModify(player, data, slot, group, false)) return;
        ItemStack taken = inSlot.copy();
        data.clearItem(slot);
        NestLeaderOrganCarry.giveBack(player, taken);
    }


    /**
     * {@code wantsInsert} "/"
     * SPEC D2
     */
    private static boolean canModify(ServerPlayer player, NestLeaderOrganData data, int slot,
                                     OrganSlotGroup group, boolean wantsInsert) {
        if (group.kind().isInnerGrid()) {
            if (NestLeaderOrganGate.canModifySlot(player, data, slot)) {
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
            // ""
            return false;
        }

        return tryUnlockSlot(player, data, slot, group);
    }

    /**
     * SPEC D2
     *
     * <p> /  /  {@code BiomassSyncPacket}  {@link OrganSlotUnlock#tryUnlock}
     * 26.1.2
     * {@code ServerPlayer#sendSystemMessage(Component, true)}
     *  {@link OrganSlotUnlock.Result#reason()}
     * {@code  5 2}</p>
     *
     * @return  true false
     */
    private static boolean tryUnlockSlot(ServerPlayer player, NestLeaderOrganData data, int slot,
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

    /**  {@code amount} */
    private static void shrinkCarried(ServerPlayer player, ItemStack carried, int amount) {
        carried.shrink(amount);
        if (carried.isEmpty()) {
            NestLeaderOrganCarry.set(player, ItemStack.EMPTY);
        }
    }
}

