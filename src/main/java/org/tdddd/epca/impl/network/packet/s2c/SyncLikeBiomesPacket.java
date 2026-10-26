package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;
import org.tdddd.epca.impl.overworld.data.LikeBiomeSavedData;

import java.util.HashMap;
import java.util.Map;

/** Sends the whole like biome table of one dimension to its clients. */
public class SyncLikeBiomesPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncLikeBiomesPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_like_biomes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLikeBiomesPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncLikeBiomesPacket::encode, SyncLikeBiomesPacket::decode);

    private final Map<Long, LikeBiome> entries;

    public SyncLikeBiomesPacket(Map<Long, LikeBiome> entries) {
        this.entries = entries;
    }

    public static void sendAll(ServerLevel level, LikeBiomeSavedData data) {
        ModNetwork.sendToDimension(level, new SyncLikeBiomesPacket(data.entries()));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entries.size());
        this.entries.forEach((key, value) -> {
            buf.writeLong(key);
            buf.writeUtf(value.biomeId().toString());
            buf.writeBoolean(value.overrideColors());
            buf.writeBoolean(value.overrideRain());
            buf.writeVarInt(value.spawnMode());
        });
    }

    public static SyncLikeBiomesPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<Long, LikeBiome> entries = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            long key = buf.readLong();
            Identifier id = Identifier.parse(buf.readUtf());
            boolean colors = buf.readBoolean();
            boolean rain = buf.readBoolean();
            int spawn = buf.readVarInt();
            entries.put(key, new LikeBiome(id, colors, rain, spawn));
        }
        return new SyncLikeBiomesPacket(entries);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncLikeBiomesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> LikeBiomeClientCache.replaceAll(packet.entries));
    }
}
