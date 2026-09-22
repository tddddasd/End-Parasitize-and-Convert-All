package org.tdddd.epca.impl.overworld.cursed;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ParasiteBiome;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deferred, exactly-once conversion of freshly generated chunks in the cursed world.
 *
 * <h2>Why the work is deferred</h2>
 * {@code ChunkEvent.Load} is posted from {@code ChunkStatusTasks#full}, i.e. while the chunk is still being
 * brought up to {@link net.minecraft.world.level.chunk.status.ChunkStatus#FULL}, and on the server that runs
 * on a worldgen worker thread. NeoForge's javadoc for that event states that interactions with the level
 * "must be delayed until the next game tick to prevent deadlocking the game". So the event handler only
 * records the chunk position here; {@link org.tdddd.epca.impl.events.CursedWorldChunkHandler} drains the queue
 * from {@code ServerTickEvent.Post} on the server thread, where the chunk is complete and
 * {@code level.setBlock} is legal.
 *
 * <h2>Why exactly once</h2>
 * The NeoForge event carries {@code isNewChunk()}, which is {@code true} only for a chunk that was generated
 * from a plain {@code ProtoChunk} - a chunk read back from disk arrives as an {@code ImposterProtoChunk} and
 * is never enqueued. On top of that, {@link #enqueue} refuses a position that is already waiting, and entries
 * are removed when drained. So a chunk can never be converted twice, and existing chunks are never touched.
 *
 * <h2>Why only fully generated chunks are converted</h2>
 * The cursed world now generates a chunk exactly like the normal overworld first (noise terrain, carvers and
 * the real biome's features and structures) and only converts it afterwards. {@link #drain} therefore takes
 * a chunk out of the queue only when it is already loaded ({@code ServerChunkCache#getChunkNow}) and its
 * persisted {@link ChunkStatus} is at or past {@link ChunkStatus#FULL}. A queued position whose chunk is not
 * ready yet stays in the queue and is retried on a later tick; it is never force-loaded or generated.
 *
 * <h2>Why converted chunks are re-sent</h2>
 * The chunk was already sent to tracking players while it still carried the real overworld biome data, so the
 * post-generation biome rewrite would not reach their clients on its own. After a chunk actually changed, it
 * is re-sent so client-side biome colours update.
 */
public final class CursedChunkConverter {

    /**
     * Hard upper bound on the number of positions waiting per level. The queue normally holds at most a few
     * dozen entries (one tick worth of chunk loads), so this cap only exists so that a level that can never
     * drain (for example one whose chunks are unloaded before they are ever converted) cannot grow the queue
     * without bound. When the cap is exceeded the oldest queued position is dropped and one warning is logged.
     */
    private static final int MAX_QUEUED_PER_LEVEL = 4096;

    /** Packed chunk positions that have been enqueued but not drained yet, per dimension. */
    private static final Map<ResourceKey<Level>, Set<Long>> PENDING = new HashMap<>();

    /** Guards the one-time resend error report so a broken send cannot spam the log every tick. */
    private static final AtomicBoolean RESEND_ERROR_REPORTED = new AtomicBoolean(false);

    private CursedChunkConverter() {}

    /** Packs a chunk position into a long using the {@code ChunkPos.toLong} layout. */
    private static long pack(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    private static int unpackX(long packed) {
        return (int) packed;
    }

    private static int unpackZ(long packed) {
        return (int) (packed >> 32);
    }

    /**
     * Records a freshly generated chunk for conversion on the next server tick.
     *
     * <p>Called from the worldgen thread, hence the synchronized block. A position that is already waiting is
     * ignored so a chunk can never be scheduled twice. If the per-level queue is full the oldest entry is
     * dropped to keep memory bounded.
     */
    public static void enqueue(ServerLevel level, int chunkX, int chunkZ) {
        synchronized (PENDING) {
            Set<Long> pending = PENDING.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>());
            if (pending.size() >= MAX_QUEUED_PER_LEVEL) {
                var oldest = pending.iterator();
                if (oldest.hasNext()) {
                    oldest.next();
                    oldest.remove();
                    epca.LOGGER.warn("Cursed world: conversion queue for {} exceeded {} entries, dropping the oldest",
                            level.dimension().identifier(), MAX_QUEUED_PER_LEVEL);
                }
            }
            pending.add(pack(chunkX, chunkZ));
        }
    }

    /**
     * Converts up to {@code maxChunks} of the chunks queued for {@code level}. Must be called on the server
     * thread.
     *
     * <p>A queued position is converted only when its chunk is already loaded and has reached
     * {@link ChunkStatus#FULL}; otherwise it is put back into the queue for a later tick.
     *
     * @return the number of chunks that were converted
     */
    public static int drain(ServerLevel level, int maxChunks) {
        List<Long> ready = new ArrayList<>(Math.min(maxChunks, 16));
        List<Long> notReady = new ArrayList<>();
        synchronized (PENDING) {
            Set<Long> pending = PENDING.get(level.dimension());
            if (pending == null || pending.isEmpty()) return 0;
            var iterator = pending.iterator();
            while (iterator.hasNext() && ready.size() < maxChunks) {
                long packed = iterator.next();
                int chunkX = unpackX(packed);
                int chunkZ = unpackZ(packed);
                // getChunkNow never loads or generates: a position whose chunk is not in memory right now
                // stays queued instead of being dropped, so it keeps its chance on a later tick.
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
                    notReady.add(packed);
                    continue;
                }
                iterator.remove();
                ready.add(packed);
            }
            if (pending.isEmpty()) PENDING.remove(level.dimension());
        }

        // Re-queue everything that is not fully generated yet, still honouring the per-level cap.
        if (!notReady.isEmpty()) {
            synchronized (PENDING) {
                Set<Long> pending = PENDING.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>());
                for (long packed : notReady) {
                    if (pending.size() >= MAX_QUEUED_PER_LEVEL) {
                        epca.LOGGER.warn("Cursed world: conversion queue for {} exceeded {} entries, dropping a pending chunk",
                                level.dimension().identifier(), MAX_QUEUED_PER_LEVEL);
                        continue;
                    }
                    pending.add(packed);
                }
            }
        }

        int converted = 0;
        for (long packed : ready) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(unpackX(packed), unpackZ(packed));
            // The chunk could have been unloaded between the two loops; then nothing is converted for it.
            if (chunk == null) continue;
            try {
                if (convertChunkAndReport(level, chunk)) {
                    converted++;
                    // The chunk was probably already sent while it still carried overworld biomes, so the
                    // rewritten biome data has to be pushed to the players tracking it.
                    resendChunkToTrackingPlayers(level, chunk);
                }
            } catch (Exception e) {
                epca.LOGGER.error("Cursed world: chunk conversion failed for chunk {} in {}",
                        chunk.getPos(), level.dimension().identifier(), e);
            }
        }
        return converted;
    }

    /** Drops every queued position for a dimension. */
    public static void clear(ResourceKey<Level> dimension) {
        synchronized (PENDING) {
            PENDING.remove(dimension);
        }
    }

    /**
     * Runs the silent chunk conversion and makes sure the whole chunk carries the parasite biome.
     *
     * <p>Both steps are guarded individually: a failure in the biome fill must not lose the block conversion
     * (and vice versa), and nothing here may throw into the server tick loop.
     */
    public static void convertChunk(ServerLevel level, ChunkAccess chunk) {
        convertChunkAndReport(level, chunk);
    }

    /**
     * Same as {@link #convertChunk} but reports whether the chunk actually changed, so the caller can decide
     * whether the chunk has to be re-sent to its tracking players.
     *
     * @return {@code true} when at least one of the two conversion steps succeeded
     */
    public static boolean convertChunkAndReport(ServerLevel level, ChunkAccess chunk) {
        boolean changed = false;
        try {
            int converted = BlockConversionManager.getInstance().convertChunkAtGeneration(level, chunk);
            if (converted > 0) {
                changed = true;
                epca.LOGGER.debug("Cursed world: converted {} blocks in chunk {} of {}",
                        converted, chunk.getPos(), level.dimension().identifier());
            }
        } catch (Exception e) {
            epca.LOGGER.error("Cursed world: block conversion failed for chunk {} in {}",
                    chunk.getPos(), level.dimension().identifier(), e);
        }
        try {
            if (forceParasiteBiome(level, chunk)) {
                changed = true;
            }
        } catch (Exception e) {
            epca.LOGGER.error("Cursed world: biome fill failed for chunk {} in {}",
                    chunk.getPos(), level.dimension().identifier(), e);
        }
        return changed;
    }

    /**
     * Sends a converted chunk to every player tracking it, so the rewritten biome data (and therefore the
     * client-side grass/foliage/water/sky/fog colours) actually reaches their clients.
     *
     * <p>{@code ServerChunkCache#chunkMap} is public in this version and
     * {@code ChunkMap#getPlayers(ChunkPos, boolean)} returns exactly the players tracking the chunk. Both
     * {@code BitSet} arguments of the packet are passed as {@code null}, which the packet constructor treats
     * as "no light update": the client keeps the light it already has and only refreshes the chunk contents
     * (which include the biome palette).
     *
     * <p>Any failure is caught and logged at most once, because this runs inside the server tick.
     */
    private static void resendChunkToTrackingPlayers(ServerLevel level, LevelChunk chunk) {
        try {
            List<ServerPlayer> tracking = level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false);
            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
            for (ServerPlayer player : tracking) {
                player.connection.send(packet);
            }
        } catch (Exception e) {
            if (RESEND_ERROR_REPORTED.compareAndSet(false, true)) {
                epca.LOGGER.error("Cursed world: re-sending a converted chunk to its tracking players failed;"
                        + " converted chunks will keep their old biome colours on clients", e);
            }
        }
    }

    /**
     * Fills the whole chunk with {@code epca:parasite_biome} if it is not already that biome.
     *
     * <p>This is the primary biome rewrite now that generation runs with the real overworld multi-noise
     * biome source. The cheap sample check keeps the common case at four biome lookups instead of a full
     * chunk fill.
     *
     * @return {@code true} when the chunk's biome data was rewritten
     */
    private static boolean forceParasiteBiome(ServerLevel level, ChunkAccess chunk) {
        Holder<Biome> target = parasiteBiomeHolder(level);
        if (target == null) return false;

        if (chunk.getNoiseBiome(0, 0, 0).is(target)
                && chunk.getNoiseBiome(3, 0, 0).is(target)
                && chunk.getNoiseBiome(0, 0, 3).is(target)
                && chunk.getNoiseBiome(3, 0, 3).is(target)) {
            return false;
        }
        // FixedBiomeSource ignores the sampler it is handed, so passing null here is safe.
        chunk.fillBiomesFromNoise(new FixedBiomeSource(target), null);
        return true;
    }

    /**
     * The parasite biome holder of {@code level}.
     *
     * <p>A level made by an older build of this feature still has a fixed parasite biome source, so the holder
     * is taken from there when possible - that hands {@link net.minecraft.world.level.chunk.LevelChunkSection}
     * the exact same holder the world already uses. The current preset generates with the real overworld
     * multi-noise biome source instead, so in that (normal) case the holder is looked up in the level's own
     * biome registry.
     */
    public static Holder<Biome> parasiteBiomeHolder(ServerLevel level) {
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        if (source instanceof FixedBiomeSource) {
            for (Holder<Biome> biome : source.possibleBiomes()) {
                if (CursedWorlds.isParasiteBiome(biome)) return biome;
            }
        }
        Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        if (!biomes.containsKey(ParasiteBiome.PARASITE_BIOME)) return null;
        return biomes.wrapAsHolder(biomes.getValue(ParasiteBiome.PARASITE_BIOME));
    }
}
