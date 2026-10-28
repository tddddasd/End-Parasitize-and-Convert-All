package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity;

import java.util.Set;

/**
 * Shared limits for naturally created hive nodes.
 *
 * <p>A chunk may only receive a limited number of nodes from the parasite ecology (a ripper
 * transforming, a stocked node absorbing a ripper, or a beckon core degrading). Nodes placed by a
 * player are not counted. Separate hive structures also have to keep a chunk-radius gap, so two
 * colonies cannot grow on top of each other.</p>
 */
public final class HiveNodeRules {

    /** Maximum number of non-player-placed hive nodes in one chunk. */
    public static final int MAX_NATURAL_NODES_PER_CHUNK = 10;

    /** A second hive structure may not appear within this many chunks of an existing one. */
    public static final int STRUCTURE_EXCLUSION_CHUNKS = 8;

    private HiveNodeRules() {
    }

    /** Counts the naturally created hive nodes whose block entity is in the same chunk as {@code pos}. */
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

    /** True while this chunk still has room for another naturally created hive node. */
    public static boolean canCreateNaturalNode(ServerLevel level, BlockPos pos) {
        return countNaturalNodesInChunk(level, pos) < MAX_NATURAL_NODES_PER_CHUNK;
    }

    /** Marks the hive node at {@code pos} as naturally created, so it counts towards the chunk cap. */
    public static void markNatural(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof HiveNodeBlockEntity node) {
            node.setNatural(true);
        }
    }

    /**
     * True when a hive structure has a node inside the given chunk radius of {@code origin}.
     *
     * <p>Only loaded chunks are inspected; this is a guard against a second colony appearing next
     * to an existing one, not a world-wide structure index.</p>
     *
     * @param excluded nodes belonging to the structure currently being checked, or {@code null}
     */
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

    /** True when any existing hive structure is within {@link #STRUCTURE_EXCLUSION_CHUNKS} chunks. */
    public static boolean hasHiveStructureNearby(ServerLevel level, BlockPos origin) {
        return hasHiveStructureWithinChunks(level, origin, STRUCTURE_EXCLUSION_CHUNKS, null);
    }
}
