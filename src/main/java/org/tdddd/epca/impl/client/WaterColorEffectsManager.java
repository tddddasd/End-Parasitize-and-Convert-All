package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side registry of the water colour modifiers of the mod.
 *
 * <p>Two independent sources remain:</p>
 * <ul>
 *   <li><b>acid</b> - {@link #updateClientEffect}/{@link #removeClientEffect}, pushed by
 *       {@code AcidWaterColorPacket} and read by {@link #getWaterColor};</li>
 *   <li><b>infested</b> - {@link #addInfestedSource}/{@link #removeInfestedSource}/
 *       {@link #addInfestedSourcesBatch}/{@link #clearInfestedCache}, pushed by
 *       {@code InfestedSourcePacket} / {@code SyncAllInfestedSourcesPacket}. The infested tint
 *       itself is currently commented out inside {@link #getWaterColor} (its colour constant is
 *       commented out too), so today only the acid source actually changes water.</li>
 * </ul>
 *
 * <p>The {@code epca:contaminated_water} entity no longer contributes here at all: its red water
 * tint was removed on request. Do not reintroduce it - the entity's own visuals are the red gas
 * clouds and dark-red specks drawn by {@code GasCloudRenderer}, not a recoloured water block.</p>
 */
public class WaterColorEffectsManager {

    private static final Map<BlockPos, Integer> acidSources = new ConcurrentHashMap<>();

    
    private static final Map<ChunkPos, Set<BlockPos>> INFESTED_BY_CHUNK = new ConcurrentHashMap<>();
    //private static final int INFESTED_PURPLE = 0xFF8066AA;

    

    
    public static void addInfestedSource(BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        INFESTED_BY_CHUNK.computeIfAbsent(cp, k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        refreshArea(pos, 8); 
    }

    
    public static void removeInfestedSource(BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        Set<BlockPos> set = INFESTED_BY_CHUNK.get(cp);
        if (set != null) {
            set.remove(pos.immutable());
            if (set.isEmpty()) INFESTED_BY_CHUNK.remove(cp);
        }
        refreshArea(pos, 8);
    }

    
    public static void addInfestedSourcesBatch(Collection<BlockPos> positions) {
        if (positions.isEmpty()) return;
        BlockPos first = null;
        for (BlockPos pos : positions) {
            ChunkPos cp = new ChunkPos(pos);
            INFESTED_BY_CHUNK.computeIfAbsent(cp, k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
            if (first == null) first = pos;
        }
        if (first != null) {
            refreshArea(first, 16);
        }
    }

    
    public static void clearInfestedCache() {
        INFESTED_BY_CHUNK.clear();
    }

    
    private static float getNearestInfestedDistance(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        float minDistSq = Float.MAX_VALUE;
        double px = pos.getX() + 0.5;
        double py = pos.getY() + 0.5;
        double pz = pos.getZ() + 0.5;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos cp = new ChunkPos(cx + dx, cz + dz);
                Set<BlockPos> set = INFESTED_BY_CHUNK.get(cp);
                if (set == null) continue;
                for (BlockPos inf : set) {
                    double dx2 = px - (inf.getX() + 0.5);
                    double dy2 = py - (inf.getY() + 0.5);
                    double dz2 = pz - (inf.getZ() + 0.5);
                    double d2 = dx2 * dx2 + dy2 * dy2 + dz2 * dz2;
                    if (d2 < minDistSq) minDistSq = (float) d2;
                }
            }
        }

        if (minDistSq <= 49.0f) {
            return (float) Math.sqrt(minDistSq);
        }
        return -1f;
    }

    
    public static int getWaterColor(BlockPos pos, int originalColor) {
        int acidColor = getAcidColor(pos);

        int mixed = originalColor;
        if (acidColor != -1) {
            mixed = mixColors(mixed, acidColor, 0.5f);
        }
/*
        float dist = getNearestInfestedDistance(pos);
        if (dist >= 0) {
            float intensity = 1.0f - (dist / 7.0f);
            mixed = mixColors(mixed, INFESTED_PURPLE, intensity);
        }
 */
        return mixed;
    }

    
    public static void updateClientEffect(BlockPos waterPos, BlockPos acidPos, int distance) {
        acidSources.put(waterPos.immutable(), Math.min(distance, 8));
        forceChunkUpdate(waterPos);
    }

    public static void removeClientEffect(BlockPos waterPos) {
        if (acidSources.remove(waterPos.immutable()) != null) {
            forceChunkUpdate(waterPos);
        }
    }

    private static int getAcidColor(BlockPos pos) {
        Integer distance = acidSources.get(pos);
        if (distance != null) {
            float factor = 1.0f - (Math.min(distance, 8) / 8.0f);
            return mixColors(0xFF3F76E4, 0xFF00FF00, factor);
        }
        return -1;
    }

    private static int mixColors(int colorA, int colorB, float t) {
        int r1 = (colorA >> 16) & 0xFF;
        int g1 = (colorA >> 8) & 0xFF;
        int b1 = colorA & 0xFF;
        int r2 = (colorB >> 16) & 0xFF;
        int g2 = (colorB >> 8) & 0xFF;
        int b2 = colorB & 0xFF;
        int r = (int) (r1 * (1 - t) + r2 * t);
        int g = (int) (g1 * (1 - t) + g2 * t);
        int b = (int) (b1 * (1 - t) + b2 * t);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    
    private static void forceChunkUpdate(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            int chunkX = pos.getX() >> 4;
            int chunkY = pos.getY() >> 4;
            int chunkZ = pos.getZ() >> 4;
            mc.levelRenderer.setSectionDirty(chunkX, chunkY, chunkZ);
        }
    }

    private static void refreshArea(BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int minZ = center.getZ() - radius;
        int maxX = center.getX() + radius;
        int maxZ = center.getZ() + radius;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) return;

        Set<ChunkPos> chunks = new HashSet<>();
        for (int x = minX >> 4; x <= maxX >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }

        for (ChunkPos cp : chunks) {
            for (int y = mc.level.getMinSection(); y <= mc.level.getMaxSection(); y++) {
                mc.levelRenderer.setSectionDirty(cp.x, y, cp.z);
            }
        }
    }

    public static void clearAll() {
        acidSources.clear();
        INFESTED_BY_CHUNK.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}