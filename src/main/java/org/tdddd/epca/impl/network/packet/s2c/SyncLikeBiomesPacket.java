package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;
import org.tdddd.epca.impl.overworld.data.LikeBiomeSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Full like biome snapshot (login, dimension change, "removeall"). Incremental updates use
 * {@link SyncLikeBiomeBatchPacket}.
 */
public class SyncLikeBiomesPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncLikeBiomesPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_like_biomes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLikeBiomesPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncLikeBiomesPacket::encode, SyncLikeBiomesPacket::decode);

    @Nullable
    private final Identifier dimension;
    private final Map<Long, LikeBiome> entries;

    public SyncLikeBiomesPacket(@Nullable Identifier dimension, Map<Long, LikeBiome> entries) {
        this.dimension = dimension;
        this.entries = entries;
    }

    public static void sendAll(ServerLevel level, LikeBiomeSavedData data) {
        ModNetwork.sendToDimension(level,
                new SyncLikeBiomesPacket(level.dimension().identifier(), data.entries()));
    }

    public static void sendTo(ServerPlayer player, ServerLevel level, LikeBiomeSavedData data) {
        ModNetwork.sendToPlayer(player,
                new SyncLikeBiomesPacket(level.dimension().identifier(), data.entries()));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.dimension != null);
        if (this.dimension != null) {
            buf.writeIdentifier(this.dimension);
        }
        buf.writeVarInt(this.entries.size());
        this.entries.forEach((key, value) -> {
            buf.writeLong(key);
            buf.writeIdentifier(value.biomeId());
            buf.writeBoolean(value.overrideColors());
            buf.writeBoolean(value.overrideRain());
            buf.writeVarInt(value.spawnMode());
            buf.writeBoolean(value.hasExplicitStage());
            if (value.hasExplicitStage()) {
                buf.writeInt(value.erosionStage());
            }
        });
    }

    public static SyncLikeBiomesPacket decode(RegistryFriendlyByteBuf buf) {
        Identifier dimension = buf.readBoolean() ? buf.readIdentifier() : null;
        int size = buf.readVarInt();
        Map<Long, LikeBiome> entries = new HashMap<>(Math.max(4, size));
        for (int i = 0; i < size; i++) {
            long key = buf.readLong();
            Identifier id = buf.readIdentifier();
            boolean colors = buf.readBoolean();
            boolean rain = buf.readBoolean();
            int spawn = buf.readVarInt();
            int stage = buf.readBoolean() ? buf.readInt() : LikeBiome.STAGE_INHERIT;
            entries.put(key, new LikeBiome(id, colors, rain, spawn, stage));
        }
        return new SyncLikeBiomesPacket(dimension, entries);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncLikeBiomesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Set<Long> dirtySections = LikeBiomeClientCache.replaceAll(packet.dimension, packet.entries);
            if (!dirtySections.isEmpty()) {
                org.tdddd.epca.impl.client.LikeBiomeClientRerender.invalidateSections(dirtySections);
            }
        });
    }
}
