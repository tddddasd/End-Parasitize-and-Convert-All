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

/**
 * <b> SavedData</b>{@code <world>/data/nestleader_organs.dat}
 *  UUID  {@link NestLeaderOrganData}
 *
 * <h2>bug </h2>
 * <p> {@code Player#getPersistentData()}  {@code NestLeaderOrgans}
 * Forge <b></b> {@code getPersistentData()}
 * <b>{@code PlayerPersisted} </b>{@code ServerPlayer#restoreFrom}_tmp_vanilla_src
 * {@code net/minecraft/server/level/ServerPlayer.java}  11541158
 * {@code net/minecraft/world/entity/player/Player.java}  119
 * {@code PERSISTED_NBT_TAG = "PlayerPersisted"}<b></b>
 * {@code NestLeaderOrgans}
 *  {@code ForgeData} {@code Entity.java}  1660 / 1737
 *  {@code ServerPlayer}
 *  {@link #readOrCreate(Player)}  {@link NestLeaderOrganData#createDefault()}
 *
 * <b></b>
 * {@code NestLeaderSavedData}{@code nestleaders.dat} UUID </p>
 *
 * <p> UUID
 * /</p>
 *
 * <h2> DimensionDataStorage</h2>
 * <p> {@code ServerLevel}  {@code getDataStorage()} <b></b>
 * {@code data} {@code ServerLevel#getDataStorage()}_tmp_vanilla_src
 * {@code net/minecraft/server/level/ServerLevel.java}  11491150
 * {@code ServerChunkCache#getDataStorage()} {@code ServerChunkCache.java}
 *  7476  {@code LevelStorageAccess#getDimensionPath(dimension)}
 *  {@code server.overworld().getDataStorage()}
 *  {@code ServerLevel.java}  209211  {@code ServerLevel.java}  11551163
 *  {@code NestLeaderManager#getSavedData()}
 * {@code server.overworld()}/
 * </p>
 *
 * <h2>NBT </h2>
 * <p>{@code nestleader_organs.dat}</p>
 * <pre>
 * {
 *   "Players": [ { "UUID": "&lt;uuid &gt;", "Organs": { ... } }, ... ]
 * }
 * </pre>
 * <p> {@code Organs} <b></b> {@code NestLeaderOrgans}
 * {@link NestLeaderOrganData#save(CompoundTag)}  {@code Items} / {@code Unlocked}
 * / {@code HeadInnerDefaultsApplied} / {@code TorsoInnerDefaultsApplied}
 *  {@code SyncNestLeaderOrgansPacket}  {@code data.save(tag)}
 *  {@code impl/network/packet/s2c/SyncNestLeaderOrgansPacket.java}  6668 / 9095
 * <b></b></p>
 *
 * <h2> + </h2>
 * <ul>
 *   <li> UUID <b></b>
 *       {@code Player#getPersistentData().getCompound("NestLeaderOrgans")}
 *       {@link NestLeaderOrganData#PERSISTENT_KEY}<b></b>
 *       </li>
 *   <li> {@link NestLeaderOrganData#createDefault()} </li>
 *   <li><b></b></li>
 *   <li> UUID  {@link #getOrgans(UUID)} / {@link #hasOrgans(UUID)}
 *       <b></b><b></b> UUID
 *       {@link #readOrCreate(Player)}  {@code Player}
 *       </li>
 * </ul>
 * <p><b></b></p>
 * <ol>
 *   <li>
 *       {@code nestleader_organs.dat}</li>
 *   <li><b></b> {@code getPersistentData()}
 *       </li>
 *   <li>
 *        UUID
 *        {@link #readOrCreate(Player)} </li>
 * </ol>
 *
 * <h2></h2>
 * <p><b> tick </b>{@code epca#onPlayerTick}
 * {@code NestLeaderOrganEffects#tick}  {@link #readOrCreate(Player)}</p>
 * <ul>
 *   <li>{@link #read(Player)} / {@link #readOrCreate(Player)}
 *       <b> {@code setDirty()}</b></li>
 *   <li>
 *       /</li>
 *   <li>{@link #write(Player, NestLeaderOrganData)}
 *       <b></b>{@code CompoundTag#equals}_tmp_vanilla_src
 *       {@code net/minecraft/nbt/CompoundTag.java}  443449
 *       {@code ListTag}  324329  {@code ByteArrayTag}  8994
 *       <b> setDirty </b>GUI
 *       </li>
 *   <li>1.20.1  API  {@code SavedData#setDirty()}_tmp_vanilla_src
 *       {@code net/minecraft/world/level/saveddata/SavedData.java}  1727
 *        {@code markDirty()} 1.20.5+  2943
 *        {@code isDirty()}  {@code DimensionDataStorage#save()}
 *        125132  {@code ServerLevel#saveLevelData()}  759 </li>
 * </ul>
 *
 * <h2></h2>
 * <p><b></b>/<b></b>
 *  {@code Player#getPersistentData()} <b></b>
 * /</p>
 * <ul>
 *   <li> {@code NestLeaderOrganTeleport}{@code NestLeaderOrganTeleportHandler}
 *        = </li>
 *   <li> {@code NestLeaderOrganMinDamage}{@code NestLeaderOrganDamageHandler}
 *        2 </li>
 *   <li> {@code NestLeaderOrganAdaptation}{@code NestLeaderOrganAdaptation}
 *       </li>
 * </ul>
 * <p>
 *  {@link NestLeaderOrganData}
 * </p>
 */
public class NestLeaderOrganSavedData extends SavedData {

    /**
     * {@code DimensionDataStorage}  {@code .dat}
     * _tmp_vanilla_src {@code net/minecraft/world/level/storage/DimensionDataStorage.java}
     *  3436  {@code <world>/data/nestleader_organs.dat}
     *
     * <p> {@code nestleaders}{@code NestLeaderSavedData}  13
     *  {@code _organs}</p>
     */
    private static final String DATA_NAME = "nestleader_organs";

    /** {@link ListTag} */
    private static final String TAG_PLAYERS = "Players";
    /**  UUID {@code StringTag} */
    private static final String TAG_UUID = "UUID";
    /**  {@code NestLeaderOrgans}  */
    private static final String TAG_ORGANS = "Organs";

    /**
     * UUID -&gt;  {@link NestLeaderOrganData#save(CompoundTag)}
     *
     *
     * <p> {@code NestLeaderOrganData}
     *  {@code CompoundTag#equals}
     *   {@code fromNbt(...)}
     *  {@link #write(Player, NestLeaderOrganData)}
     *  {@code NestLeaderOrganActionPacket#handle}
     *  220222 </p>
     */
    private final Map<UUID, CompoundTag> organs = new HashMap<>();

    public NestLeaderOrganSavedData() {
    }

    //  SavedData  NestLeaderSavedData

    public static NestLeaderOrganSavedData fromNbt(CompoundTag tag) {
        NestLeaderOrganSavedData data = new NestLeaderOrganSavedData();
        data.load(tag);
        return data;
    }

    /**
     * <b></b>
     * {@code DimensionDataStorage#readSavedData}  _tmp_vanilla_src
     * {@code DimensionDataStorage.java}  3877
     *
     * @return  /  {@code null}
     */
    @Nullable
    public static NestLeaderOrganSavedData get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return get(server.overworld());
    }

    /**
     * <b></b>
     *  DimensionDataStorage
     */
    public static NestLeaderOrganSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                NestLeaderOrganSavedData::fromNbt,
                NestLeaderOrganSavedData::new,
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

    /**  */
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


    /**  */
    public boolean hasOrgans(UUID uuid) {
        return uuid != null && organs.containsKey(uuid);
    }

    /**  {@code null}<b></b> */
    @Nullable
    public NestLeaderOrganData getOrgans(UUID uuid) {
        CompoundTag tag = uuid == null ? null : organs.get(uuid);
        return tag == null ? null : NestLeaderOrganData.fromNbt(tag);
    }

    /**
     *
     *
     * <p><b></b>
     *  {@code setDirty()} tick
     *  GUI </p>
     */
    public void putOrgans(UUID uuid, NestLeaderOrganData data) {
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


    /**
     *
     *
     * <p></p>
     */
    public static boolean hasData(Player player) {
        if (player == null) return false;
        NestLeaderOrganSavedData store = storeFor(player);
        if (store != null && store.hasOrgans(player.getUUID())) return true;
        return legacyTag(player) != null;
    }

    /**
     *  {@code null}
     *
     * <p><b></b> +  {@link #readOrCreate(Player)}</p>
     */
    @Nullable
    public static NestLeaderOrganData read(Player player) {
        if (player == null) return null;
        NestLeaderOrganSavedData store = storeFor(player);
        if (store != null) {
            NestLeaderOrganData existing = store.getOrgans(player.getUUID());
            if (existing != null) return existing;
        }
        return readLegacy(player);
    }

    /**
     *  SPEC <b></b>
     *
     * <p></p>
     * <ol>
     *   <li>  </li>
     *   <li>  <b></b>
     *       </li>
     *   <li>  {@link NestLeaderOrganData#createDefault()} </li>
     * </ol>
     * <p>
     * {@link NestLeaderOrganData#applyHeadInnerDefaultsOnce()}
     * {@link NestLeaderOrganData#applyTorsoInnerDefaultsOnce()}</p>
     *
     * <p> null {@code player}  null</p>
     *
     * <p> {@link Player}  {@code Player}
     *  UUID </p>
     */
    public static NestLeaderOrganData readOrCreate(Player player) {
        if (player == null) {
            return NestLeaderOrganData.createDefault();
        }
        UUID uuid = player.getUUID();
        NestLeaderOrganSavedData store = storeFor(player);
        if (store == null) {
            //  +
            NestLeaderOrganData fallback = readLegacy(player);
            return fallback != null ? fallback : NestLeaderOrganData.createDefault();
        }

        NestLeaderOrganData data = store.getOrgans(uuid);
        if (data == null) {
            CompoundTag legacy = legacyTag(player);
            if (legacy != null) {
                data = NestLeaderOrganData.fromNbt(legacy);
            } else {
                //  SPEC
                data = NestLeaderOrganData.createDefault();
            }
            store.putOrgans(uuid, data);
        }

        applyHeadInnerDefaultsOnce(store, uuid, data);
        applyTorsoInnerDefaultsOnce(store, uuid, data);
        return data;
    }

    /**
     *
     *
     * <p> {@link #readOrCreate(Player)} <b></b>
     *
     *  /  / </p>
     *
     * <p> {@code NestLeaderManager#addNestLeader(UUID)}  UUID
     *  {@code ServerPlayer}</p>
     */
    public static void initDefaultsIfAbsent(Player player) {
        if (player == null) return;
        readOrCreate(player);
    }

    /**
     * <b></b>
     *
     * <p>
     * </p>
     */
    public static void write(Player player, NestLeaderOrganData data) {
        if (player == null || data == null) return;
        NestLeaderOrganSavedData store = storeFor(player);
        if (store == null) return;
        store.putOrgans(player.getUUID(), data);
    }

    //   +

    /**
     *
     *
     * <p> {@code NestLeaderManager#getSavedData()} 1621
     *  {@code ServerLifecycleHooks.getCurrentServer()} + {@code server.overworld()}
     *  {@code player.getServer()}
     *  _tmp_vanilla_src {@code Entity.java}  28172818   {@code Level#getServer()}
     *  156159  / {@code ServerLevel#getServer()}  1058 </p>
     */
    @Nullable
    private static NestLeaderOrganSavedData storeFor(@Nullable Player player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null && player != null) {
            server = player.getServer();
        }
        return server == null ? null : get(server.overworld());
    }

    /**
     * {@code Player#getPersistentData()}  {@code NestLeaderOrgans}
     * {@link NestLeaderOrganData#PERSISTENT_KEY}
     *
     * <p> {@code hasData(Player)}
     * </p>
     *
     * <p>{@code getPersistentData()}  Forge  {@code Entity}
     * _tmp_vanilla_src {@code net/minecraftforge/common/extensions/IForgeEntity.java}
     *  6873  {@code Entity.java}  34183423 </p>
     */
    @Nullable
    private static CompoundTag legacyTag(@Nullable Player player) {
        if (player == null) return null;
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(NestLeaderOrganData.PERSISTENT_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        return persistent.getCompound(NestLeaderOrganData.PERSISTENT_KEY);
    }

    @Nullable
    private static NestLeaderOrganData readLegacy(@Nullable Player player) {
        CompoundTag tag = legacyTag(player);
        return tag == null ? null : NestLeaderOrganData.fromNbt(tag);
    }


    /**
     * 33  7
     *
     * <p>{@link NestLeaderOrganData#applyHeadInnerDefaultsOnce()}
     *  false 33
     * 39 </p>
     */
    private static void applyHeadInnerDefaultsOnce(NestLeaderOrganSavedData store, UUID uuid,
                                                   NestLeaderOrganData data) {
        if (data.applyHeadInnerDefaultsOnce()) {
            store.putOrgans(uuid, data);
        }
    }

    /**
     * 39
     *
     * <p>{@link NestLeaderOrganData#applyTorsoInnerDefaultsOnce()}
     * {@code TorsoInnerDefaultsApplied} false 39
     *  33
     * </p>
     */
    private static void applyTorsoInnerDefaultsOnce(NestLeaderOrganSavedData store, UUID uuid,
                                                    NestLeaderOrganData data) {
        if (data.applyTorsoInnerDefaultsOnce()) {
            store.putOrgans(uuid, data);
        }
    }
}

