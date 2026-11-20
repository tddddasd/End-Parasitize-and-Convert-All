package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedResidue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class FallingBlockMergeHandler {

    private static final double SEARCH_RADIUS = 64.0;

    /**
     * Loaded falling residue blocks per level. The merge scan used to run an entity query over a
     * 128x128x128 box for every player on every level tick, i.e. over 512 entity sections per player
     * per tick even when nothing was falling. Tracking the entities makes the common case (no
     * residue falling anywhere) a single counter check.
     */
    private static final Map<Level, int[]> TRACKED = new IdentityHashMap<>();

    private static int[] counter(Level level) {
        return TRACKED.computeIfAbsent(level, key -> new int[]{0});
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof FallingBlockEntity falling)) {
            return;
        }
        BlockState state = falling.getBlockState();
        if (state != null && state.getBlock() instanceof InfestedResidue) {
            counter((Level) event.getLevel())[0]++;
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof FallingBlockEntity falling)) {
            return;
        }
        BlockState state = falling.getBlockState();
        if (state != null && state.getBlock() instanceof InfestedResidue) {
            int[] counter = TRACKED.get(event.getLevel());
            if (counter != null && counter[0] > 0) {
                counter[0]--;
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {

        if (event.side.isClient()) return;
        if (event.phase != TickEvent.Phase.END) return;

        Level level = event.level;
        if (level == null) return;

        int[] counter = TRACKED.get(level);
        if (counter == null || counter[0] <= 0) {
            return;
        }

        List<? extends Player> players = level.players();
        if (players.isEmpty()) return;

        Set<FallingBlockEntity> processed = new HashSet<>();
        int found = 0;

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

                found++;

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

        // resynchronise the counter when the tracked entities are gone (e.g. removed without an
        // unload event); the next tick then takes the cheap early out again
        if (found == 0 && counter[0] > 0) {
            counter[0] = 0;
        }
    }
}
