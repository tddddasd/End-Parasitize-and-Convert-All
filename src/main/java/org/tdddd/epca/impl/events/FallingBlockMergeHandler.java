package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedResidue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = epca.MODID)
public class FallingBlockMergeHandler {

    private static final double SEARCH_RADIUS = 64.0; 

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        

        Level level = event.getLevel();
        if (level == null) return;

        
        List<? extends Player> players = level.players();
        if (players.isEmpty()) return;

        
        Set<FallingBlockEntity> processed = new HashSet<>();

        for (Player player : players) {
            
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
                    entity -> true 
            );

            for (FallingBlockEntity falling : fallingBlocks) {
                
                if (!processed.add(falling)) continue;

                
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