package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.tdddd.epca.impl.epca;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.LongStream;


public class SacrificeSavedData extends SavedData {

    
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "sacrifice_ritual");

    
    private static final int AXIS_BITS = 21;
    private static final long AXIS_MASK = (1L << AXIS_BITS) - 1L;
    
    private static final int AXIS_LIMIT = (1 << (AXIS_BITS - 1)) - 1;

    private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("x").forGetter(BlockPos::getX),
            Codec.INT.fieldOf("y").forGetter(BlockPos::getY),
            Codec.INT.fieldOf("z").forGetter(BlockPos::getZ)
    ).apply(i, BlockPos::new));

    
    private static final Codec<long[]> POSITIONS_CODEC = Codec.LONG_STREAM
            .comapFlatMap(stream -> DataResult.success(stream.toArray()), LongStream::of);

    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    
    public record SacrificeEntry(ResourceKey<Level> dimension, BlockPos center, UUID playerId,
                                 long[] positions, int nextIndex) {}

    
    private record StoredEntry(Optional<ResourceKey<Level>> dimension, Optional<BlockPos> center,
                               Optional<String> playerId, long[] positions, int nextIndex) {
        private static final Codec<StoredEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                DIMENSION_CODEC.optionalFieldOf("Dimension").forGetter(StoredEntry::dimension),
                BLOCK_POS_CODEC.optionalFieldOf("Center").forGetter(StoredEntry::center),
                Codec.STRING.optionalFieldOf("Player").forGetter(StoredEntry::playerId),
                POSITIONS_CODEC.optionalFieldOf("Positions", new long[0]).forGetter(StoredEntry::positions),
                Codec.INT.optionalFieldOf("NextIndex", 0).forGetter(StoredEntry::nextIndex)
        ).apply(i, StoredEntry::new));
    }

    
    public static final Codec<SacrificeSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            StoredEntry.CODEC.listOf().optionalFieldOf("Tasks", List.of())
                    .flatXmap(SacrificeSavedData::decodeTasks, SacrificeSavedData::encodeTasks)
                    .forGetter(SacrificeSavedData::taskList)
    ).apply(i, SacrificeSavedData::new));

    public static final SavedDataType<SacrificeSavedData> TYPE =
            new SavedDataType<SacrificeSavedData>(DATA_ID, SacrificeSavedData::new, CODEC);

    private final List<SacrificeEntry> tasks = new ArrayList<>();

    public SacrificeSavedData() {}

    
    private SacrificeSavedData(List<SacrificeEntry> entries) {
        tasks.addAll(entries);
    }

    
    public static SacrificeSavedData get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<SacrificeEntry> taskList() {
        return new ArrayList<>(tasks);
    }

    
    public List<SacrificeEntry> getTasks() {
        return new ArrayList<>(tasks);
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    public void clear() {
        tasks.clear();
        setDirty();
    }

    
    public void setTasksForDimension(ResourceKey<Level> dimension, List<SacrificeEntry> entries) {
        tasks.removeIf(entry -> entry.dimension().equals(dimension));
        tasks.addAll(entries);
        setDirty();
    }

    
    public void updateProgress(ResourceKey<Level> dimension, BlockPos center, int nextIndex) {
        for (int i = 0; i < tasks.size(); i++) {
            SacrificeEntry entry = tasks.get(i);
            if (entry.dimension().equals(dimension) && entry.center().equals(center)) {
                tasks.set(i, new SacrificeEntry(entry.dimension(), entry.center(), entry.playerId(),
                        entry.positions(), Math.max(0, nextIndex)));
                setDirty();
                return;
            }
        }
    }

    
    public boolean removeTasksAt(ResourceKey<Level> dimension, BlockPos center) {
        boolean removed = tasks.removeIf(entry ->
                entry.dimension().equals(dimension) && entry.center().equals(center));
        if (removed) setDirty();
        return removed;
    }

    
    public boolean removeTasksForPlayer(ResourceKey<Level> dimension, UUID playerId) {
        boolean removed = tasks.removeIf(entry ->
                entry.dimension().equals(dimension) && entry.playerId().equals(playerId));
        if (removed) setDirty();
        return removed;
    }

    

    
    public static long encodeOffset(int dx, int dy, int dz) {
        return ((long) dx & AXIS_MASK) | (((long) dy & AXIS_MASK) << AXIS_BITS)
                | (((long) dz & AXIS_MASK) << (AXIS_BITS * 2));
    }

    private static int decodeSigned(long packed, int shift) {
        int value = (int) ((packed >>> shift) & AXIS_MASK);
        
        return (value << (32 - AXIS_BITS)) >> (32 - AXIS_BITS);
    }

    
    public static BlockPos[] decodePositions(long[] packed, BlockPos center) {
        BlockPos[] result = new BlockPos[packed.length];
        for (int i = 0; i < packed.length; i++) {
            int dx = decodeSigned(packed[i], 0);
            int dy = decodeSigned(packed[i], AXIS_BITS);
            int dz = decodeSigned(packed[i], AXIS_BITS * 2);
            result[i] = center.offset(dx, dy, dz);
        }
        return result;
    }

    
    public static boolean isEncodableOffset(int dx, int dy, int dz) {
        return Math.abs(dx) <= AXIS_LIMIT && Math.abs(dy) <= AXIS_LIMIT && Math.abs(dz) <= AXIS_LIMIT;
    }

    

    private static DataResult<List<SacrificeEntry>> decodeTasks(List<StoredEntry> stored) {
        List<SacrificeEntry> result = new ArrayList<>(stored.size());
        for (StoredEntry entry : stored) {
            if (entry.dimension().isEmpty() || entry.center().isEmpty() || entry.playerId().isEmpty()) {
                epca.LOGGER.warn("Dropping incomplete sacrifice task entry in SacrificeSavedData");
                continue;
            }
            UUID playerId = parseUuid(entry.playerId().get());
            if (playerId == null) continue;
            result.add(new SacrificeEntry(entry.dimension().get(), entry.center().get(), playerId,
                    entry.positions(), Math.max(0, entry.nextIndex())));
        }
        return DataResult.success(result);
    }

    
    private static DataResult<List<StoredEntry>> encodeTasks(List<SacrificeEntry> entries) {
        List<StoredEntry> result = new ArrayList<>(entries.size());
        for (SacrificeEntry entry : entries) {
            result.add(new StoredEntry(Optional.of(entry.dimension()),
                    Optional.of(entry.center()),
                    Optional.of(entry.playerId().toString()),
                    entry.positions(), entry.nextIndex()));
        }
        return DataResult.success(result);
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            
            epca.LOGGER.warn("Invalid UUID in SacrificeSavedData: {}", value);
            return null;
        }
    }
}
