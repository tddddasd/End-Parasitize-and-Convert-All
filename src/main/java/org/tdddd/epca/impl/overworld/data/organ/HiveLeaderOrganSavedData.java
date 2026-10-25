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

/**
 * <b> SavedData</b>{@code <world>/data/hiveleader_organs.dat}
 *  UUID  {@link HiveLeaderOrganData}
 *
 * <h2>bug </h2>
 * <p> {@code Player#getPersistentData()}  {@code HiveLeaderOrgans}
 *  {@code getPersistentData()}
 * <b>{@code PlayerPersisted} </b>{@code ServerPlayer#restoreFrom}
 * {@code Player#PERSISTED_NBT_TAG = "PlayerPersisted"}<b></b>
 * {@code HiveLeaderOrgans}
 *  {@code ServerPlayer}
 *  {@link #readOrCreate(Player)}  {@link HiveLeaderOrganData#createDefault()}
 *
 * <b></b>
 * {@code HiveLeaderSavedData}{@code hiveleaders.dat} UUID </p>
 *
 * <p> UUID
 * /</p>
 *
 * <h2> DimensionDataStorage</h2>
 * <p> {@code ServerLevel}  {@code getDataStorage()} <b></b>
 * {@code data}  {@code server.overworld().getDataStorage()}
 * {@code HiveLeaderManager#getSavedData()}
 * {@code server.overworld()}/
 * </p>
 *
 * <h2>1.20.1 -&gt; 26.1.2  API </h2>
 * <p>26.1.2  {@code SavedData} {@code load(CompoundTag)} + {@code save(CompoundTag)}
 * <b> {@link Codec} +  {@link SavedDataType}</b>
 * {@code level.getDataStorage().computeIfAbsent(TYPE)} / codec
 *  NBT <b></b> codec</p>
 * <ul>
 *   <li> {@code {"Players": [ {"UUID": "...", "Organs": {...}}, ... ]}}
 *        {@code record}{@link PlayerOrgans} / {@link Organs}
 *       {@code CompoundTag.CODEC} <b></b>passthroughcodec {@code Organs}
 *       <b></b></li>
 *   <li>{@code UUID} <b></b>{@code Codec.STRING.xmap(UUID::fromString, UUID::toString)}
 *        {@code HiveLeaderSavedData}  {@code UUID_STRING_CODEC}
 *        {@code UUIDUtil.CODEC}  int </li>
 *   <li>{@code UUID#fromString}  codec <b></b>
 *       {@code comapFlatMap}/{@code DataResult.error}
 *        {@code load()}  try/catch </li>
 * </ul>
 * <p> {@code Organs}
 * {@link HiveLeaderOrganData#save(CompoundTag)}  {@code Items} / {@code Unlocked}
 * / {@code HeadInnerDefaultsApplied} / {@code TorsoInnerDefaultsApplied}
 * {@code SyncHiveLeaderOrgansPacket} <b></b></p>
 *
 * <h2> + </h2>
 * <ul>
 *   <li> UUID <b></b>
 *       {@code Player#getPersistentData().getCompound("HiveLeaderOrgans")}
 *       {@link HiveLeaderOrganData#PERSISTENT_KEY}<b></b>
 *       </li>
 *   <li> {@link HiveLeaderOrganData#createDefault()} </li>
 *   <li><b></b></li>
 *   <li> UUID  {@link #getOrgans(UUID)} / {@link #hasOrgans(UUID)}
 *       <b></b><b></b> UUID
 *       {@link #readOrCreate(Player)}  {@code Player}
 *       </li>
 * </ul>
 * <p><b></b><b></b>
 *  {@code getPersistentData()}
 *  UUID </p>
 *
 * <h2></h2>
 * <p><b> tick </b> tick  {@code HiveLeaderOrganEffects#tick}
 * {@link #readOrCreate(Player)}</p>
 * <ul>
 *   <li>{@link #read(Player)} / {@link #readOrCreate(Player)}
 *       <b> {@code setDirty()}</b></li>
 *   <li>
 *       /</li>
 *   <li>{@link #putOrgans(UUID, HiveLeaderOrganData)}
 *       <b></b>{@code CompoundTag#equals} {@code ListTag}
 *       {@code ByteArrayTag}<b> setDirty </b>GUI
 *       </li>
 *   <li>26.1.2  {@code SavedData#setDirty()}
 *       {@code DimensionDataStorage#save()} {@code ServerLevel#saveLevelData()} </li>
 * </ul>
 *
 * <h2></h2>
 * <p><b></b>/<b></b>
 *  {@code Player#getPersistentData()} <b></b></p>
 * <ul>
 *   <li> {@code HiveLeaderOrganTeleport}{@code HiveLeaderOrganTeleportHandler}</li>
 *   <li> {@code HiveLeaderOrganMinDamage}{@code HiveLeaderOrganDamageHandler}</li>
 *   <li> {@code HiveLeaderOrganAdaptation}</li>
 * </ul>
 * <p>
 *  {@link HiveLeaderOrganData}
 * </p>
 */
public class HiveLeaderOrganSavedData extends SavedData {

    /**
     * {@code DimensionDataStorage}  {@code .dat}
     *  {@code <world>/data/hiveleader_organs.dat}
     *
     * <p> {@code hiveleaders}  {@code _organs}
     * </p>
     */
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "hiveleader_organs");

    /** {@code ListTag} */
    private static final String TAG_PLAYERS = "Players";
    /**  UUID {@code StringTag} */
    private static final String TAG_UUID = "UUID";
    /**  {@code HiveLeaderOrgans}  */
    private static final String TAG_ORGANS = "Organs";

    /**
     * UUID  codec {@code HiveLeaderSavedData#UUID_STRING_CODEC}
     *  {@code hiveleader_organs.dat}  UUID <b></b>
     */
    private static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.comapFlatMap(
            HiveLeaderOrganSavedData::parseUuid,
            UUID::toString);

    /** {@code {"UUID": "...", "Organs": {...}}} NBT  */
    private record PlayerOrgans(UUID uuid, CompoundTag organs) {
        static final Codec<PlayerOrgans> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUID_STRING_CODEC.fieldOf(TAG_UUID).forGetter(PlayerOrgans::uuid),
                CompoundTag.CODEC.fieldOf(TAG_ORGANS).forGetter(PlayerOrgans::organs)
        ).apply(i, PlayerOrgans::new));
    }

    /** {@code {"Players": [...]}} NBT  */
    private record Organs(List<PlayerOrgans> players) {
        static final Codec<Organs> CODEC = RecordCodecBuilder.create(i -> i.group(
                PlayerOrgans.CODEC.listOf().optionalFieldOf(TAG_PLAYERS, List.of())
                        .forGetter(Organs::players)
        ).apply(i, Organs::new));
    }

    /**
     *  SavedData  codec /
     *
     * <p>{@link SavedDataType}  codec
     * {@code level.getDataStorage().computeIfAbsent(TYPE)} </p>
     */
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

    /**
     * UUID -&gt;  {@link HiveLeaderOrganData#save(CompoundTag)}
     *
     *
     * <p> {@code HiveLeaderOrganData}
     *  {@code CompoundTag#equals}
     *   {@code fromNbt(...)}
     *  {@link #write(Player, HiveLeaderOrganData)} </p>
     */
    private final Map<UUID, CompoundTag> organs = new HashMap<>();

    public HiveLeaderOrganSavedData() {
    }

    //  SavedData  HiveLeaderSavedData

    /**
     * <b></b> {@code DimensionDataStorage}
     *
     *
     * @return  /  {@code null}
     */
    @Nullable
    public static HiveLeaderOrganSavedData get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return get(server.overworld());
    }

    /**
     * <b></b>
     *  DimensionDataStorage
     */
    public static HiveLeaderOrganSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static com.mojang.serialization.DataResult<UUID> parseUuid(String value) {
        try {
            return com.mojang.serialization.DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            //  decode
            return com.mojang.serialization.DataResult.error(
                    () -> "Invalid UUID in HiveLeaderOrganSavedData: " + value);
        }
    }


    /**  */
    public boolean hasOrgans(UUID uuid) {
        return uuid != null && organs.containsKey(uuid);
    }

    /**  {@code null}<b></b> */
    @Nullable
    public HiveLeaderOrganData getOrgans(UUID uuid) {
        CompoundTag tag = uuid == null ? null : organs.get(uuid);
        return tag == null ? null : HiveLeaderOrganData.fromNbt(tag);
    }

    /**
     *
     *
     * <p><b></b>
     *  {@code setDirty()} tick
     *  GUI </p>
     */
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


    /**
     *
     *
     * <p></p>
     */
    public static boolean hasData(Player player) {
        if (player == null) return false;
        HiveLeaderOrganSavedData store = storeFor(player);
        if (store != null && store.hasOrgans(player.getUUID())) return true;
        return legacyTag(player) != null;
    }

    /**
     *  {@code null}
     *
     * <p><b></b> +  {@link #readOrCreate(Player)}</p>
     */
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

    /**
     *  SPEC <b></b>
     *
     * <p></p>
     * <ol>
     *   <li>  </li>
     *   <li>  <b></b>
     *       </li>
     *   <li>  {@link HiveLeaderOrganData#createDefault()} </li>
     * </ol>
     * <p>
     * {@link HiveLeaderOrganData#applyHeadInnerDefaultsOnce()}
     * {@link HiveLeaderOrganData#applyTorsoInnerDefaultsOnce()}</p>
     *
     * <p> null {@code player}  null</p>
     *
     * <p> {@link Player}  {@code Player}
     *  UUID </p>
     */
    public static HiveLeaderOrganData readOrCreate(Player player) {
        if (player == null) {
            return HiveLeaderOrganData.createDefault();
        }
        UUID uuid = player.getUUID();
        HiveLeaderOrganSavedData store = storeFor(player);
        if (store == null) {
            //  +
            HiveLeaderOrganData fallback = readLegacy(player);
            return fallback != null ? fallback : HiveLeaderOrganData.createDefault();
        }

        HiveLeaderOrganData data = store.getOrgans(uuid);
        if (data == null) {
            CompoundTag legacy = legacyTag(player);
            if (legacy != null) {
                data = HiveLeaderOrganData.fromNbt(legacy);
            } else {
                //  SPEC
                data = HiveLeaderOrganData.createDefault();
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
     * <p> {@code HiveLeaderManager#addHiveLeader(UUID)}  UUID
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
    public static void write(Player player, HiveLeaderOrganData data) {
        if (player == null || data == null) return;
        HiveLeaderOrganSavedData store = storeFor(player);
        if (store == null) return;
        store.putOrgans(player.getUUID(), data);
    }

    //   +

    /**
     *
     *
     * <p> {@code HiveLeaderManager#getSavedData()}
     * {@code ServerLifecycleHooks.getCurrentServer()} + {@code server.overworld()}
     *  {@code player.getServer()} </p>
     *
     * <p>26.1.2  {@code ServerLifecycleHooks}  {@code net.neoforged.neoforge.server}
     * 1.20.1  {@code net.minecraftforge.server.ServerLifecycleHooks}</p>
     */
    @Nullable
    private static HiveLeaderOrganSavedData storeFor(@Nullable Player player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null && player != null) {
            // 26.1.2: Entity#getServer() was deleted.  The surviving entry point
            // is Level#getServer() -> @Nullable MinecraftServer (_tmp_26src
            // net/minecraft/world/level/Level.java:168; ServerLevel.java:1321 overrides
            // it as non-null).  Same semantics as 1.20.1 player.getServer().
            server = player.level().getServer();
        }
        return server == null ? null : get(server.overworld());
    }

    /**
     * {@code Player#getPersistentData()}  {@code HiveLeaderOrgans}
     * {@link HiveLeaderOrganData#PERSISTENT_KEY}
     *
     * <p> {@code hasData(Player)}
     * </p>
     *
     * <p>26.1.2{@code CompoundTag#getCompound(String)}  {@code Optional<CompoundTag>}
     * _tmp_26src {@code CompoundTag.java}  375  {@code getCompoundOrEmpty}
     *  379  {@code getCompound(...).orElse(null)}</p>
     */
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


    /**
     * 33  7
     *
     * <p>{@link HiveLeaderOrganData#applyHeadInnerDefaultsOnce()}
     *  false 33
     * 39 </p>
     */
    private static void applyHeadInnerDefaultsOnce(HiveLeaderOrganSavedData store, UUID uuid,
                                                   HiveLeaderOrganData data) {
        if (data.applyHeadInnerDefaultsOnce()) {
            store.putOrgans(uuid, data);
        }
    }

    /**
     * 39
     *
     * <p>{@link HiveLeaderOrganData#applyTorsoInnerDefaultsOnce()}
     * {@code TorsoInnerDefaultsApplied} false 39
     *  33
     * </p>
     */
    private static void applyTorsoInnerDefaultsOnce(HiveLeaderOrganSavedData store, UUID uuid,
                                                    HiveLeaderOrganData data) {
        if (data.applyTorsoInnerDefaultsOnce()) {
            store.putOrgans(uuid, data);
        }
    }
}

