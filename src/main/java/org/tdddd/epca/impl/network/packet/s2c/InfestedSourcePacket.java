package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;

import java.util.function.Supplier;

public class InfestedSourcePacket {
    public static class AddInfestedSourcePacket {
        private final BlockPos pos;

        public AddInfestedSourcePacket(BlockPos pos) {
            this.pos = pos;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public static AddInfestedSourcePacket decode(FriendlyByteBuf buf) {
            return new AddInfestedSourcePacket(buf.readBlockPos());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // 客户端执行
                WaterColorEffectsManager.addInfestedSource(pos);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // 移除虫染方块包（类似，用于反转换或破坏时同步）
    public static class RemoveInfestedSourcePacket {
        private final BlockPos pos;

        public RemoveInfestedSourcePacket(BlockPos pos) {
            this.pos = pos;
        }
        public void encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public static RemoveInfestedSourcePacket decode(FriendlyByteBuf buf) {
            return new RemoveInfestedSourcePacket(buf.readBlockPos());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // 客户端执行
                WaterColorEffectsManager.removeInfestedSource(pos);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}