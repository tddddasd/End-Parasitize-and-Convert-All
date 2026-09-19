package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NestLeaderSavedData extends SavedData {
    /** 1.20.1 stored this as "nestleaders"; it stays a valid Identifier path. */
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "nestleaders");

    /** Legacy shape was a list of UUID strings (StringTag.valueOf(uuid.toString())). */
    private static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.comapFlatMap(
            NestLeaderSavedData::parseUuid,
            UUID::toString);

    /** Legacy field name "Leaders" kept, so an old data compound can be moved under {"data": ...} unchanged. */
    public static final Codec<NestLeaderSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUID_STRING_CODEC.listOf().optionalFieldOf("Leaders", List.of())
                    .forGetter(NestLeaderSavedData::leaderList)
    ).apply(i, NestLeaderSavedData::new));

    public static final SavedDataType<NestLeaderSavedData> TYPE =
            new SavedDataType<NestLeaderSavedData>(DATA_ID, NestLeaderSavedData::new, CODEC);

    private final Set<UUID> leaders = new HashSet<>();

    public NestLeaderSavedData() {}

    /** Codec decode constructor. */
    private NestLeaderSavedData(List<UUID> leaderIds) {
        leaders.addAll(leaderIds);
    }

    private List<UUID> leaderList() {
        return new ArrayList<>(leaders);
    }

    private static DataResult<UUID> parseUuid(String value) {
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            // A malformed entry now fails the decode (the data is dropped with a log line) instead of
            // throwing out of the old load(CompoundTag), which aborted loading the whole storage.
            return DataResult.error(() -> "Invalid UUID in NestLeaderSavedData: " + value);
        }
    }

    public static NestLeaderSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void addLeader(UUID uuid) {
        leaders.add(uuid);
        setDirty();
    }

    public void removeLeader(UUID uuid) {
        leaders.remove(uuid);
        setDirty();
    }

    public boolean isLeader(UUID uuid) {
        return leaders.contains(uuid);
    }

    public Set<UUID> getLeaders() {
        return new HashSet<>(leaders);
    }
}
