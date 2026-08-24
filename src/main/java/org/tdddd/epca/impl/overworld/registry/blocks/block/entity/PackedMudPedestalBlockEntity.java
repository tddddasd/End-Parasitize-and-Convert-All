package org.tdddd.epca.impl.overworld.registry.blocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.tdddd.epca.impl.overworld.registry.blocks.block.PackedMudPedestal;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.PedestalItemSyncPacket;

public class PackedMudPedestalBlockEntity extends BlockEntity {
    private final ItemStackHandler itemHandler = createHandler();
    private final Map<Direction, LazyOptional<IItemHandler>> sideHandlers = new EnumMap<>(Direction.class);

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(1) {
            protected void onContentsChanged(int slot) {
                setChanged();
                if (level != null) {
                    requestModelDataUpdate();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return true;
            }

            @Override
            public int getSlotLimit(int slot) {
                // 修改为 64，允许堆叠物品
                return 64;
            }
        };
    }

    public boolean hasItem() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }

    public ItemStack getItem() {
        return itemHandler.getStackInSlot(0);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            requestModelDataUpdate();
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            LazyOptional<IItemHandler> handler = sideHandlers.get(side);
            if (handler != null) {
                return handler.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<IItemHandler> handler : sideHandlers.values()) {
            handler.invalidate();
        }
        sideHandlers.clear();
    }

    private List<String> filterData = Collections.emptyList();

    public List<String> getFilterData() { return filterData; }

    public void setFilterData(List<String> data) {
        this.filterData = data == null ? Collections.emptyList() : new ArrayList<>(data);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("inventory"));
        }
        if (tag.contains("filterData")) {
            ListTag list = tag.getList("filterData", Tag.TAG_STRING);
            filterData = new ArrayList<>();
            for (Tag t : list) {
                filterData.add(t.getAsString());
            }
        } else {
            filterData = Collections.emptyList();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        ListTag list = new ListTag();
        for (String s : filterData) {
            list.add(StringTag.valueOf(s));
        }
        tag.put("filterData", list);
    }

    // 添加静态匹配方法（供内外调用）
    public static boolean matchesFilter(ItemStack stack, List<String> filters) {
        if (filters.isEmpty()) return true;
        ResourceLocation stackId = stack.getItem().builtInRegistryHolder().key().location();
        for (String filter : filters) {
            if (filter.isEmpty()) continue;
            try {
                ResourceLocation filterId = new ResourceLocation(filter);
                if (stackId.equals(filterId)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private class SideFilteredItemHandler implements IItemHandler {
        private final Direction side;

        SideFilteredItemHandler(Direction side) {
            this.side = side;
        }

        private boolean isLocked() {
            if (level == null) return true;
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof PackedMudPedestal pedestal) {
                return pedestal.isLocked(level, worldPosition);
            }
            return true;
        }

        private void notifyChange() {
            setChanged();
            if (level != null && !level.isClientSide) {
                requestModelDataUpdate();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                syncToClient();
            }
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (isLocked()) return stack;
            if (side == Direction.UP || side == Direction.DOWN) return stack;
            if (!itemHandler.getStackInSlot(slot).isEmpty()) return stack;

            if (!PackedMudPedestalBlockEntity.this.isMainPedestal()) {
                if (!PackedMudPedestalBlockEntity.this.filterData.isEmpty()) {
                    if (!PackedMudPedestalBlockEntity.matchesFilter(stack,
                            PackedMudPedestalBlockEntity.this.filterData)) {
                        return stack;
                    }
                }
            }

            ItemStack remaining = itemHandler.insertItem(slot, stack, simulate);
            if (!simulate && (remaining.isEmpty() || remaining.getCount() < stack.getCount())) {
                notifyChange();
            }
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isLocked()) return ItemStack.EMPTY;
            if (side != Direction.DOWN) return ItemStack.EMPTY;

            ItemStack extracted = itemHandler.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) {
                notifyChange();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isLocked()) return false;
            if (side == Direction.UP || side == Direction.DOWN) return false;
            return itemHandler.getStackInSlot(slot).isEmpty() && itemHandler.isItemValid(slot, stack);
        }
    }

    private int clientRenderTick = 0;

    public static void tick(Level level, BlockPos pos, BlockState state, PackedMudPedestalBlockEntity be) {
        if (level.isClientSide) {
            be.clientRenderTick++;
            if (be.clientRenderTick >= 10) {
                be.clientRenderTick = 0;
                be.requestModelDataUpdate();
                if (be.level != null) {
                    be.level.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void syncItem(ItemStack stack) {
        // 不再强制 setCount(1)，保留原始堆叠数量
        if (stack.isEmpty()) {
            clearItem();
        } else {
            // 直接复制，不修改数量
            itemHandler.setStackInSlot(0, stack.copy());
        }
        requestModelDataUpdate();
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void syncToClient() {
        if (level == null || level.isClientSide) return;
        ItemStack stack = getItem();
        Packet<?> packet = ModNetwork.INSTANCE.toVanillaPacket(
                new PedestalItemSyncPacket(worldPosition, stack),
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
        ServerLevel serverLevel = (ServerLevel) level;
        LevelChunk chunk = serverLevel.getChunkAt(worldPosition);
        serverLevel.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false).forEach(player -> {
            player.connection.send(packet);
        });
    }

    public void setItem(ItemStack stack) {
        // 不再强制 setCount(1)，保留原始数量
        if (stack.isEmpty()) {
            clearItem();
        } else {
            itemHandler.setStackInSlot(0, stack.copy());
            syncToClient();
        }
    }

    public void clearItem() {
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        syncToClient();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            syncToClient();
        }
    }


    public PackedMudPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PACKED_MUD_PEDESTAL.get(), pos, state);
        for (Direction dir : Direction.values()) {
            sideHandlers.put(dir, LazyOptional.of(() -> new SideFilteredItemHandler(dir)));
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    // 在 PackedMudPedestalBlockEntity 中添加/重写以下方法

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag); // 自动保存 filterData 和 inventory
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            requestModelDataUpdate();
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 添加字段
    private boolean isMainPedestal = false;

    public void setMainPedestal(boolean isMain) {
        this.isMainPedestal = isMain;
    }

    public boolean isMainPedestal() {
        return isMainPedestal;
    }
}