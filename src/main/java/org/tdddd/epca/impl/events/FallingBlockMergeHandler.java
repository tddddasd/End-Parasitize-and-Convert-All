package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedResidue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class FallingBlockMergeHandler {

    private static final double SEARCH_RADIUS = 64.0; // 搜索半径（格）

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // 只在服务端且 Phase.END 阶段执行
        if (event.side.isClient()) return;
        if (event.phase != TickEvent.Phase.END) return;

        Level level = event.level;
        if (level == null) return;

        // 获取所有玩家
        List<? extends Player> players = level.players();
        if (players.isEmpty()) return;

        // 用于去重（同一实体可能同时被多个玩家的 AABB 命中）
        Set<FallingBlockEntity> processed = new HashSet<>();

        for (Player player : players) {
            // 以玩家为中心构造 AABB
            AABB searchBox = new AABB(
                    player.getX() - SEARCH_RADIUS,
                    player.getY() - SEARCH_RADIUS,
                    player.getZ() - SEARCH_RADIUS,
                    player.getX() + SEARCH_RADIUS,
                    player.getY() + SEARCH_RADIUS,
                    player.getZ() + SEARCH_RADIUS
            );

            List<FallingBlockEntity> fallingBlocks = level.getEntitiesOfClass(
                    FallingBlockEntity.class,
                    searchBox,
                    entity -> true // 获取全部，后续再过滤
            );

            for (FallingBlockEntity falling : fallingBlocks) {
                // 已经处理过的跳过
                if (!processed.add(falling)) continue;

                // 检查实体有效性
                BlockState state = falling.getBlockState();
                if (state == null || state.isAir()) continue;

                if (!(state.getBlock() instanceof InfestedResidue)) continue;

                double bottomY = falling.getY();
                BlockPos belowPos = new BlockPos(
                        (int) Math.floor(falling.getX()),
                        (int) Math.floor(bottomY - 0.01),
                        (int) Math.floor(falling.getZ())
                );

                if (!level.isInWorldBounds(belowPos)) continue;

                BlockState belowState = level.getBlockState(belowPos);
                if (!(belowState.getBlock() instanceof InfestedResidue)) continue;

                int layers = belowState.getValue(InfestedResidue.LAYERS);
                double topY = belowPos.getY() + layers * 2.0 / 16.0;

                if (bottomY - topY <= 2.0 / 16.0) {
                    int fallingLayers = state.getValue(InfestedResidue.LAYERS);
                    falling.discard();
                    InfestedResidue.mergeLayers(level, belowPos, belowState, fallingLayers);
                }
            }
        }
    }
}