package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.fluid.AcidDamageSystem;
import org.tdddd.epca.impl.fluid.AcidSolutionBlock;

import java.util.Set;

@Mod.EventBusSubscriber
public class AcidBlockListener {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        if (state.getBlock() instanceof AcidSolutionBlock) {
            
            Set<BlockPos> affectedWaters = WaterInteractionHandler.getAcidifiedWaterBlocks(level, pos, 8);

            
            AcidDamageSystem.registerAcidDamageArea(level, pos, affectedWaters);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (state.getBlock() instanceof AcidSolutionBlock) {
            
            Set<BlockPos> affectedWaters = WaterInteractionHandler.getAcidifiedWaterBlocks(level, pos, 8);

            
            AcidDamageSystem.removeAcidDamageArea(level, pos, affectedWaters);
        }
    }
}