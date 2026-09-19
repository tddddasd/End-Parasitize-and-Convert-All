package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.InfestedBlockManager;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

@EventBusSubscriber(modid = epca.MODID)
public class InfestedBlockEventHandler {
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        BlockState state = event.getState();
        if (state.getBlock() instanceof InfestedBlockInterface) {
            BlockPos pos = event.getPos();
            InfestedBlockManager.addInfestedBlock(serverLevel, pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        BlockState state = event.getState();
        if (state.getBlock() instanceof InfestedBlockInterface) {
            BlockPos pos = event.getPos();
            InfestedBlockManager.removeInfestedBlock(serverLevel, pos);
        }
    }
}
