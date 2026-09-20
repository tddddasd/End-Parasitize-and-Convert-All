package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WaterColorEffectsManager {

    private static final Map<BlockPos, Integer> acidSources = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> contaminationSources = new ConcurrentHashMap<>();

    
    private static final Map<ChunkPos, Set<BlockPos>> INFESTED_BY_CHUNK = new ConcurrentHashMap<>();
    //private static final int INFESTED_PURPLE = 0xFF8066AA;

    

    
    public static void addInfestedSource(BlockPos pos) {
        // 26.1.2: ChunkPos is a record (x, z) and lost its BlockPos constructor; use ChunkPos#containing.
        ChunkPos cp = ChunkPos.containing(pos);
        INFESTED_BY_CHUNK.computeIfAbsent(cp, k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        refreshArea(pos, 8); 
    }

    
    public static void removeInfestedSource(BlockPos pos) {
        ChunkPos cp = ChunkPos.containing(pos);
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
            ChunkPos cp = ChunkPos.containing(pos);
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
        int bloodColor = getContaminationColor(pos);

        int mixed = originalColor;
        if (acidColor != -1) {
            mixed = mixColors(mixed, acidColor, 0.5f);
        }
        if (bloodColor != -1) {
            float intensity = getContaminationIntensity(pos);
            if (intensity > 0) {
                mixed = mixColors(mixed, bloodColor, intensity * 0.7f);
            }
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

    public static void addContaminationEffect(UUID uuid, Vec3 center) {
        contaminationSources.put(uuid, center);
        BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);
        refreshArea(centerPos, 2);
    }

    public static void removeContaminationEffect(UUID uuid) {
        Vec3 center = contaminationSources.remove(uuid);
        if (center != null) {
            BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);
            refreshArea(centerPos, 2);
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

    private static int getContaminationColor(BlockPos pos) {
        for (Map.Entry<UUID, Vec3> entry : contaminationSources.entrySet()) {
            Vec3 center = entry.getValue();
            double dx = pos.getX() + 0.5 - center.x;
            double dy = pos.getY() + 0.5 - center.y;
            double dz = pos.getZ() + 0.5 - center.z;
            if (dx * dx + dy * dy + dz * dz <= 2.5 * 2.5) {
                return 0xFFFF0000;
            }
        }
        return -1;
    }

    private static float getContaminationIntensity(BlockPos pos) {
        float maxIntensity = 0;
        for (Map.Entry<UUID, Vec3> entry : contaminationSources.entrySet()) {
            Vec3 center = entry.getValue();
            double dx = pos.getX() + 0.5 - center.x;
            double dy = pos.getY() + 0.5 - center.y;
            double dz = pos.getZ() + 0.5 - center.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= 2.5) {
                float intensity = (float) (1.0 - dist / 2.5);
                if (intensity > maxIntensity) maxIntensity = intensity;
            }
        }
        return maxIntensity;
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
            // 26.1.2: Level#getMinSection/getMaxSection -> getMinSectionY/getMaxSectionY, and ChunkPos exposes
            // record accessors x()/z() instead of public fields.
            for (int y = mc.level.getMinSectionY(); y <= mc.level.getMaxSectionY(); y++) {
                mc.levelRenderer.setSectionDirty(cp.x(), y, cp.z());
            }
        }
    }

    public static void clearAll() {
        acidSources.clear();
        contaminationSources.clear();
        INFESTED_BY_CHUNK.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}