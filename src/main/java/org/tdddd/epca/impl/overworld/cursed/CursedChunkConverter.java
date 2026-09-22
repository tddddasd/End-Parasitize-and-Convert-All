package org.tdddd.epca.impl.overworld.cursed;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deferred, exactly-once conversion of freshly generated chunks in the cursed world.
 *
 * <h2>Why the work is deferred</h2>
 * Forge's {@code ChunkEvent.Load} is posted while the chunk is still being brought up to
 * {@code ChunkStatus.FULL} and, on the server, from a worldgen worker thread. Replacing blocks there would
 * deadlock the game, and the chunk's sections may not even be in their final shape yet. The event handler
 * therefore only records the chunk position here;
 * {@link org.tdddd.epca.impl.events.CursedWorldChunkHandler} drains the queue from
 * {@code TickEvent.ServerTickEvent} (phase {@code END}) on the server thread, where the chunk is complete and
 * {@code level.setBlock} is legal.
 *
 * <h2>Why the chunk must be generated first</h2>
 * The world preset generates the cursed overworld with the normal multi-noise overworld biomes and the
 * vanilla overworld noise settings, so terrain, carvers, the real biomes' features (trees, grass, flowers,
 * ores) and structures all generate exactly as in a normal overworld. Only afterwards is that finished chunk
 * converted: blocks become their infested forms and the biome data is rewritten to {@code epca:parasite_biome}.
 * {@link #drain} therefore refuses to touch a position whose chunk is not loaded and fully generated yet,
 * and never loads or generates anything itself.
 *
 * <h2>Why exactly once</h2>
 * The event carries {@code isNewChunk()}, which is {@code true} only for a chunk that was generated rather
 * than read back from disk, so an existing save is never enqueued. On top of that {@link #enqueue} refuses a
 * position that is already waiting and entries are removed once they have been converted, so a chunk can
 * never be converted twice and already-existing chunks are never touched.
 */
public final class CursedChunkConverter {

    /** Packed chunk positions that have been enqueued but not drained yet, per dimension. */
    private static final Map<ResourceKey<Level>, Set<Long>> PENDING = new HashMap<>();

    /**
     * Upper bound on the number of queued chunk positions per dimension.
     *
     * <p>A position that is not ready to be converted stays queued, so a level that generates far faster
     * than the server can convert (for example a large view distance, or many players exploring at once)
     * could otherwise grow the queue without bound. When the cap is exceeded the oldest entries are dropped,
     * which keeps memory bounded; those chunks simply stay unconverted like any chunk in an already-explored
     * area.
     */
    private static final int MAX_PENDING_PER_DIMENSION = 4096;

    /**
     * Exception class names of chunk re-send failures that have already been logged, so a failure that
     * repeats every tick is logged once per kind instead of flooding the log.
     */
    private static final Set<String> LOGGED_RESEND_FAILURES = ConcurrentHashMap.newKeySet();

    private CursedChunkConverter() {}

    /** Packs a chunk position into a long, using the same layout as {@code ChunkPos.toLong}. */
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
     * ignored so a chunk can never be scheduled twice.
     */
    public static void enqueue(ServerLevel level, int chunkX, int chunkZ) {
        synchronized (PENDING) {
            Set<Long> pending = PENDING.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>());
            addBounded(pending, pack(chunkX, chunkZ));
        }
    }

    /**
     * Adds {@code packed} to {@code pending} while keeping the queue bounded.
     *
     * <p>{@code pending} is a {@link LinkedHashSet}, so iteration order is insertion order and the first
     * element an iterator returns is the oldest one. A position that is already queued keeps its place and is
     * not duplicated. Once the queue is longer than {@link #MAX_PENDING_PER_DIMENSION} the oldest entries are
     * dropped, so the queue can never grow without bound.
     */
    private static void addBounded(Set<Long> pending, long packed) {
        if (!pending.add(packed)) return;
        while (pending.size() > MAX_PENDING_PER_DIMENSION) {
            Iterator<Long> oldest = pending.iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /**
     * Converts up to {@code maxChunks} of the chunks queued for {@code level}. Must be called on the server
     * thread.
     *
     * <p>A queued position is only converted when its chunk is loaded <em>and</em> already fully generated.
     * A position whose chunk is not ready yet is put back into the queue and retried on a later tick, so no
     * entry is ever lost and nothing here starts world generation.
     *
     * @return the number of chunks that were converted
     */
    public static int drain(ServerLevel level, int maxChunks) {
        if (maxChunks <= 0) return 0;

        List<Long> batch = new ArrayList<>(Math.min(maxChunks, 16));
        synchronized (PENDING) {
            Set<Long> pending = PENDING.get(level.dimension());
            if (pending == null || pending.isEmpty()) return 0;
            Iterator<Long> iterator = pending.iterator();
            while (iterator.hasNext() && batch.size() < maxChunks) {
                batch.add(iterator.next());
                iterator.remove();
            }
            if (pending.isEmpty()) PENDING.remove(level.dimension());
        }

        int converted = 0;
        List<Long> notReady = new ArrayList<>();
        for (long packed : batch) {
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            // getChunkNow never loads or generates a chunk: it returns the chunk only when it is already
            // loaded, so a queued position can never force world generation. A null result means the chunk
            // is still being generated or was unloaded again, so retry it on a later tick.
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (chunk == null) {
                notReady.add(packed);
                continue;
            }
            // Explicit "fully generated before conversion" gate. In 1.20.1 there is no
            // ChunkAccess#getPersistedStatus() (that accessor only exists in later versions) and
            // LevelChunk#getStatus() is hardcoded to ChunkStatus.FULL, so on top of getChunkNow this is a
            // defensive guarantee of the requirement rather than a filter that can actually reject here.
            if (!chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
                notReady.add(packed);
                continue;
            }
            if (convertChunk(level, chunk)) {
                // Only a chunk that actually changed is re-sent: the client has to be told about the
                // rewritten biome data, otherwise its biome colours keep the pre-conversion values.
                resendChunk(level, chunk);
            }
            converted++;
        }
        if (!notReady.isEmpty()) {
            synchronized (PENDING) {
                Set<Long> pending = PENDING.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>());
                for (long packed : notReady) {
                    addBounded(pending, packed);
                }
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
     *
     * @return {@code true} when the chunk was actually changed by at least one of the two steps
     */
    public static boolean convertChunk(ServerLevel level, ChunkAccess chunk) {
        boolean changed = false;
        try {
            int converted = BlockConversionManager.getInstance().convertChunkAtGeneration(level, chunk);
            if (converted > 0) {
                changed = true;
                epca.LOGGER.debug("Cursed world: converted {} blocks in chunk {} of {}",
                        converted, chunk.getPos(), level.dimension().location());
            }
        } catch (Exception e) {
            epca.LOGGER.error("Cursed world: block conversion failed for chunk {} in {}",
                    chunk.getPos(), level.dimension().location(), e);
        }
        try {
            // The biome rewrite is the primary mechanism now: the chunk was generated with the real
            // overworld biomes, so its biome data has to be replaced with the parasite biome afterwards.
            if (forceParasiteBiome(level, chunk)) changed = true;
        } catch (Exception e) {
            epca.LOGGER.error("Cursed world: biome fill failed for chunk {} in {}",
                    chunk.getPos(), level.dimension().location(), e);
        }
        return changed;
    }

    /**
     * Re-sends a converted chunk to every player tracking it, so the client replaces the biome data (and the
     * blocks) it received before the conversion with the converted state.
     *
     * <p>The packet is built only after the conversion because
     * {@link ClientboundLevelChunkWithLightPacket} serialises the chunk - including its biomes - in its
     * constructor. A {@code null} sky/block light mask means "send the full light data", which is exactly
     * how vanilla builds the first chunk packet for a player.
     */
    private static void resendChunk(ServerLevel level, LevelChunk chunk) {
        try {
            // ChunkMap#getPlayers(ChunkPos, boolean) and the ServerChunkCache#chunkMap field are both public
            // in 1.20.1, so the players that track this chunk can be read directly. With {@code false} the
            // call returns exactly the players within the server's chunk tracking range of that chunk.
            List<ServerPlayer> trackingPlayers = level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false);
            if (trackingPlayers.isEmpty()) return;
            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
            for (ServerPlayer player : trackingPlayers) {
                player.connection.send(packet);
            }
        } catch (Exception e) {
            // A failing re-send must never break the server tick, and the same failure would repeat every
            // tick, so each failure kind is logged only once.
            String kind = e.getClass().getName();
            if (LOGGED_RESEND_FAILURES.add(kind)) {
                epca.LOGGER.error("Cursed world: re-sending converted chunk {} in {} to its tracking players "
                        + "failed; further failures of type {} are not logged",
                        chunk.getPos(), level.dimension().location(), kind, e);
            }
        }
    }

    /**
     * Fills the whole chunk with {@code epca:parasite_biome} if it is not already that biome.
     *
     * <p>This is the primary biome rewrite: the world preset deliberately generates the chunk with the real
     * overworld biomes first, so the parasite biome has to be written afterwards. The cheap sample check
     * keeps the common case at four biome lookups instead of a full chunk fill.
     *
     * @return {@code true} when the biome data was actually rewritten
     */
    private static boolean forceParasiteBiome(ServerLevel level, ChunkAccess chunk) {
        Holder<Biome> target = parasiteBiomeHolder(level);
        if (target == null) return false;

        // Compared by resource key instead of Holder#is(Holder): that overload does not exist in 1.20.1.
        if (sameBiome(chunk.getNoiseBiome(0, 0, 0), target)
                && sameBiome(chunk.getNoiseBiome(3, 0, 0), target)
                && sameBiome(chunk.getNoiseBiome(0, 0, 3), target)
                && sameBiome(chunk.getNoiseBiome(3, 0, 3), target)) {
            return false;
        }
        // FixedBiomeSource ignores the sampler it is handed, so passing null here is safe.
        chunk.fillBiomesFromNoise(new FixedBiomeSource(target), null);
        return true;
    }

    /**
     * Whether {@code candidate} is the same biome as {@code target} according to its resource key. A holder
     * without a key (unregistered/direct) is never equal to a registered biome.
     */
    private static boolean sameBiome(Holder<Biome> candidate, Holder<Biome> target) {
        return candidate.unwrapKey().isPresent() && candidate.unwrapKey().equals(target.unwrapKey());
    }

    /**
     * The parasite biome holder of {@code level}: the generator's own holder for a world from the earlier
     * build of this feature, otherwise the holder from the level's biome registry.
     */
    public static Holder<Biome> parasiteBiomeHolder(ServerLevel level) {
        return CursedWorlds.parasiteBiome(level);
    }
}
