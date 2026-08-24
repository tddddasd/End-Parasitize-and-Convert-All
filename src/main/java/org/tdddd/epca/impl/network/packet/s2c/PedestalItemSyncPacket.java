package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.PackedMudPedestalBlockEntity;

import java.util.function.Supplier;

public class PedestalItemSyncPacket {
    private final BlockPos pos;
    private final ItemStack stack;

    public PedestalItemSyncPacket(BlockPos pos, ItemStack stack) {
        this.pos = pos;
        this.stack = stack.copy();
    }

    public static void encode(PedestalItemSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeNbt(pkt.stack.save(new CompoundTag()));
    }

    public static PedestalItemSyncPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ItemStack stack = ItemStack.of(buf.readNbt());
        return new PedestalItemSyncPacket(pos, stack);
    }

    public static void handle(PedestalItemSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().level != null) {
                var be = Minecraft.getInstance().level.getBlockEntity(pkt.pos);
                if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                    pedestal.syncItem(pkt.stack);
                }
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}