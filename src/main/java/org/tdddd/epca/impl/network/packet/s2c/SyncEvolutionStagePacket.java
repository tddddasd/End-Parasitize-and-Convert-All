package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.ClientEvolutionData;

import java.util.function.Supplier;

public class SyncEvolutionStagePacket {
    private final ResourceLocation dimension;
    private final int stage;

    public SyncEvolutionStagePacket(ResourceLocation dimension, int stage) {
        this.dimension = dimension;
        this.stage = stage;
    }

    public static void encode(SyncEvolutionStagePacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.dimension);
        buf.writeInt(msg.stage);
    }

    public static SyncEvolutionStagePacket decode(FriendlyByteBuf buf) {
        return new SyncEvolutionStagePacket(buf.readResourceLocation(), buf.readInt());
    }

    public static void handle(SyncEvolutionStagePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientEvolutionData.updateStage(msg.dimension, msg.stage));
        ctx.get().setPacketHandled(true);
    }
}