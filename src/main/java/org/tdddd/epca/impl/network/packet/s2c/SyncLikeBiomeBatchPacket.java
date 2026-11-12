package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeClientCache;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Incremental (batched) like biome synchronisation.
 *
 * <p>All unit changes of a server tick are aggregated by
 * {@link org.tdddd.epca.impl.overworld.data.LikeBiomeSync} and sent with this packet at the end of
 * the tick. Layout is compact:</p>
 * <pre>
 *   dimension          ResourceLocation
 *   palette length     VarInt, then that many ResourceLocations (biome ids)
 *   upsert count       VarInt
 *     per upsert: unit coords as 3 zig-zag VarInts, palette index VarInt,
 *                 flags byte (1=colors, 2=rain, 4=explicit erosion stage),
 *                 spawn mode VarInt, erosion stage zig-zag VarInt (only when flag 4 is set)
 *   removal count      VarInt, then that many unit coordinate triplets
 * </pre>
 * A typical upsert costs 6-8 bytes, a removal 3-6 bytes, so even a few thousand units per tick stay
 * far below the vanilla payload limit.
 */
public class SyncLikeBiomeBatchPacket {

    private static final int FLAG_COLORS = 1;

    private static final int FLAG_RAIN = 2;

    private static final int FLAG_STAGE = 4;

    private final ResourceLocation dimension;
    private final Map<Long, LikeBiome> upserts;
    private final List<Long> removals;

    public SyncLikeBiomeBatchPacket(ResourceLocation dimension, Map<Long, LikeBiome> upserts,
                                    List<Long> removals) {
        this.dimension = dimension;
        this.upserts = upserts;
        this.removals = removals;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.dimension);

        List<ResourceLocation> palette = new ArrayList<>();
        Map<ResourceLocation, Integer> paletteIndex = new HashMap<>();
        for (LikeBiome value : this.upserts.values()) {
            if (!paletteIndex.containsKey(value.biomeId())) {
                paletteIndex.put(value.biomeId(), palette.size());
                palette.add(value.biomeId());
            }
        }
        buf.writeVarInt(palette.size());
        for (ResourceLocation id : palette) {
            buf.writeResourceLocation(id);
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

    public static SyncLikeBiomeBatchPacket decode(FriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readResourceLocation();

        int paletteSize = buf.readVarInt();
        List<ResourceLocation> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            palette.add(buf.readResourceLocation());
        }

        int upsertCount = buf.readVarInt();
        Map<Long, LikeBiome> upserts = new LinkedHashMap<>(Math.max(4, upsertCount));
        for (int i = 0; i < upsertCount; i++) {
            long key = readUnitKey(buf);
            int paletteIdx = buf.readVarInt();
            int flags = buf.readUnsignedByte();
            int spawnMode = buf.readVarInt();
            int stage = (flags & FLAG_STAGE) != 0 ? readSignedVarInt(buf) : LikeBiome.STAGE_INHERIT;
            ResourceLocation id;
            if (palette.isEmpty()) {
                id = new ResourceLocation("minecraft", "plains");
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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Set<Long> dirtySections = LikeBiomeClientCache.applyBatch(this.dimension, this.upserts,
                    this.removals);
            if (!dirtySections.isEmpty()) {
                org.tdddd.epca.impl.client.LikeBiomeClientRerender.invalidateSections(dirtySections);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void writeUnitKey(FriendlyByteBuf buf, long key) {
        writeSignedVarInt(buf, LikeBiomeManager.unitX(key));
        writeSignedVarInt(buf, LikeBiomeManager.unitY(key));
        writeSignedVarInt(buf, LikeBiomeManager.unitZ(key));
    }

    private static long readUnitKey(FriendlyByteBuf buf) {
        int x = readSignedVarInt(buf);
        int y = readSignedVarInt(buf);
        int z = readSignedVarInt(buf);
        return LikeBiomeManager.unitKey(x, y, z);
    }

    private static void writeSignedVarInt(FriendlyByteBuf buf, int value) {
        buf.writeVarInt((value << 1) ^ (value >> 31));
    }

    private static int readSignedVarInt(FriendlyByteBuf buf) {
        int raw = buf.readVarInt();
        return (raw >>> 1) ^ -(raw & 1);
    }
}
