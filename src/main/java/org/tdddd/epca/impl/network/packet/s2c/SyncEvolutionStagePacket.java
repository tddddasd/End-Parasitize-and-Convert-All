package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.ClientEvolutionData;
import org.tdddd.epca.impl.network.ModNetwork;


public class SyncEvolutionStagePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncEvolutionStagePacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_evolution_stage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEvolutionStagePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncEvolutionStagePacket::encode, SyncEvolutionStagePacket::decode);

    private final Identifier dimension;
    private final int stage;

    public SyncEvolutionStagePacket(Identifier dimension, int stage) {
        this.dimension = dimension;
        this.stage = stage;
    }

    public static void encode(SyncEvolutionStagePacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeIdentifier(msg.dimension);
        buf.writeInt(msg.stage);
    }

    public static SyncEvolutionStagePacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncEvolutionStagePacket(buf.readIdentifier(), buf.readInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncEvolutionStagePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientEvolutionData.updateStage(msg.dimension, msg.stage));
    }
}
