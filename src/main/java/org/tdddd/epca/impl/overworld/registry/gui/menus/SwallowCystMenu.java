package org.tdddd.epca.impl.overworld.registry.gui.menus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.overworld.registry.ModMenus;

public class SwallowCystMenu extends AbstractContainerMenu {
    private final SwallowCystBlockEntity blockEntity;
    private final Inventory playerInventory;

    public SwallowCystMenu(int id, Inventory inv, SwallowCystBlockEntity be) {
        super(ModMenus.SWALLOW_CYST.get(), id);
        this.blockEntity = be;
        this.playerInventory = inv;

        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new SlotItemHandler(be.getInventory(), col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        blockEntity.onPlayerTake(player, stack);
                        super.onTake(player, stack);
                    }
                });
            }
        }

        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            stack = slotStack.copy();
            if (index < 27) { 
                if (!this.moveItemStackTo(slotStack, 27, 63, true)) {
                    return ItemStack.EMPTY;
                }
            } else { 
                if (!this.moveItemStackTo(slotStack, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return stack;
    }
}