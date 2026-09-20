package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

@EventBusSubscriber(modid = epca.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        
        if (event.getLevel().isClientSide()) return;

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        
        BlockState oldState = level.getBlockState(pos);
        if (oldState.getBlock() instanceof InfestedBlockInterface) {
            sendRemovePacket(level, pos);
        }

        
        BlockState newState = event.getPlacedBlock();
        if (newState.getBlock() instanceof InfestedBlockInterface) {
            sendAddPacket(level, pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockState state = event.getState();
        if (state.getBlock() instanceof InfestedBlockInterface) {
            sendRemovePacket((Level) event.getLevel(), event.getPos());
        }
    }

    
    private static void sendAddPacket(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        ModNetwork.sendToAll(new InfestedSourcePacket.AddInfestedSourcePacket(pos));
        
    }

    
    private static void sendRemovePacket(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        ModNetwork.sendToAll(new InfestedSourcePacket.RemoveInfestedSourcePacket(pos));
    }
}
