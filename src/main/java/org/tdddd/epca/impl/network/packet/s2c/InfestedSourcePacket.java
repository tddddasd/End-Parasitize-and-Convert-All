package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;
import org.tdddd.epca.impl.overworld.data.InfestedBlockManager;

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
                
                WaterColorEffectsManager.addInfestedSource(pos);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    
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
                
                WaterColorEffectsManager.removeInfestedSource(pos);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class RequestAllInfestedSourcesPacket {
        public void encode(FriendlyByteBuf buf) {
            
        }

        public static RequestAllInfestedSourcesPacket decode(FriendlyByteBuf buf) {
            return new RequestAllInfestedSourcesPacket();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    InfestedBlockManager.syncAllToPlayer(player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}