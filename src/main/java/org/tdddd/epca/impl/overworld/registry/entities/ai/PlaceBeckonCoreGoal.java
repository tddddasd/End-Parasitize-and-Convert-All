package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.data.EntityKillCountManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIBeckon;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIIBeckon;

import java.util.EnumSet;
import java.util.List;

public class PlaceBeckonCoreGoal extends Goal {
    private final Mob entity;
    private BlockPos targetPos;
    private int checkCooldown = 0;
    private boolean isNearby = false; 

    private static final int KILLS_REQUIRED = 15;
    private static final int SEARCH_RADIUS = 16;
    private static final int NEARBY_RADIUS = 1; 

    public PlaceBeckonCoreGoal(Mob entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(entity instanceof IParasite)) return false;
        int kills = EntityKillCountManager.getCurrentKillCount(entity);
        if (kills < KILLS_REQUIRED) return false;

        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }
        checkCooldown = 20;

        Level level = entity.level();
        BlockPos entityPos = entity.blockPosition();

        
        if (isBeckonCoreNearby(level, entityPos)) return false;
        if (isBeckonEntityNearby(level, entityPos)) return false;

        
        BlockPos nearby = findNearbyBlock(level, entityPos);
        if (nearby != null) {
            this.targetPos = nearby;
            this.isNearby = true;
            return true;
        }

        
        this.targetPos = findSuitableBlock(level, entityPos);
        this.isNearby = false;
        return targetPos != null;
    }

    
    private BlockPos findNearbyBlock(Level level, BlockPos center) {
        for (int dx = -NEARBY_RADIUS; dx <= NEARBY_RADIUS; dx++) {
            for (int dy = -NEARBY_RADIUS; dy <= NEARBY_RADIUS; dy++) {
                for (int dz = -NEARBY_RADIUS; dz <= NEARBY_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (isValidBlock(level, pos, state)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    
    private BlockPos findSuitableBlock(Level level, BlockPos center) {
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (isValidBlock(level, pos, state)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidBlock(Level level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > 4) return false;
        if (!state.isCollisionShapeFullBlock(level, pos)) return false;

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.isCollisionShapeFullBlock(level, below)) return false;

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.isCollisionShapeFullBlock(level, above)) return false;

        return true;
    }

    
    private boolean isBeckonCoreNearby(Level level, BlockPos center) {
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -16; dy <= 16; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos checkPos = center.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).getBlock() == ModBlocks.BECKON_CORE.get()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    
    private boolean isBeckonEntityNearby(Level level, BlockPos center) {
        AABB area = new AABB(center).inflate(SEARCH_RADIUS);
        List<StageIBeckon> stageIList = level.getEntitiesOfClass(StageIBeckon.class, area, e -> e.isAlive());
        if (!stageIList.isEmpty()) return true;
        List<StageIIBeckon> stageIIList = level.getEntitiesOfClass(StageIIBeckon.class, area, e -> e.isAlive());
        return !stageIIList.isEmpty();
    }

    @Override
    public void start() {
        if (targetPos == null) return;
        if (!isNearby) {
            
            Vec3 pos = Vec3.atBottomCenterOf(targetPos);
            entity.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.0);
        }
        
    }

    @Override
    public void tick() {
        if (targetPos == null) return;

        
        if (isNearby || entity.distanceToSqr(Vec3.atCenterOf(targetPos)) < 4.0) {
            Level level = entity.level();
            BlockState state = level.getBlockState(targetPos);
            if (isValidBlock(level, targetPos, state)) {
                level.setBlock(targetPos, ModBlocks.BECKON_CORE.get().defaultBlockState(), 3);
                EntityKillCountManager.setKillCount(entity,
                        EntityKillCountManager.getCurrentKillCount(entity) - 15);
            }
            targetPos = null;
            isNearby = false;
        }
        
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPos == null) return false;
        
        if (isNearby) return true;
        
        return entity.getNavigation().isInProgress();
    }

    @Override
    public void stop() {
        targetPos = null;
        isNearby = false;
        entity.getNavigation().stop();
    }
}