package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;
import org.tdddd.epca.impl.overworld.data.LikeBiomeSavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Full like biome snapshot (login, dimension change, "removeall"). Incremental updates use
 * {@link SyncLikeBiomeBatchPacket}.
 */
public class SyncLikeBiomesPacket {

    @Nullable
    private final ResourceLocation dimension;
    private final Map<Long, LikeBiome> entries;

    public SyncLikeBiomesPacket(@Nullable ResourceLocation dimension, Map<Long, LikeBiome> entries) {
        this.dimension = dimension;
        this.entries = entries;
    }

    public static void sendAll(ServerLevel level, LikeBiomeSavedData data) {
        ModNetwork.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension),
                new SyncLikeBiomesPacket(level.dimension().location(), data.entries()));
    }

    public static void sendTo(ServerPlayer player, ServerLevel level, LikeBiomeSavedData data) {
        ModNetwork.sendToPlayer(player,
                new SyncLikeBiomesPacket(level.dimension().location(), data.entries()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.dimension != null);
        if (this.dimension != null) {
            buf.writeResourceLocation(this.dimension);
        }
        buf.writeVarInt(this.entries.size());
        this.entries.forEach((key, value) -> {
            buf.writeLong(key);
            buf.writeResourceLocation(value.biomeId());
            buf.writeBoolean(value.overrideColors());
            buf.writeBoolean(value.overrideRain());
            buf.writeVarInt(value.spawnMode());
            buf.writeBoolean(value.hasExplicitStage());
            if (value.hasExplicitStage()) {
                buf.writeInt(value.erosionStage());
            }
        });
    }

    public static SyncLikeBiomesPacket decode(FriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readBoolean() ? buf.readResourceLocation() : null;
        int size = buf.readVarInt();
        Map<Long, LikeBiome> entries = new HashMap<>(Math.max(4, size));
        for (int i = 0; i < size; i++) {
            long key = buf.readLong();
            ResourceLocation id = buf.readResourceLocation();
            boolean colors = buf.readBoolean();
            boolean rain = buf.readBoolean();
            int spawn = buf.readVarInt();
            int stage = buf.readBoolean() ? buf.readInt() : LikeBiome.STAGE_INHERIT;
            entries.put(key, new LikeBiome(id, colors, rain, spawn, stage));
        }
        return new SyncLikeBiomesPacket(dimension, entries);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Set<Long> dirtySections = LikeBiomeClientCache.replaceAll(this.dimension, this.entries);
            if (!dirtySections.isEmpty()) {
                org.tdddd.epca.impl.client.LikeBiomeClientRerender.invalidateSections(dirtySections);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
