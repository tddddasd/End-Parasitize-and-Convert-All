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

public class HiveLeaderSavedData extends SavedData {

    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "hiveleaders");

    private static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.comapFlatMap(
            HiveLeaderSavedData::parseUuid,
            UUID::toString);

    public static final Codec<HiveLeaderSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUID_STRING_CODEC.listOf().optionalFieldOf("Leaders", List.of())
                    .forGetter(HiveLeaderSavedData::leaderList)
    ).apply(i, HiveLeaderSavedData::new));

    public static final SavedDataType<HiveLeaderSavedData> TYPE =
            new SavedDataType<HiveLeaderSavedData>(DATA_ID, HiveLeaderSavedData::new, CODEC);

    private final Set<UUID> leaders = new HashSet<>();

    public HiveLeaderSavedData() {}

    private HiveLeaderSavedData(List<UUID> leaderIds) {
        leaders.addAll(leaderIds);
    }

    private List<UUID> leaderList() {
        return new ArrayList<>(leaders);
    }

    private static DataResult<UUID> parseUuid(String value) {
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException e) {

            return DataResult.error(() -> "Invalid UUID in HiveLeaderSavedData: " + value);
        }
    }

    public static HiveLeaderSavedData get(ServerLevel level) {
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
