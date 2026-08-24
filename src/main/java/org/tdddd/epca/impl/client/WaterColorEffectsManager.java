package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WaterColorEffectsManager {
    
    private static final Map<BlockPos, Integer> acidSources = new ConcurrentHashMap<>();
    
    private static final Map<UUID, Vec3> contaminationSources = new ConcurrentHashMap<>();

    // ---- 新增：区块索引缓存 ----
    private static final Map<ChunkPos, Set<BlockPos>> INFESTED_BY_CHUNK = new ConcurrentHashMap<>();
    private static final int INFESTED_PURPLE = 0xFF8066AA;

    // ---- 外部 API（主动添加/移除，立即刷新周围水色） ----
    public static void addInfestedSource(BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        INFESTED_BY_CHUNK.computeIfAbsent(cp, k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        // 刷新周围 8 格半径内的区块，覆盖 7 格影响范围
        refreshArea(pos, 8);
    }

    public static void removeInfestedSource(BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        Set<BlockPos> set = INFESTED_BY_CHUNK.get(cp);
        if (set != null) {
            set.remove(pos.immutable());
            if (set.isEmpty()) INFESTED_BY_CHUNK.remove(cp);
        }
        // 刷新周围 8 格半径内的区块
        refreshArea(pos, 8);
    }

    public static void clearInfestedCache() {
        INFESTED_BY_CHUNK.clear();
    }

    /**
     * 扫描玩家附近指定区块半径内的所有虫染方块，并添加到缓存中。
     * @param level 客户端世界
     * @param center 玩家位置
     * @param chunkRadius 区块半径（例如 12）
     */
    public static void scanAndAddNearbyInfested(Level level, BlockPos center, int chunkRadius) {
        if (level == null || center == null) return;
        int minChunkX = (center.getX() - chunkRadius * 16) >> 4;
        int maxChunkX = (center.getX() + chunkRadius * 16) >> 4;
        int minChunkZ = (center.getZ() - chunkRadius * 16) >> 4;
        int maxChunkZ = (center.getZ() + chunkRadius * 16) >> 4;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        int addedCount = 0;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue; // 跳过未加载区块

                int baseX = cx << 4;
                int baseZ = cz << 4;
                for (int x = baseX; x < baseX + 16; x++) {
                    for (int z = baseZ; z < baseZ + 16; z++) {
                        for (int y = minY; y < maxY; y++) {
                            mutablePos.set(x, y, z);
                            BlockState state = level.getBlockState(mutablePos);
                            if (state.getBlock() instanceof InfestedBlockInterface) {
                                // 使用 addInfestedSource 添加到缓存并刷新周围 8 格
                                addInfestedSource(mutablePos.immutable());
                                addedCount++;
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- 核心距离查询（仅查相邻9个区块） ----
    private static float getNearestInfestedDistance(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        float minDistSq = Float.MAX_VALUE;
        double px = pos.getX() + 0.5;
        double py = pos.getY() + 0.5;
        double pz = pos.getZ() + 0.5;

        // 检查自身及周围8个区块
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos cp = new ChunkPos(cx + dx, cz + dz);
                Set<BlockPos> set = INFESTED_BY_CHUNK.get(cp);
                if (set == null) continue;
                for (BlockPos inf : set) {
                    double dx2 = px - (inf.getX() + 0.5);
                    double dy2 = py - (inf.getY() + 0.5);
                    double dz2 = pz - (inf.getZ() + 0.5);
                    double d2 = dx2*dx2 + dy2*dy2 + dz2*dz2;
                    if (d2 < minDistSq) minDistSq = (float) d2;
                }
            }
        }

        if (minDistSq <= 49.0f) { // 7²
            return (float) Math.sqrt(minDistSq);
        }
        return -1;
    }

    // ---- 修改颜色混合逻辑，叠加深紫色 ----
    public static int getWaterColor(BlockPos pos, int originalColor) {
        // 原有酸液效果
        int acidColor = getAcidColor(pos);
        // 原有血液污染效果
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

        // ---- 新增：受感染深紫色效果（性能最优） ----
        float dist = getNearestInfestedDistance(pos);
        if (dist >= 0) {
            // 距离越近强度越大，线性衰减
            float intensity = 1.0f - (dist / 7.0f);
            // 使用 0.85 权重使其明显，但不会完全覆盖原有颜色（保留部分水色层次）
            mixed = mixColors(mixed, INFESTED_PURPLE, intensity);
        }

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
        BlockPos centerPos = new BlockPos((int)center.x, (int)center.y, (int)center.z);
        refreshArea(centerPos, 2); 
    }

    public static void removeContaminationEffect(UUID uuid) {
        Vec3 center = contaminationSources.remove(uuid);
        if (center != null) {
            BlockPos centerPos = new BlockPos((int)center.x, (int)center.y, (int)center.z);
            refreshArea(centerPos, 2);
        }
    }

    private static int getAcidColor(BlockPos pos) {
        Integer distance = acidSources.get(pos);
        if (distance != null) {
            float factor = 1.0f - (Math.min(distance, 8) / 8.0f);
            int waterColor = 0xFF3F76E4;
            int acidColor = 0xFF00FF00;
            return mixColors(waterColor, acidColor, factor);
        }
        return -1;
    }

    private static int getContaminationColor(BlockPos pos) {
        for (Map.Entry<UUID, Vec3> entry : contaminationSources.entrySet()) {
            Vec3 center = entry.getValue();
            double dx = pos.getX() + 0.5 - center.x;
            double dy = pos.getY() + 0.5 - center.y;
            double dz = pos.getZ() + 0.5 - center.z;
            double distSq = dx*dx + dy*dy + dz*dz;
            if (distSq <= 2.5*2.5) {
                
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
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist <= 2.5) {
                float intensity = (float)(1.0 - dist / 2.5);
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
        int r = (int)(r1 * (1 - t) + r2 * t);
        int g = (int)(g1 * (1 - t) + g2 * t);
        int b = (int)(b1 * (1 - t) + b2 * t);
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

    public static void refreshNearbyWater(Player player, int radius) {
        if (player == null || player.level() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null) return;

        BlockPos center = player.blockPosition();
        int minX = center.getX() - radius;
        int minZ = center.getZ() - radius;
        int maxX = center.getX() + radius;
        int maxZ = center.getZ() + radius;

        
        Set<ChunkPos> chunksToUpdate = new HashSet<>();
        for (int x = minX >> 4; x <= maxX >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                chunksToUpdate.add(new ChunkPos(x, z));
            }
        }

        
        for (ChunkPos chunkPos : chunksToUpdate) {
            for (int y = mc.level.getMinSection(); y <= mc.level.getMaxSection(); y++) {
                mc.levelRenderer.setSectionDirty(chunkPos.x, y, chunkPos.z);
            }
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
        contaminationSources.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}