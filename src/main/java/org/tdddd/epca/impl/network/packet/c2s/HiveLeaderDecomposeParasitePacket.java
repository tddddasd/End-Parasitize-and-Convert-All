package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.world.entity.LivingEntity;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;

public class HiveLeaderDecomposeParasitePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HiveLeaderDecomposeParasitePacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("hiveleader_decompose_parasite"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HiveLeaderDecomposeParasitePacket> STREAM_CODEC =
            CustomPacketPayload.codec(HiveLeaderDecomposeParasitePacket::encode,
                    HiveLeaderDecomposeParasitePacket::decode);

    public HiveLeaderDecomposeParasitePacket() {
    }

    public static void encode(HiveLeaderDecomposeParasitePacket packet, RegistryFriendlyByteBuf buf) {
    }

    public static HiveLeaderDecomposeParasitePacket decode(RegistryFriendlyByteBuf buf) {
        return new HiveLeaderDecomposeParasitePacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HiveLeaderDecomposeParasitePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            HiveLeaderDecomposeParasiteHandler.handleRequest(sender);
        });
    }
}
