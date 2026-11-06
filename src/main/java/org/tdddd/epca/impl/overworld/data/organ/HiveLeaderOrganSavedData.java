package org.tdddd.epca.impl.overworld.data.organ;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HiveLeaderOrganSavedData extends SavedData {

    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "hiveleader_organs");

    private static final String TAG_PLAYERS = "Players";

    private static final String TAG_UUID = "UUID";

    private static final String TAG_ORGANS = "Organs";

    private static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.comapFlatMap(
            HiveLeaderOrganSavedData::parseUuid,
            UUID::toString);

    private record PlayerOrgans(UUID uuid, CompoundTag organs) {
        static final Codec<PlayerOrgans> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUID_STRING_CODEC.fieldOf(TAG_UUID).forGetter(PlayerOrgans::uuid),
                CompoundTag.CODEC.fieldOf(TAG_ORGANS).forGetter(PlayerOrgans::organs)
        ).apply(i, PlayerOrgans::new));
    }

    private record Organs(List<PlayerOrgans> players) {
        static final Codec<Organs> CODEC = RecordCodecBuilder.create(i -> i.group(
                PlayerOrgans.CODEC.listOf().optionalFieldOf(TAG_PLAYERS, List.of())
                        .forGetter(Organs::players)
        ).apply(i, Organs::new));
    }

    public static final SavedDataType<HiveLeaderOrganSavedData> TYPE =
            new SavedDataType<HiveLeaderOrganSavedData>(DATA_ID, HiveLeaderOrganSavedData::new, Organs.CODEC.xmap(
                    root -> {
                        HiveLeaderOrganSavedData data = new HiveLeaderOrganSavedData();
                        for (PlayerOrgans entry : root.players()) {
                            data.organs.put(entry.uuid(), entry.organs().copy());
                        }
                        return data;
                    },
                    data -> {
                        List<PlayerOrgans> players = new ArrayList<>(data.organs.size());
                        for (Map.Entry<UUID, CompoundTag> entry : data.organs.entrySet()) {
                            players.add(new PlayerOrgans(entry.getKey(), entry.getValue().copy()));
                        }
                        return new Organs(players);
                    }));

    private final Map<UUID, CompoundTag> organs = new HashMap<>();

    public HiveLeaderOrganSavedData() {
    }

    @Nullable
    public static HiveLeaderOrganSavedData get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return get(server.overworld());
    }

    public static HiveLeaderOrganSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static com.mojang.serialization.DataResult<UUID> parseUuid(String value) {
        try {
            return com.mojang.serialization.DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException e) {

            return com.mojang.serialization.DataResult.error(
                    () -> "Invalid UUID in HiveLeaderOrganSavedData: " + value);
        }
    }

    public boolean hasOrgans(UUID uuid) {
        return uuid != null && organs.containsKey(uuid);
    }

    @Nullable
    public HiveLeaderOrganData getOrgans(UUID uuid) {
        CompoundTag tag = uuid == null ? null : organs.get(uuid);
        return tag == null ? null : HiveLeaderOrganData.fromNbt(tag);
    }

    public void putOrgans(UUID uuid, HiveLeaderOrganData data) {
        if (uuid == null || data == null) return;
        CompoundTag fresh = new CompoundTag();
        data.save(fresh);
        CompoundTag old = organs.get(uuid);
        if (fresh.equals(old)) {
            return;
        }
        organs.put(uuid, fresh);
        setDirty();
    }

    public static boolean hasData(Player player) {
        if (player == null) return false;
        HiveLeaderOrganSavedData store = storeFor(player);
        if (store != null && store.hasOrgans(player.getUUID())) return true;
        return legacyTag(player) != null;
    }

    @Nullable
    public static HiveLeaderOrganData read(Player player) {
        if (player == null) return null;
        HiveLeaderOrganSavedData store = storeFor(player);
        if (store != null) {
            HiveLeaderOrganData existing = store.getOrgans(player.getUUID());
            if (existing != null) return existing;
        }
        return readLegacy(player);
    }

    public static HiveLeaderOrganData readOrCreate(Player player) {
        if (player == null) {
            return HiveLeaderOrganData.createDefault();
        }
        UUID uuid = player.getUUID();
        HiveLeaderOrganSavedData store = storeFor(player);
        if (store == null) {

            HiveLeaderOrganData fallback = readLegacy(player);
            return fallback != null ? fallback : HiveLeaderOrganData.createDefault();
        }

        HiveLeaderOrganData data = store.getOrgans(uuid);
        if (data == null) {
            CompoundTag legacy = legacyTag(player);
            if (legacy != null) {
                data = HiveLeaderOrganData.fromNbt(legacy);
            } else {

                data = HiveLeaderOrganData.createDefault();
            }
            store.putOrgans(uuid, data);
        }

        applyHeadInnerDefaultsOnce(store, uuid, data);
        applyTorsoInnerDefaultsOnce(store, uuid, data);
        return data;
    }

    public static void initDefaultsIfAbsent(Player player) {
        if (player == null) return;
        readOrCreate(player);
    }

    public static void write(Player player, HiveLeaderOrganData data) {
        if (player == null || data == null) return;
        HiveLeaderOrganSavedData store = storeFor(player);
        if (store == null) return;
        store.putOrgans(player.getUUID(), data);
    }

    @Nullable
    private static HiveLeaderOrganSavedData storeFor(@Nullable Player player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null && player != null) {

            server = player.level().getServer();
        }
        return server == null ? null : get(server.overworld());
    }

    @Nullable
    private static CompoundTag legacyTag(@Nullable Player player) {
        if (player == null) return null;
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(HiveLeaderOrganData.PERSISTENT_KEY)) {
            return null;
        }
        return persistent.getCompoundOrEmpty(HiveLeaderOrganData.PERSISTENT_KEY);
    }

    @Nullable
    private static HiveLeaderOrganData readLegacy(@Nullable Player player) {
        CompoundTag tag = legacyTag(player);
        return tag == null ? null : HiveLeaderOrganData.fromNbt(tag);
    }

    private static void applyHeadInnerDefaultsOnce(HiveLeaderOrganSavedData store, UUID uuid,
                                                   HiveLeaderOrganData data) {
        if (data.applyHeadInnerDefaultsOnce()) {
            store.putOrgans(uuid, data);
        }
    }

    private static void applyTorsoInnerDefaultsOnce(HiveLeaderOrganSavedData store, UUID uuid,
                                                    HiveLeaderOrganData data) {
        if (data.applyTorsoInnerDefaultsOnce()) {
            store.putOrgans(uuid, data);
        }
    }
}
