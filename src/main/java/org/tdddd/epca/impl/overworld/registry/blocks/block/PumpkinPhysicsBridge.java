package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * The only class in epca that touches {@code epca_physics}.
 *
 * <p>Keeping the physics references here means {@link org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedCarvedPumpkin}
 * never links against {@code epca_physics}, so epca still loads when Valkyrien Skies is absent. The
 * calls below are static calls on another class, which the JVM resolves at execution time, and they
 * only run after {@link #isAvailable()} returns true.</p>
 */
public final class PumpkinPhysicsBridge {

    private static final String PHYSICS_MOD = "epca_physics";
    private static final String VS_MOD = "valkyrienskies";

    private PumpkinPhysicsBridge() {
    }

    /** True when both the physics addon and Valkyrien Skies are present. */
    public static boolean isAvailable() {
        var modList = net.minecraftforge.fml.ModList.get();
        return modList.isLoaded(PHYSICS_MOD) && modList.isLoaded(VS_MOD);
    }

    public static boolean hasSilkTouch(net.minecraft.world.entity.player.Player player) {
        return PumpkinPhysicsHelper.hasSilkTouch(player);
    }

    public static void markSilkTouchBreak(Level level, BlockPos pos) {
        PumpkinPhysicsHelper.markSilkTouchBreak(level, pos);
    }

    public static boolean wasSilkTouchBreak(Level level, BlockPos pos) {
        return PumpkinPhysicsHelper.wasSilkTouchBreak(level, pos);
    }

    public static void spawnPhysicsPumpkin(Level level, BlockPos pos, BlockState state) {
        PumpkinPhysicsHelper.spawnPhysicsPumpkin(level, pos, state);
    }
}
