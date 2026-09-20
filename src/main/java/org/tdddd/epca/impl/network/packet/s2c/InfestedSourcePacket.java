package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.InfestedBlockManager;


public class InfestedSourcePacket {

    
    public static class AddInfestedSourcePacket implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AddInfestedSourcePacket> TYPE =
                new CustomPacketPayload.Type<>(ModNetwork.id("add_infested_source"));

        public static final StreamCodec<RegistryFriendlyByteBuf, AddInfestedSourcePacket> STREAM_CODEC =
                CustomPacketPayload.codec(AddInfestedSourcePacket::encode, AddInfestedSourcePacket::decode);

        private final BlockPos pos;

        public AddInfestedSourcePacket(BlockPos pos) {
            this.pos = pos;
        }

        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public static AddInfestedSourcePacket decode(RegistryFriendlyByteBuf buf) {
            return new AddInfestedSourcePacket(buf.readBlockPos());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(AddInfestedSourcePacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                
                WaterColorEffectsManager.addInfestedSource(packet.pos);
            });
        }
    }

    
    
    public static class RemoveInfestedSourcePacket implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RemoveInfestedSourcePacket> TYPE =
                new CustomPacketPayload.Type<>(ModNetwork.id("remove_infested_source"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveInfestedSourcePacket> STREAM_CODEC =
                CustomPacketPayload.codec(RemoveInfestedSourcePacket::encode, RemoveInfestedSourcePacket::decode);

        private final BlockPos pos;

        public RemoveInfestedSourcePacket(BlockPos pos) {
            this.pos = pos;
        }
        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public static RemoveInfestedSourcePacket decode(RegistryFriendlyByteBuf buf) {
            return new RemoveInfestedSourcePacket(buf.readBlockPos());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RemoveInfestedSourcePacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                
                WaterColorEffectsManager.removeInfestedSource(packet.pos);
            });
        }
    }

    
    public static class RequestAllInfestedSourcesPacket implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestAllInfestedSourcesPacket> TYPE =
                new CustomPacketPayload.Type<>(ModNetwork.id("request_all_infested_sources"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RequestAllInfestedSourcesPacket> STREAM_CODEC =
                CustomPacketPayload.codec(RequestAllInfestedSourcesPacket::encode, RequestAllInfestedSourcesPacket::decode);

        public void encode(RegistryFriendlyByteBuf buf) {
            
        }

        public static RequestAllInfestedSourcesPacket decode(RegistryFriendlyByteBuf buf) {
            return new RequestAllInfestedSourcesPacket();
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RequestAllInfestedSourcesPacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) {
                    InfestedBlockManager.syncAllToPlayer(player);
                }
            });
        }
    }
}
