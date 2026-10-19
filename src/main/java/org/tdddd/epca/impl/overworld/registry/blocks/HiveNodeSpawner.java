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

/**
 * Turns a ripper into a hive node when a colony gathers.
 *
 * <p>Condition: inside a 2x2 chunk square (32 blocks wide, any height) there are at least 16
 * {@link Curbug} and {@link Ripper} together, and the dimension's erosion stage is 0 or above. One
 * ripper of that group then becomes a hive node block.</p>
 */
public final class HiveNodeSpawner {

    /** How often the check runs (5 s). */
    private static final int CHECK_INTERVAL_TICKS = 100;
    /** Half of the 32-block square. */
    private static final double HALF_SQUARE = 16.0D;
    /** Effectively unlimited height for the count. */
    private static final double VERTICAL_REACH = 256.0D;
    /** Minimum combined curbub + ripper count inside that square (12 as specified). */
    private static final int REQUIRED_KINDS = 12;
    /** Upper bound of conversions per pass, so a big colony converts gradually. */
    private static final int MAX_CONVERSIONS_PER_PASS = 1;

    private static int timer = 0;

    private HiveNodeSpawner() {
    }

    public static void onServerTick(ServerLevel level) {
        if (++timer < CHECK_INTERVAL_TICKS) {
            return;
        }
        timer = 0;
        // Erosion stage gate: the dimension must be eroded at all (stage 0 or above).
        if (EvolutionManager.getStageForDimension(level) < 0) {
            return;
        }
        List<Ripper> rippers = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Ripper ripper && ripper.isAlive()) {
                rippers.add(ripper);
            }
        }
        int converted = 0;
        for (Ripper ripper : rippers) {
            if (converted >= MAX_CONVERSIONS_PER_PASS) {
                return;
            }
            if (!ripper.isAlive()) {
                continue;
            }
            if (tryConvert(level, ripper)) {
                converted++;
            }
        }
    }

    /** Converts one ripper into a hive node when its colony is large enough. */
    public static boolean tryConvert(ServerLevel level, Ripper ripper) {
        // 2x2 chunk square, any height (the spec says height does not matter)
        AABB area = new AABB(ripper.blockPosition()).inflate(HALF_SQUARE, VERTICAL_REACH, HALF_SQUARE);
        int curbugs = level.getEntitiesOfClass(Curbug.class, area).size();
        int rippers = level.getEntitiesOfClass(Ripper.class, area).size();
        if (curbugs + rippers < REQUIRED_KINDS) {
            return false;
        }
        BlockPos pos = ripper.blockPosition();
        ripper.discard();
        level.setBlock(pos, ModBlocks.HIVE_NODE.get().defaultBlockState(), 3);
        return true;
    }
}
