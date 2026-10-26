package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;
import org.tdddd.epca.impl.overworld.data.LikeBiomeSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sends the whole like biome table of one dimension to its clients.
 *
 * <p>The table only changes when an operator runs {@code /epca_biome_like}, so sending it as a whole is
 * cheaper than tracking per section deltas.</p>
 */
public class SyncLikeBiomesPacket {

    private final Map<Long, LikeBiome> entries;

    public SyncLikeBiomesPacket(Map<Long, LikeBiome> entries) {
        this.entries = entries;
    }

    public static void sendAll(ServerLevel level, LikeBiomeSavedData data) {
        ModNetwork.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension),
                new SyncLikeBiomesPacket(data.entries()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entries.size());
        this.entries.forEach((key, value) -> {
            buf.writeLong(key);
            buf.writeResourceLocation(value.biomeId());
            buf.writeBoolean(value.overrideColors());
            buf.writeBoolean(value.overrideRain());
            buf.writeVarInt(value.spawnMode());
        });
    }

    public static SyncLikeBiomesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<Long, LikeBiome> entries = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            long key = buf.readLong();
            ResourceLocation id = buf.readResourceLocation();
            boolean colors = buf.readBoolean();
            boolean rain = buf.readBoolean();
            int spawn = buf.readVarInt();
            entries.put(key, new LikeBiome(id, colors, rain, spawn));
        }
        return new SyncLikeBiomesPacket(entries);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> LikeBiomeClientCache.replaceAll(this.entries));
        ctx.get().setPacketHandled(true);
    }
}
