package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.InfestedBlockManager;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfestedBlockEventHandler {
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        BlockState state = event.getState();
        if (state.getBlock() instanceof InfestedBlockInterface) {
            BlockPos pos = event.getPos();
            InfestedBlockManager.addInfestedBlock(serverLevel, pos);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        BlockState state = event.getState();
        if (state.getBlock() instanceof InfestedBlockInterface) {
            BlockPos pos = event.getPos();
            InfestedBlockManager.removeInfestedBlock(serverLevel, pos);
        }
    }
}