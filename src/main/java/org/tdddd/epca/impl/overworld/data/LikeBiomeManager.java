package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomesPacket;
import org.tdddd.epca.impl.overworld.registry.ParasiteBiome;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Server side driver of the like biome (类群系) system.
 *
 * <p>A like biome lives on a <b>4x4x4</b> block unit. Units are generated/removed purely from
 * the blocks inside them:</p>
 * <ul>
 *     <li>unit contains at least one block implementing {@link InfestedBlockInterface}
 *     &rarr; the unit gets the EPCA parasite like biome;</li>
 *     <li>unit contains no such block anymore &rarr; the (auto generated) like biome is removed.</li>
 * </ul>
 *
 * <p>Network updates never go out per unit: {@link LikeBiomeSync} aggregates every change of a
 * tick and flushes one (or a few) compact batch packets at the end of that tick.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LikeBiomeManager {

    /** log2 of the unit edge length: a unit is 4x4x4 blocks. */
    public static final int UNIT_SHIFT = 2;

    /** Edge length of a like biome unit in blocks. */
    public static final int UNIT_SIZE = 1 << UNIT_SHIFT;

    /** Number of blocks on one axis that share the same unit. */
    public static final int UNIT_MASK = UNIT_SIZE - 1;

    /** Blocks per unit volume. */
    public static final int UNIT_VOLUME = UNIT_SIZE * UNIT_SIZE * UNIT_SIZE;

    private static final int SWEEP_INTERVAL_TICKS = 10;
    private static final int SWEEP_BATCH = 64;

    private static final int UNIT_COORD_BITS = 21;
    private static final int UNIT_COORD_MASK = (1 << UNIT_COORD_BITS) - 1;

    private static final Map<ServerLevel, int[]> SWEEP_CURSOR = new IdentityHashMap<>();

    /** Units whose infestation state must be re-checked after the world changed this tick. */
    private static final Map<ServerLevel, java.util.Set<Long>> PENDING_REFRESH = new IdentityHashMap<>();

    /**
     * Wall clock budget of the reconciler per server tick. The sweep used to spend a fixed budget of
     * units, which is fine for a handful of units but turns into thousands of block reads on a big
     * world; a time budget keeps the tick cost flat (= invisible) no matter how many units exist.
     */
    private static final long SWEEP_BUDGET_NANOS = 1_500_000L;

    /**
     * Per level cache of {@code ResourceLocation -> ResourceKey<Biome>} resolutions. The parasite
     * biome is looked up on <b>every</b> infested block placement (through the
     * {@code LevelChunk#setBlockState} hook), so the registry lookup and the {@code ResourceKey}
     * allocation have to happen once, not once per block.
     */
    private static final Map<ServerLevel, Map<ResourceLocation, java.util.Optional<ResourceKey<Biome>>>>
            RESOLVE_CACHE = new IdentityHashMap<>();

    /** Levels whose parasite like biome was already verified to exist. */
    private static final Map<ServerLevel, Boolean> PARASITE_VERIFIED = new IdentityHashMap<>();

    /** The auto generated like biome value, immutable and therefore shared by every unit. */
    private static final LikeBiome PARASITE_LIKE_BIOME =
            new LikeBiome(ParasiteBiome.PARASITE_BIOME.location(), true, true, LikeBiome.SPAWN_NONE,
                    LikeBiome.STAGE_INHERIT);

    private static final ResourceKey<Biome> PARASITE_KEY =
            ResourceKey.create(Registries.BIOME, ParasiteBiome.PARASITE_BIOME.location());

    private LikeBiomeManager() {
    }

    // ------------------------------------------------------------------
    // unit <-> block coordinate helpers (single source of truth for the size)
    // ------------------------------------------------------------------

    /** Block coordinate -> unit coordinate (floor division by {@link #UNIT_SIZE}). */
    public static int unitCoord(int blockCoord) {
        return blockCoord >> UNIT_SHIFT;
    }

    /** Unit coordinate -> block coordinate of the first block of that unit. */
    public static int unitStart(int unitCoord) {
        return unitCoord << UNIT_SHIFT;
    }

    /** Packs unit coordinates into a single key. */
    public static long unitKey(int unitX, int unitY, int unitZ) {
        return ((long) (unitX & UNIT_COORD_MASK) << (UNIT_COORD_BITS * 2))
                | ((long) (unitY & UNIT_COORD_MASK) << UNIT_COORD_BITS)
                | (unitZ & UNIT_COORD_MASK);
    }

    public static long unitKey(BlockPos pos) {
        return unitKey(unitCoord(pos.getX()), unitCoord(pos.getY()), unitCoord(pos.getZ()));
    }

    /** Packs block coordinates into the key of the unit that contains them. */
    public static long unitKeyOfBlock(int blockX, int blockY, int blockZ) {
        return unitKey(unitCoord(blockX), unitCoord(blockY), unitCoord(blockZ));
    }

    public static int unitX(long key) {
        return (int) (key << (64 - UNIT_COORD_BITS * 3) >> (64 - UNIT_COORD_BITS));
    }

    public static int unitY(long key) {
        return (int) (key << (64 - UNIT_COORD_BITS * 2) >> (64 - UNIT_COORD_BITS));
    }

    public static int unitZ(long key) {
        return (int) (key << (64 - UNIT_COORD_BITS) >> (64 - UNIT_COORD_BITS));
    }

    /** Block position of the minimum corner of a unit. */
    public static BlockPos unitOrigin(long key) {
        return new BlockPos(unitStart(unitX(key)), unitStart(unitY(key)), unitStart(unitZ(key)));
    }

    /** Section key (16x16x16) that contains the given unit, used for client re-render hints. */
    public static long sectionKeyOfUnit(long unitKey) {
        // SectionPos.asLong(int, int, int) expects section coordinates, so shift the block coords.
        return net.minecraft.core.SectionPos.asLong(
                unitStart(unitX(unitKey)) >> 4,
                unitStart(unitY(unitKey)) >> 4,
                unitStart(unitZ(unitKey)) >> 4);
    }

    // ------------------------------------------------------------------
    // registry helpers
    // ------------------------------------------------------------------

    @Nullable
    public static ResourceKey<Biome> resolve(ServerLevel level, ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Map<ResourceLocation, java.util.Optional<ResourceKey<Biome>>> cache = RESOLVE_CACHE.get(level);
        if (cache == null) {
            cache = new java.util.HashMap<>();
            RESOLVE_CACHE.put(level, cache);
        }
        java.util.Optional<ResourceKey<Biome>> cached = cache.get(id);
        if (cached == null) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
            cached = level.registryAccess().registryOrThrow(Registries.BIOME).containsKey(key)
                    ? java.util.Optional.of(key)
                    : java.util.Optional.empty();
            // a server wide biome registry never changes after load, so a negative answer is final
            cache.put(id, cached);
        }
        return cached.orElse(null);
    }

    /** Cached "does the parasite like biome exist in this level" answer. */
    private static boolean parasiteBiomeAvailable(ServerLevel level) {
        Boolean verified = PARASITE_VERIFIED.get(level);
        if (verified != null) {
            return verified;
        }
        boolean present = level.registryAccess().registryOrThrow(Registries.BIOME)
                .containsKey(PARASITE_KEY);
        PARASITE_VERIFIED.put(level, present);
        return present;
    }

    /** Current erosion (evolution) stage of a dimension. */
    public static int dimensionStage(Level level) {
        return EvolutionManager.getStageForDimension(level);
    }

    /** The like biome that is generated for units containing infested blocks. */
    public static LikeBiome parasiteLikeBiome() {
        return PARASITE_LIKE_BIOME;
    }

    @Nullable
    public static LikeBiome activeAt(ServerLevel level, BlockPos pos) {
        LikeBiome stored = LikeBiomeSavedData.get(level).at(pos);
        if (stored == null || resolve(level, stored.biomeId()) == null) {
            return null;
        }
        return stored;
    }

    @Nullable
    public static LikeBiome atUnit(ServerLevel level, long unitKey) {
        return LikeBiomeSavedData.get(level).atKey(unitKey);
    }

    // ------------------------------------------------------------------
    // generation / removal
    // ------------------------------------------------------------------

    /**
     * Called when a block that implements {@link InfestedBlockInterface} was placed or created
     * inside {@code pos}: the unit must carry the parasite like biome. No scanning is needed, the
     * block itself already proves the unit is infested.
     */
    public static void markUnitInfested(ServerLevel level, BlockPos pos) {
        ensureUnit(level, unitKey(pos));
    }

    /** Creates the auto generated like biome for a unit when it is not present yet. */
    public static void ensureUnit(ServerLevel level, long unitKey) {
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);
        LikeBiome existing = data.atKey(unitKey);
        if (existing != null) {
            return;
        }
        if (!parasiteBiomeAvailable(level)) {
            return;
        }
        data.set(unitKey, PARASITE_LIKE_BIOME);
        data.setManaged(unitKey, true);
        LikeBiomeSync.markUpsert(level, unitKey);
    }

    /**
     * Re-evaluates a unit: creates the like biome when it holds an infested block, removes the
     * auto generated like biome when it holds none. Units placed by the command are never removed.
     */
    public static void refreshUnit(ServerLevel level, BlockPos pos) {
        refreshUnitByKey(level, unitKey(pos));
    }

    /**
     * Defers the re-check to the end of the current tick. Used when the caller cannot know the final
     * block state yet (e.g. a block break event fires before the block is actually removed).
     */
    public static void scheduleRefresh(ServerLevel level, BlockPos pos) {
        PENDING_REFRESH.computeIfAbsent(level, key -> new java.util.HashSet<>()).add(unitKey(pos));
    }

    public static void refreshUnitByKey(ServerLevel level, long unitKey) {
        Boolean infested = unitHasInfestedBlock(level, unitKey);
        if (infested == null) {
            // chunk not loaded: keep the current state, the reconciler retries later
            return;
        }
        if (infested) {
            ensureUnit(level, unitKey);
            return;
        }
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);
        if (data.isManaged(unitKey) && data.remove(unitKey)) {
            LikeBiomeSync.markRemoval(level, unitKey);
        }
    }

    /**
     * @return {@code TRUE}/{@code FALSE} when the unit could be inspected, {@code null} when its
     * chunk is not loaded and the answer is unknown.
     */
    @Nullable
    public static Boolean unitHasInfestedBlock(ServerLevel level, long unitKey) {
        int baseX = unitStart(unitX(unitKey));
        int baseY = unitStart(unitY(unitKey));
        int baseZ = unitStart(unitZ(unitKey));
        if (!level.getChunkSource().hasChunk(baseX >> 4, baseZ >> 4)) {
            return null;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < UNIT_SIZE; dx++) {
            for (int dy = 0; dy < UNIT_SIZE; dy++) {
                for (int dz = 0; dz < UNIT_SIZE; dz++) {
                    BlockState state = level.getBlockState(cursor.set(baseX + dx, baseY + dy, baseZ + dz));
                    if (state.getBlock() instanceof InfestedBlockInterface) {
                        return Boolean.TRUE;
                    }
                }
            }
        }
        return Boolean.FALSE;
    }

    /** Bulk variant used after chunk generation converted a whole chunk. */
    public static void markUnitsInfested(ServerLevel level, Collection<Long> unitKeys) {
        for (Long key : unitKeys) {
            ensureUnit(level, key);
        }
    }

    // ------------------------------------------------------------------
    // manual (command) editing
    // ------------------------------------------------------------------

    /** Manual placement: the unit becomes a normal, unmanaged like biome. */
    public static void set(ServerLevel level, BlockPos pos, LikeBiome likeBiome) {
        long key = unitKey(pos);
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);
        data.set(key, likeBiome);
        data.setManaged(key, false);
        LikeBiomeSync.markUpsert(level, key);
    }

    public static boolean remove(ServerLevel level, BlockPos pos) {
        long key = unitKey(pos);
        boolean removed = LikeBiomeSavedData.get(level).remove(key);
        LikeBiomeSync.markRemoval(level, key);
        return removed;
    }

    /** Queues an existing unit for the next batch sync (value is read from the saved data). */
    public static void markUnitChanged(ServerLevel level, long unitKey) {
        LikeBiomeSync.markUpsert(level, unitKey);
    }

    public static int removeAll(ServerLevel level) {
        int count = LikeBiomeSavedData.get(level).removeAll();
        LikeBiomeSync.clear(level);
        SyncLikeBiomesPacket.sendAll(level, LikeBiomeSavedData.get(level));
        return count;
    }

    public static boolean overridesColors(ServerLevel level, BlockPos pos) {
        LikeBiome like = activeAt(level, pos);
        return like != null && like.overrideColors();
    }

    public static boolean overridesRain(ServerLevel level, BlockPos pos) {
        LikeBiome like = activeAt(level, pos);
        return like != null && like.overrideRain();
    }

    // ------------------------------------------------------------------
    // client synchronisation
    // ------------------------------------------------------------------

    /**
     * Full snapshot for one player, used on login and on dimension change.
     *
     * <p>Deliberately does <b>no</b> work here: this runs inside {@code PlayerLoggedInEvent} on the
     * server thread, and copying the key list of a 400k unit world there would block the very login
     * it is supposed to accelerate. The cursor is materialised on the first pump tick instead (see
     * {@link #pumpSnapshots}), which is spread over the following ticks anyway.</p>
     */
    public static void syncAllToPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        // The snapshot is paged over several ticks: a big world holds tens of thousands of units and
        // one packet containing all of them is both a login hitch and - from roughly 60k units on -
        // larger than the protocol maximum packet size.
        LOGIN_SYNC.put(player, SnapshotCursor.of(level));
    }

    public static void sendFullSnapshot(ServerLevel level) {
        SyncLikeBiomesPacket.sendAll(level, LikeBiomeSavedData.get(level));
    }

    // ------------------------------------------------------------------
    // paged login snapshot
    // ------------------------------------------------------------------

    /** Units carried by one page packet. 1024 units stay around 6-8 KB on the wire. */
    private static final int SNAPSHOT_PAGE_UNITS = 1024;

    /** Pages sent per player per tick; keeps the login burst bounded while staying fast. */
    private static final int SNAPSHOT_PAGES_PER_TICK = 4;

    /**
     * Absolute iteration cap of the paging loop. The loop is bounded by
     * {@link #SNAPSHOT_PAGES_PER_TICK} already; this is the belt-and-braces guard the login audit
     * asks for, so a future edit (a page size of zero, a cursor that does not advance) turns into one
     * error line instead of a frozen server thread.
     */
    private static final int SNAPSHOT_MAX_ITERATIONS = 64;

    private static final Map<ServerPlayer, SnapshotCursor> LOGIN_SYNC = new IdentityHashMap<>();

    private static final class SnapshotCursor {

        private final ServerLevel level;

        /** {@code null} until the first pump tick materialises it. */
        private long[] keys;

        private int index;

        private int pagesSent;

        private SnapshotCursor(ServerLevel level) {
            this.level = level;
        }

        static SnapshotCursor of(ServerLevel level) {
            return new SnapshotCursor(level);
        }

        boolean done() {
            return this.keys != null && this.index >= this.keys.length;
        }
    }

    /**
     * Sends at most {@link #SNAPSHOT_PAGES_PER_TICK} pages of the pending login snapshot for every
     * player. The first page carries the "reset" flag so a re-login drops stale client side units.
     */
    private static void pumpSnapshots(net.minecraft.server.MinecraftServer server) {
        if (LOGIN_SYNC.isEmpty()) {
            return;
        }
        java.util.Iterator<Map.Entry<ServerPlayer, SnapshotCursor>> iterator =
                LOGIN_SYNC.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerPlayer, SnapshotCursor> entry = iterator.next();
            ServerPlayer player = entry.getKey();
            SnapshotCursor cursor = entry.getValue();
            if (player.hasDisconnected() || player.serverLevel() != cursor.level) {
                // left the level / changed dimension: the next login or dimension change queues a
                // fresh snapshot for the new dimension
                iterator.remove();
                continue;
            }
            LikeBiomeSavedData data = LikeBiomeSavedData.get(cursor.level);
            if (cursor.keys == null) {
                // first tick after login: this is the only place the key list is copied, so a huge
                // world costs one allocation on a tick instead of inside the login event
                cursor.keys = data.keysSnapshot();
            }
            boolean first = cursor.index == 0;
            if (cursor.keys.length == 0) {
                // Zero units must still produce the reset page, otherwise a re-login keeps the units
                // of the previous session in the client cache forever (they are never overwritten).
                org.tdddd.epca.impl.network.ModNetwork.sendToPlayer(player,
                        new org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomeBatchPacket(
                                cursor.level.dimension().location(), java.util.Map.of(),
                                java.util.List.of(), true));
                cursor.pagesSent = 1;
                iterator.remove();
                continue;
            }
            int pages = 0;
            int iterations = 0;
            while (cursor.index < cursor.keys.length && pages < SNAPSHOT_PAGES_PER_TICK) {
                if (++iterations > SNAPSHOT_MAX_ITERATIONS) {
                    org.tdddd.epca.impl.epca.LOGGER.error(
                            "[epca] likeBiome.pumpSnapshots hit the iteration cap ({}) at index "
                                    + "{}/{} - the paging loop did not terminate, dropping the snapshot",
                            SNAPSHOT_MAX_ITERATIONS, cursor.index, cursor.keys.length);
                    break;
                }
                int step = Math.max(1, SNAPSHOT_PAGE_UNITS);
                int end = Math.min(cursor.index + step, cursor.keys.length);
                if (end <= cursor.index) {
                    org.tdddd.epca.impl.epca.LOGGER.error(
                            "[epca] likeBiome.pumpSnapshots cursor did not advance "
                                    + "({} -> {}) - aborting this snapshot to avoid an endless loop",
                            cursor.index, end);
                    cursor.index = cursor.keys.length;
                    break;
                }
                Map<Long, LikeBiome> upserts = new java.util.LinkedHashMap<>();
                for (int i = cursor.index; i < end; i++) {
                    long key = cursor.keys[i];
                    LikeBiome value = data.atKey(key);
                    if (value != null) {
                        upserts.put(key, value);
                    }
                }
                cursor.index = end;
                if (!upserts.isEmpty()) {
                    org.tdddd.epca.impl.network.ModNetwork.sendToPlayer(player,
                            new org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomeBatchPacket(
                                    cursor.level.dimension().location(), upserts, java.util.List.of(),
                                    first));
                    // only a packet that was really sent may consume the reset flag; otherwise a
                    // page whose units all disappeared before the flush would silently drop it and
                    // the client would keep the previous session's units
                    first = false;
                    cursor.pagesSent++;
                }
                pages++;
            }
            if (cursor.done()) {
                iterator.remove();
            }
        }
    }

    // ------------------------------------------------------------------
    // ticking: reconciler + one packet flush per tick
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        processPendingRefreshes();
        if (event.getServer().getTickCount() % SWEEP_INTERVAL_TICKS == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                sweep(level);
            }
        }
        pumpSnapshots(event.getServer());
        LikeBiomeSync.flushAll();
    }

    private static void processPendingRefreshes() {
        if (PENDING_REFRESH.isEmpty()) {
            return;
        }
        java.util.List<Map.Entry<ServerLevel, java.util.Set<Long>>> snapshot =
                new java.util.ArrayList<>(PENDING_REFRESH.entrySet());
        PENDING_REFRESH.clear();
        for (Map.Entry<ServerLevel, java.util.Set<Long>> entry : snapshot) {
            for (Long unitKey : entry.getValue()) {
                refreshUnitByKey(entry.getKey(), unitKey);
            }
        }
    }

    /**
     * Slow reconciler: re-checks a small slice of the auto generated units every
     * {@link #SWEEP_INTERVAL_TICKS} ticks so that units whose infested blocks disappeared without a
     * block event (explosions, /setblock, world edit, ...) are cleaned up eventually.
     */
    private static void sweep(ServerLevel level) {
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);
        int size = data.size();
        if (size <= 0) {
            SWEEP_CURSOR.remove(level);
            return;
        }
        long deadline = System.nanoTime() + SWEEP_BUDGET_NANOS;
        int[] cursor = SWEEP_CURSOR.computeIfAbsent(level, key -> new int[]{0});
        int start = Math.floorMod(cursor[0], size);
        int budget = Math.min(SWEEP_BATCH, size);
        int inspected = 0;
        for (int i = 0; i < budget; i++) {
            if (System.nanoTime() >= deadline) {
                break;
            }
            long key = data.keyAt((start + i) % size);
            if (key == Long.MIN_VALUE) {
                break;
            }
            inspected = i + 1;
            if (!data.isManaged(key)) {
                continue;
            }
            refreshUnitByKey(level, key);
        }
        cursor[0] = Math.floorMod(start + Math.max(1, inspected), size);
    }

    /** Drops per level bookkeeping, used when a world is unloaded. */
    public static void clearRuntimeState() {
        SWEEP_CURSOR.clear();
        PENDING_REFRESH.clear();
        RESOLVE_CACHE.clear();
        PARASITE_VERIFIED.clear();
        LOGIN_SYNC.clear();
        LikeBiomeSync.clearAll();
    }
}
