package org.tdddd.epca.impl.overworld.registry.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.CothEffect;

import java.util.List;
import java.util.stream.Collectors;

public interface IInfested {
    
    
    default int getBreakCooldown() {
        return 25;
    }

    
    default boolean isIncompleteLightSource(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SOUL_FIRE ||
                block == Blocks.FIRE ||
                block == Blocks.TORCH ||
                block == Blocks.WALL_TORCH ||
                block == Blocks.LANTERN ||
                block == Blocks.SOUL_TORCH ||
                block == Blocks.SOUL_WALL_TORCH ||
                block == Blocks.SOUL_LANTERN ||
                block == Blocks.BLACK_BED ||
                block == Blocks.BLUE_BED ||
                block == Blocks.BROWN_BED ||
                block == Blocks.CYAN_BED ||
                block == Blocks.GRAY_BED ||
                block == Blocks.GREEN_BED ||
                block == Blocks.LIGHT_BLUE_BED ||
                block == Blocks.LIGHT_GRAY_BED ||
                block == Blocks.LIME_BED ||
                block == Blocks.MAGENTA_BED ||
                block == Blocks.ORANGE_BED ||
                block == Blocks.PINK_BED ||
                block == Blocks.PURPLE_BED ||
                block == Blocks.RED_BED ||
                block == Blocks.WHITE_BED ||
                block == Blocks.GLASS_PANE ||
                block == Blocks.YELLOW_BED ||
                block == Blocks.WHITE_STAINED_GLASS_PANE ||
                block == Blocks.ORANGE_STAINED_GLASS_PANE ||
                block == Blocks.MAGENTA_STAINED_GLASS_PANE ||
                block == Blocks.LIGHT_BLUE_STAINED_GLASS_PANE ||
                block == Blocks.YELLOW_STAINED_GLASS_PANE ||
                block == Blocks.LIME_STAINED_GLASS_PANE ||
                block == Blocks.PINK_STAINED_GLASS_PANE ||
                block == Blocks.GRAY_STAINED_GLASS_PANE ||
                block == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE ||
                block == Blocks.CYAN_STAINED_GLASS_PANE ||
                block == Blocks.PURPLE_STAINED_GLASS_PANE ||
                block == Blocks.BLUE_STAINED_GLASS_PANE ||
                block == Blocks.BROWN_STAINED_GLASS_PANE ||
                block == Blocks.GREEN_STAINED_GLASS_PANE ||
                block == Blocks.RED_STAINED_GLASS_PANE ||
                block == Blocks.BLACK_STAINED_GLASS_PANE ||
                block == Blocks.OAK_LEAVES ||
                block == Blocks.SPRUCE_LEAVES ||
                block == Blocks.BIRCH_LEAVES ||
                block == Blocks.JUNGLE_LEAVES ||
                block == Blocks.ACACIA_LEAVES ||
                block == Blocks.DARK_OAK_LEAVES ||
                block == Blocks.OAK_DOOR ||
                block == Blocks.DARK_OAK_DOOR ||
                block == Blocks.ACACIA_DOOR ||
                block == Blocks.CRIMSON_DOOR ||
                block == Blocks.WARPED_DOOR ||
                block == Blocks.MANGROVE_DOOR ||
                block == Blocks.CHERRY_DOOR ||
                block == Blocks.BAMBOO_DOOR;
    }


    /**
     * 破坏自身碰撞箱范围内的光源方块。
     */
    default int tryBreakLightSources(Mob mob, int currentCooldown) {
        if (currentCooldown > 0) {
            return currentCooldown - 1;
        }

        // 保存原始碰撞箱（未扩展）
        AABB originalBox = mob.getBoundingBox();

        // 获取搜索范围内所有方块位置
        List<BlockPos> blockPositions = BlockPos.betweenClosedStream(originalBox)
                .map(BlockPos::immutable)
                .collect(Collectors.toList());

        for (BlockPos pos : blockPositions) {
            if (mob.level().isLoaded(pos)) {
                BlockState state = mob.level().getBlockState(pos);
                if (isIncompleteLightSource(state)) {
                    mob.level().destroyBlock(pos, true, mob);
                    return getBreakCooldown();
                }
            }
        }

        return 0;
    }

    
    default int tryApplyCothEffect(Mob mob, int currentCooldown) {
        if (currentCooldown > 0) {
            return currentCooldown - 1;
        }

        
        Vec3 mobPos = mob.position();
        AABB effectArea = new AABB(
                mobPos.x - 3,
                mobPos.y - 3,
                mobPos.z - 3,
                mobPos.x + 3,
                mobPos.y + 3,
                mobPos.z + 3
        );

        
        List<LivingEntity> nearbyEntities = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                effectArea,
                entity -> entity != mob 
        );

        
        for (LivingEntity entity : nearbyEntities) {
            CothEffect.applyCothEffect(entity, 3600, 0);
        }

        return 100; 
    }
}