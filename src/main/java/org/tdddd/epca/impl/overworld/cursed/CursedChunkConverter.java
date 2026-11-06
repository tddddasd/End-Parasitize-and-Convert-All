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

public final class CursedChunkConverter {

    private static final Map<ResourceKey<Level>, Set<Long>> PENDING = new HashMap<>();

    private static final int MAX_PENDING_PER_DIMENSION = 4096;

    private static final Set<String> LOGGED_RESEND_FAILURES = ConcurrentHashMap.newKeySet();

    private CursedChunkConverter() {}

    private static long pack(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    private static int unpackX(long packed) {
        return (int) packed;
    }

    private static int unpackZ(long packed) {
        return (int) (packed >> 32);
    }

    public static void enqueue(ServerLevel level, int chunkX, int chunkZ) {
        synchronized (PENDING) {
            Set<Long> pending = PENDING.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>());
            addBounded(pending, pack(chunkX, chunkZ));
        }
    }

    private static void addBounded(Set<Long> pending, long packed) {
        if (!pending.add(packed)) return;
        while (pending.size() > MAX_PENDING_PER_DIMENSION) {
            Iterator<Long> oldest = pending.iterator();
            oldest.next();
            oldest.remove();
        }
    }

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

            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (chunk == null) {
                notReady.add(packed);
                continue;
            }

            if (!chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
                notReady.add(packed);
                continue;
            }
            if (convertChunk(level, chunk)) {

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

    public static void clear(ResourceKey<Level> dimension) {
        synchronized (PENDING) {
            PENDING.remove(dimension);
        }
    }

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

            if (forceParasiteBiome(level, chunk)) changed = true;
        } catch (Exception e) {
            epca.LOGGER.error("Cursed world: biome fill failed for chunk {} in {}",
                    chunk.getPos(), level.dimension().location(), e);
        }
        return changed;
    }

    private static void resendChunk(ServerLevel level, LevelChunk chunk) {
        try {

            List<ServerPlayer> trackingPlayers = level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false);
            if (trackingPlayers.isEmpty()) return;
            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
            for (ServerPlayer player : trackingPlayers) {
                player.connection.send(packet);
            }
        } catch (Exception e) {

            String kind = e.getClass().getName();
            if (LOGGED_RESEND_FAILURES.add(kind)) {
                epca.LOGGER.error("Cursed world: re-sending converted chunk {} in {} to its tracking players "
                        + "failed; further failures of type {} are not logged",
                        chunk.getPos(), level.dimension().location(), kind, e);
            }
        }
    }

    private static boolean forceParasiteBiome(ServerLevel level, ChunkAccess chunk) {
        Holder<Biome> target = parasiteBiomeHolder(level);
        if (target == null) return false;

        if (sameBiome(chunk.getNoiseBiome(0, 0, 0), target)
                && sameBiome(chunk.getNoiseBiome(3, 0, 0), target)
                && sameBiome(chunk.getNoiseBiome(0, 0, 3), target)
                && sameBiome(chunk.getNoiseBiome(3, 0, 3), target)) {
            return false;
        }

        chunk.fillBiomesFromNoise(new FixedBiomeSource(target), null);
        return true;
    }

    private static boolean sameBiome(Holder<Biome> candidate, Holder<Biome> target) {
        return candidate.unwrapKey().isPresent() && candidate.unwrapKey().equals(target.unwrapKey());
    }

    public static Holder<Biome> parasiteBiomeHolder(ServerLevel level) {
        return CursedWorlds.parasiteBiome(level);
    }
}
