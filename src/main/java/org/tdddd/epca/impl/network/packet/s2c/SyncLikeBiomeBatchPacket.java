package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incremental (batched) like biome synchronisation.
 *
 * <p>All unit changes of a server tick are aggregated by
 * {@link org.tdddd.epca.impl.overworld.data.LikeBiomeSync} and sent with this packet at the end of
 * the tick. Layout is compact:</p>
 * <pre>
 *   dimension          Identifier
 *   palette length     VarInt, then that many Identifiers (biome ids)
 *   upsert count       VarInt
 *     per upsert: unit coords as 3 zig-zag VarInts, palette index VarInt,
 *                 flags byte (1=colors, 2=rain, 4=explicit erosion stage),
 *                 spawn mode VarInt, erosion stage zig-zag VarInt (only when flag 4 is set)
 *   removal count      VarInt, then that many unit coordinate triplets
 * </pre>
 * A typical upsert costs 6-8 bytes, a removal 3-6 bytes, so even a few thousand units per tick stay
 * far below the vanilla payload limit.
 */
public class SyncLikeBiomeBatchPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncLikeBiomeBatchPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_like_biome_batch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLikeBiomeBatchPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncLikeBiomeBatchPacket::encode, SyncLikeBiomeBatchPacket::decode);

    private static final int FLAG_COLORS = 1;

    private static final int FLAG_RAIN = 2;

    private static final int FLAG_STAGE = 4;

    private final Identifier dimension;
    private final Map<Long, LikeBiome> upserts;
    private final List<Long> removals;

    public SyncLikeBiomeBatchPacket(Identifier dimension, Map<Long, LikeBiome> upserts,
                                    List<Long> removals) {
        this.dimension = dimension;
        this.upserts = upserts;
        this.removals = removals;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeIdentifier(this.dimension);

        List<Identifier> palette = new ArrayList<>();
        Map<Identifier, Integer> paletteIndex = new HashMap<>();
        for (LikeBiome value : this.upserts.values()) {
            if (!paletteIndex.containsKey(value.biomeId())) {
                paletteIndex.put(value.biomeId(), palette.size());
                palette.add(value.biomeId());
            }
        }
        buf.writeVarInt(palette.size());
        for (Identifier id : palette) {
            buf.writeIdentifier(id);
        }

        buf.writeVarInt(this.upserts.size());
        for (Map.Entry<Long, LikeBiome> entry : this.upserts.entrySet()) {
            long key = entry.getKey();
            writeUnitKey(buf, key);
            LikeBiome value = entry.getValue();
            buf.writeVarInt(paletteIndex.get(value.biomeId()));
            int flags = (value.overrideColors() ? FLAG_COLORS : 0)
                    | (value.overrideRain() ? FLAG_RAIN : 0)
                    | (value.hasExplicitStage() ? FLAG_STAGE : 0);
            buf.writeByte(flags);
            buf.writeVarInt(value.spawnMode());
            if (value.hasExplicitStage()) {
                writeSignedVarInt(buf, value.erosionStage());
            }
        }

        buf.writeVarInt(this.removals.size());
        for (Long key : this.removals) {
            writeUnitKey(buf, key);
        }
    }

    public static SyncLikeBiomeBatchPacket decode(RegistryFriendlyByteBuf buf) {
        Identifier dimension = buf.readIdentifier();

        int paletteSize = buf.readVarInt();
        List<Identifier> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            palette.add(buf.readIdentifier());
        }

        int upsertCount = buf.readVarInt();
        Map<Long, LikeBiome> upserts = new LinkedHashMap<>(Math.max(4, upsertCount));
        for (int i = 0; i < upsertCount; i++) {
            long key = readUnitKey(buf);
            int paletteIdx = buf.readVarInt();
            int flags = buf.readUnsignedByte();
            int spawnMode = buf.readVarInt();
            int stage = (flags & FLAG_STAGE) != 0 ? readSignedVarInt(buf) : LikeBiome.STAGE_INHERIT;
            Identifier id;
            if (palette.isEmpty()) {
                id = Identifier.fromNamespaceAndPath("minecraft", "plains");
            } else {
                id = palette.get(Math.max(0, Math.min(paletteIdx, palette.size() - 1)));
            }
            upserts.put(key, new LikeBiome(id, (flags & FLAG_COLORS) != 0, (flags & FLAG_RAIN) != 0,
                    spawnMode, stage));
        }

        int removalCount = buf.readVarInt();
        List<Long> removals = new ArrayList<>(Math.max(4, removalCount));
        for (int i = 0; i < removalCount; i++) {
            removals.add(readUnitKey(buf));
        }
        return new SyncLikeBiomeBatchPacket(dimension, upserts, removals);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncLikeBiomeBatchPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Set<Long> dirtySections = LikeBiomeClientCache.applyBatch(packet.dimension, packet.upserts,
                    packet.removals);
            if (!dirtySections.isEmpty()) {
                org.tdddd.epca.impl.client.LikeBiomeClientRerender.invalidateSections(dirtySections);
            }
        });
    }

    private static void writeUnitKey(RegistryFriendlyByteBuf buf, long key) {
        writeSignedVarInt(buf, LikeBiomeManager.unitX(key));
        writeSignedVarInt(buf, LikeBiomeManager.unitY(key));
        writeSignedVarInt(buf, LikeBiomeManager.unitZ(key));
    }

    private static long readUnitKey(RegistryFriendlyByteBuf buf) {
        int x = readSignedVarInt(buf);
        int y = readSignedVarInt(buf);
        int z = readSignedVarInt(buf);
        return LikeBiomeManager.unitKey(x, y, z);
    }

    private static void writeSignedVarInt(RegistryFriendlyByteBuf buf, int value) {
        buf.writeVarInt((value << 1) ^ (value >> 31));
    }

    private static int readSignedVarInt(RegistryFriendlyByteBuf buf) {
        int raw = buf.readVarInt();
        return (raw >>> 1) ^ -(raw & 1);
    }
}
