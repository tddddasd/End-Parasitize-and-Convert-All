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
 * <b> SavedData</b>{@code <world>/data/nestleader_organs.dat}
 *  UUID  {@link NestLeaderOrganData}
 *
 * <h2>bug </h2>
 * <p> {@code Player#getPersistentData()}  {@code NestLeaderOrgans}
 *  {@code getPersistentData()}
 * <b>{@code PlayerPersisted} </b>{@code ServerPlayer#restoreFrom}
 * {@code Player#PERSISTED_NBT_TAG = "PlayerPersisted"}<b></b>
 * {@code NestLeaderOrgans}
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
 * {@code data}  {@code server.overworld().getDataStorage()}
 * {@code NestLeaderManager#getSavedData()}
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
 *        {@code NestLeaderSavedData}  {@code UUID_STRING_CODEC}
 *        {@code UUIDUtil.CODEC}  int </li>
 *   <li>{@code UUID#fromString}  codec <b></b>
 *       {@code comapFlatMap}/{@code DataResult.error}
 *        {@code load()}  try/catch </li>
 * </ul>
 * <p> {@code Organs}
 * {@link NestLeaderOrganData#save(CompoundTag)}  {@code Items} / {@code Unlocked}
 * / {@code HeadInnerDefaultsApplied} / {@code TorsoInnerDefaultsApplied}
 * {@code SyncNestLeaderOrgansPacket} <b></b></p>
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
 * <p><b></b><b></b>
 *  {@code getPersistentData()}
 *  UUID </p>
 *
 * <h2></h2>
 * <p><b> tick </b> tick  {@code NestLeaderOrganEffects#tick}
 * {@link #readOrCreate(Player)}</p>
 * <ul>
 *   <li>{@link #read(Player)} / {@link #readOrCreate(Player)}
 *       <b> {@code setDirty()}</b></li>
 *   <li>
 *       /</li>
 *   <li>{@link #putOrgans(UUID, NestLeaderOrganData)}
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
 *   <li> {@code NestLeaderOrganTeleport}{@code NestLeaderOrganTeleportHandler}</li>
 *   <li> {@code NestLeaderOrganMinDamage}{@code NestLeaderOrganDamageHandler}</li>
 *   <li> {@code NestLeaderOrganAdaptation}</li>
 * </ul>
 * <p>
 *  {@link NestLeaderOrganData}
 * </p>
 */
public class NestLeaderOrganSavedData extends SavedData {

    /**
     * {@code DimensionDataStorage}  {@code .dat}
     *  {@code <world>/data/nestleader_organs.dat}
     *
     * <p> {@code nestleaders}  {@code _organs}
     * </p>
     */
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "nestleader_organs");

    /** {@code ListTag} */
    private static final String TAG_PLAYERS = "Players";
    /**  UUID {@code StringTag} */
    private static final String TAG_UUID = "UUID";
    /**  {@code NestLeaderOrgans}  */
    private static final String TAG_ORGANS = "Organs";

    /**
     * UUID  codec {@code NestLeaderSavedData#UUID_STRING_CODEC}
     *  {@code nestleader_organs.dat}  UUID <b></b>
     */
    private static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.comapFlatMap(
            NestLeaderOrganSavedData::parseUuid,
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
    public static final SavedDataType<NestLeaderOrganSavedData> TYPE =
            new SavedDataType<NestLeaderOrganSavedData>(DATA_ID, NestLeaderOrganSavedData::new, Organs.CODEC.xmap(
                    root -> {
                        NestLeaderOrganSavedData data = new NestLeaderOrganSavedData();
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
     * UUID -&gt;  {@link NestLeaderOrganData#save(CompoundTag)}
     *
     *
     * <p> {@code NestLeaderOrganData}
     *  {@code CompoundTag#equals}
     *   {@code fromNbt(...)}
     *  {@link #write(Player, NestLeaderOrganData)} </p>
     */
    private final Map<UUID, CompoundTag> organs = new HashMap<>();

    public NestLeaderOrganSavedData() {
    }

    //  SavedData  NestLeaderSavedData

    /**
     * <b></b> {@code DimensionDataStorage}
     *
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
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static com.mojang.serialization.DataResult<UUID> parseUuid(String value) {
        try {
            return com.mojang.serialization.DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            //  decode
            return com.mojang.serialization.DataResult.error(
                    () -> "Invalid UUID in NestLeaderOrganSavedData: " + value);
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
     * <p> {@code NestLeaderManager#getSavedData()}
     * {@code ServerLifecycleHooks.getCurrentServer()} + {@code server.overworld()}
     *  {@code player.getServer()} </p>
     *
     * <p>26.1.2  {@code ServerLifecycleHooks}  {@code net.neoforged.neoforge.server}
     * 1.20.1  {@code net.minecraftforge.server.ServerLifecycleHooks}</p>
     */
    @Nullable
    private static NestLeaderOrganSavedData storeFor(@Nullable Player player) {
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
     * {@code Player#getPersistentData()}  {@code NestLeaderOrgans}
     * {@link NestLeaderOrganData#PERSISTENT_KEY}
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
        if (!persistent.contains(NestLeaderOrganData.PERSISTENT_KEY)) {
            return null;
        }
        return persistent.getCompoundOrEmpty(NestLeaderOrganData.PERSISTENT_KEY);
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

