package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca_physics.structure.SubLevelRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  /
 *
 * <p>1.20.1 Forge  {@code IForgeBlock#onDestroyedByPlayer}
 * {@code Block#playerWillDestroy} {@code onRemove}
 * {@code onRemove}  {@code playerWillDestroy}
 *  {@code onRemove}
 * </p>
 *
 * <p>Phase 3 {@code PhysicsEntity}
 * {@link SubLevelRegistry#captureSingle}
 * {@link InfestedPumpkinBehaviour}  {@link #spawnPhysicsPumpkin}</p>
 */
final class PumpkinPhysicsHelper {

    /**    */
    private static final Map<Long, Long> SILK_TOUCH_BREAKS = new ConcurrentHashMap<>();

    private PumpkinPhysicsHelper() {
    }

    /**  */
    static boolean hasSilkTouch(Player player) {
        return player != null
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, player.getMainHandItem()) > 0;
    }

    /** {@code playerWillDestroy}  */
    static void markSilkTouchBreak(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        long tick = level.getGameTime();
        SILK_TOUCH_BREAKS.entrySet().removeIf(entry -> entry.getValue() != tick);
        SILK_TOUCH_BREAKS.put(pos.asLong(), tick);
    }

    /** {@code onRemove}  */
    static boolean wasSilkTouchBreak(Level level, BlockPos pos) {
        Long tick = SILK_TOUCH_BREAKS.remove(pos.asLong());
        return tick != null && tick == level.getGameTime();
    }

    /**
     *
     * {@link InfestedPumpkinBehaviour}
     *
     * <p> {@code onRemove}<b></b>
     *
     * {@code SubLevelRegistry#captureSingle}</p>
     */
    static void spawnPhysicsPumpkin(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        SubLevelRegistry.captureSingle(serverLevel, pos, state, new InfestedPumpkinBehaviour());
    }
}

