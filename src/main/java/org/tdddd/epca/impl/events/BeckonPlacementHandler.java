package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.BiomassSyncPacket;
import org.tdddd.epca.impl.overworld.data.BiomassManager;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.block.BeckonCore;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeckonPlacementHandler {
    private static final long COOLDOWN_TICKS = 60 * 20;
    private static final int BECKON_COST = 15;
    private static final int ADD_KILL_COST = 1;

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;

        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;

        if (!player.getMainHandItem().isEmpty()) return;

        if (!player.getPersistentData().getBoolean("VKeyPressed")) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (!(block instanceof BeckonCore)) {
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness <= 0 || hardness > 4) return;
            if (!state.isCollisionShapeFullBlock(level, pos)) return;

            // 检查生物质点数
            int points = BiomassManager.getBiomassPoints(player);
            if (points < BECKON_COST) {
                player.displayClientMessage(Component.literal("需" + BECKON_COST + "生物质点数"), true);
                return;
            }

            long lastPlace = player.getPersistentData().getLong("BeckonPlaceCooldown");
            long now = level.getGameTime();
            if (now - lastPlace < COOLDOWN_TICKS) {
                return;
            }

            level.setBlock(pos, ModBlocks.BECKON_CORE.get().defaultBlockState(), 3);

            BiomassManager.addBiomassPoints(player, -BECKON_COST);
            ModNetwork.sendToPlayer((ServerPlayer) player,
                    new BiomassSyncPacket(true, BiomassManager.getBiomassPoints(player)));

            player.getPersistentData().putLong("BeckonPlaceCooldown", now);
            player.displayClientMessage(Component.literal("已放置召唤柱核心"), true);
            event.setCanceled(true);
            return;
        }

        if (block instanceof BeckonCore) {
            int points = BiomassManager.getBiomassPoints(player);
            if (points < ADD_KILL_COST) {
                return;
            }

            if (level.getBlockEntity(pos) instanceof BeckonCoreBlockEntity be) {
                be.addKillCount(1);
                BiomassManager.addBiomassPoints(player, -ADD_KILL_COST);
                ModNetwork.sendToPlayer((ServerPlayer) player,
                        new BiomassSyncPacket(true, BiomassManager.getBiomassPoints(player)));
                event.setCanceled(true);
            }
        }
    }
}