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

public final class CursedChunkConverter {

    private static final int MAX_QUEUED_PER_LEVEL = 4096;

    private static final Map<ResourceKey<Level>, Set<Long>> PENDING = new HashMap<>();

    private static final AtomicBoolean RESEND_ERROR_REPORTED = new AtomicBoolean(false);

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

            if (chunk == null) continue;
            try {
                if (convertChunkAndReport(level, chunk)) {
                    converted++;

                    resendChunkToTrackingPlayers(level, chunk);
                }
            } catch (Exception e) {
                epca.LOGGER.error("Cursed world: chunk conversion failed for chunk {} in {}",
                        chunk.getPos(), level.dimension().identifier(), e);
            }
        }
        return converted;
    }

    public static void clear(ResourceKey<Level> dimension) {
        synchronized (PENDING) {
            PENDING.remove(dimension);
        }
    }

    public static void convertChunk(ServerLevel level, ChunkAccess chunk) {
        convertChunkAndReport(level, chunk);
    }

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

    private static boolean forceParasiteBiome(ServerLevel level, ChunkAccess chunk) {
        Holder<Biome> target = parasiteBiomeHolder(level);
        if (target == null) return false;

        if (chunk.getNoiseBiome(0, 0, 0).is(target)
                && chunk.getNoiseBiome(3, 0, 0).is(target)
                && chunk.getNoiseBiome(0, 0, 3).is(target)
                && chunk.getNoiseBiome(3, 0, 3).is(target)) {
            return false;
        }

        chunk.fillBiomesFromNoise(new FixedBiomeSource(target), null);
        return true;
    }

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
