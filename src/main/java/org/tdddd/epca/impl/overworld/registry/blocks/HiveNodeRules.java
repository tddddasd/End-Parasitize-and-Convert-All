package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity;

import java.util.Set;

public final class HiveNodeRules {

    public static final int MAX_NATURAL_NODES_PER_CHUNK = 10;

    public static final int STRUCTURE_EXCLUSION_CHUNKS = 8;

    private HiveNodeRules() {
    }

    public static int countNaturalNodesInChunk(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) {
            return 0;
        }
        int count = 0;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof HiveNodeBlockEntity node && node.isNatural()) {
                count++;
            }
        }
        return count;
    }

    public static boolean canCreateNaturalNode(ServerLevel level, BlockPos pos) {
        return countNaturalNodesInChunk(level, pos) < MAX_NATURAL_NODES_PER_CHUNK;
    }

    public static void markNatural(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof HiveNodeBlockEntity node) {
            node.setNatural(true);
        }
    }

    public static boolean hasHiveStructureWithinChunks(
            ServerLevel level, BlockPos origin, int chunkRadius, Set<BlockPos> excluded) {
        int centerX = SectionPos.blockToSectionCoord(origin.getX());
        int centerZ = SectionPos.blockToSectionCoord(origin.getZ());
        for (int chunkX = centerX - chunkRadius; chunkX <= centerX + chunkRadius; chunkX++) {
            for (int chunkZ = centerZ - chunkRadius; chunkZ <= centerZ + chunkRadius; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof HiveNodeBlockEntity node) || !node.isInStructure()) {
                        continue;
                    }
                    if (excluded != null && excluded.contains(node.getBlockPos())) {
                        continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasHiveStructureNearby(ServerLevel level, BlockPos origin) {
        return hasHiveStructureWithinChunks(level, origin, STRUCTURE_EXCLUSION_CHUNKS, null);
    }
}
