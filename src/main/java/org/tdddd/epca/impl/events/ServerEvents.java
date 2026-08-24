package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        // 只在服务端执行（PlaceEvent 默认在服务端触发，但为防止意外检查）
        if (event.getLevel().isClientSide()) return;

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        // 检查旧方块（放置前的方块）
        BlockState oldState = level.getBlockState(pos);
        if (oldState.getBlock() instanceof InfestedBlockInterface) {
            sendRemovePacket(level, pos);
        }

        // 检查新放置的方块
        BlockState newState = event.getPlacedBlock();
        if (newState.getBlock() instanceof InfestedBlockInterface) {
            sendAddPacket(level, pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockState state = event.getState();
        if (state.getBlock() instanceof InfestedBlockInterface) {
            sendRemovePacket((Level) event.getLevel(), event.getPos());
        }
    }

    // 发送添加包（仅服务端调用）
    private static void sendAddPacket(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        ModNetwork.INSTANCE.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new InfestedSourcePacket.AddInfestedSourcePacket(pos));
        // 若想优化范围，可使用 PacketDistributor.NEAR
    }

    // 发送移除包
    private static void sendRemovePacket(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        ModNetwork.INSTANCE.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                new InfestedSourcePacket.RemoveInfestedSourcePacket(pos));
    }
}