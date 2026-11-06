package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HiveLeaderOrganSavedData extends SavedData {

    private static final String DATA_NAME = "hiveleader_organs";

    private static final String TAG_PLAYERS = "Players";

    private static final String TAG_UUID = "UUID";

    private static final String TAG_ORGANS = "Organs";

    private final Map<UUID, CompoundTag> organs = new HashMap<>();

    public HiveLeaderOrganSavedData() {
    }

    public static HiveLeaderOrganSavedData fromNbt(CompoundTag tag) {
        HiveLeaderOrganSavedData data = new HiveLeaderOrganSavedData();
        data.load(tag);
        return data;
    }

    @Nullable
    public static HiveLeaderOrganSavedData get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return get(server.overworld());
    }

    public static HiveLeaderOrganSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                HiveLeaderOrganSavedData::fromNbt,
                HiveLeaderOrganSavedData::new,
                DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, CompoundTag> entry : organs.entrySet()) {
            CompoundTag one = new CompoundTag();
            one.putString(TAG_UUID, entry.getKey().toString());
            one.put(TAG_ORGANS, entry.getValue().copy());
            list.add(one);
        }
        tag.put(TAG_PLAYERS, list);
        return tag;
    }

    public void load(CompoundTag tag) {
        organs.clear();
        ListTag list = tag.getList(TAG_PLAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag one = list.getCompound(i);
            UUID uuid = parseUuid(one.getString(TAG_UUID));
            if (uuid == null) continue;
            if (!one.contains(TAG_ORGANS, Tag.TAG_COMPOUND)) continue;
            organs.put(uuid, one.getCompound(TAG_ORGANS).copy());
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

    @Nullable
    private static UUID parseUuid(@Nullable String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
            server = player.getServer();
        }
        return server == null ? null : get(server.overworld());
    }

    @Nullable
    private static CompoundTag legacyTag(@Nullable Player player) {
        if (player == null) return null;
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(HiveLeaderOrganData.PERSISTENT_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        return persistent.getCompound(HiveLeaderOrganData.PERSISTENT_KEY);
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
