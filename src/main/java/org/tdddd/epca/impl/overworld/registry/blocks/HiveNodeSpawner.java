package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Curbug;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper;

import java.util.ArrayList;
import java.util.List;

public final class HiveNodeSpawner {

    private static final int CHECK_INTERVAL_TICKS = 100;

    private static final double HALF_SQUARE = 16.0D;

    private static final double VERTICAL_REACH = 256.0D;

    private static final int REQUIRED_KINDS = 16;

    private static final int MAX_CONVERSIONS_PER_PASS = 1;

    /**
     * Tick counter per level. A single static counter shared by all levels made the pass run once
     * per {@code CHECK_INTERVAL_TICKS * levelCount} ticks instead of once per
     * {@code CHECK_INTERVAL_TICKS} for each level.
     */
    private static final java.util.Map<ServerLevel, int[]> TIMER = new java.util.IdentityHashMap<>();

    private HiveNodeSpawner() {
    }

    public static void onServerTick(ServerLevel level) {
        int[] timer = TIMER.computeIfAbsent(level, key -> new int[]{0});
        if (++timer[0] < CHECK_INTERVAL_TICKS) {
            return;
        }
        timer[0] = 0;

        if (EvolutionManager.getStageForDimension(level) < 0) {
            return;
        }
        runPass(level);
    }

    private static void runPass(ServerLevel level) {
        List<Ripper> rippers = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Ripper ripper && ripper.isAlive()) {
                rippers.add(ripper);
            }
        }
        if (rippers.isEmpty()) {
            return;
        }

        for (int attempt = 0; attempt < rippers.size(); attempt++) {
            Ripper pick = rippers.get(level.getRandom().nextInt(rippers.size()));
            if (!pick.isAlive()) {
                continue;
            }
            if (tryConvert(level, pick)) {
                return;
            }
        }
    }

    public static boolean tryConvert(ServerLevel level, Ripper ripper) {

        AABB area = new AABB(ripper.blockPosition()).inflate(HALF_SQUARE, VERTICAL_REACH, HALF_SQUARE);
        int curbugs = level.getEntitiesOfClass(Curbug.class, area).size();
        int rippers = level.getEntitiesOfClass(Ripper.class, area).size();
        if (curbugs + rippers < REQUIRED_KINDS) {
            return false;
        }
        BlockPos pos = ripper.blockPosition();
        if (!HiveNodeRules.canCreateNaturalNode(level, pos)) {
            return false;
        }
        ripper.discard();
        level.setBlock(pos, ModBlocks.HIVE_NODE.get().defaultBlockState(), 3);
        HiveNodeRules.markNatural(level, pos);
        return true;
    }
}
