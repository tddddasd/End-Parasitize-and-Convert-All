package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import org.tdddd.epca.impl.overworld.data.EntityKillCountManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.EnumSet;

public class GoToBeckonCoreGoal extends Goal {
    private final Mob entity;
    private BlockPos corePos;
    private int searchCooldown = 0;

    public GoToBeckonCoreGoal(Mob entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(entity instanceof IParasite)) return false;
        
        if (EntityKillCountManager.getCurrentKillCount(entity) <= 0) return false;

        if (--searchCooldown > 0) return false;
        searchCooldown = 10; 

        Level level = entity.level();
        
        BlockPos entityPos = entity.blockPosition();
        double nearestDistSq = 48 * 48; 
        BlockPos nearestCore = null;

        
        for (int dx = -48; dx <= 48; dx++) {
            for (int dz = -48; dz <= 48; dz++) {
                for (int dy = -16; dy <= 16; dy++) {
                    BlockPos checkPos = entityPos.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).getBlock() == ModBlocks.BECKON_CORE.get()) {
                        double dist = entityPos.distSqr(checkPos);
                        if (dist < nearestDistSq) {
                            
                            if (level.getBlockEntity(checkPos) instanceof BeckonCoreBlockEntity be) {
                                if (!be.isGenerating() && be.getKillCount() < 128) { 
                                    nearestDistSq = dist;
                                    nearestCore = checkPos;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (nearestCore != null) {
            this.corePos = nearestCore;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        if (corePos != null) {
            Vec3 target = Vec3.atBottomCenterOf(corePos);
            entity.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }
    }

    @Override
    public void tick() {
        if (corePos == null) return;
        
        if (entity.distanceToSqr(Vec3.atCenterOf(corePos)) <= 4.0) {
            
            entity.getNavigation().stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (corePos == null) return false;
        
        if (EntityKillCountManager.getCurrentKillCount(entity) <= 0) return false;
        Level level = entity.level();
        if (!(level.getBlockEntity(corePos) instanceof BeckonCoreBlockEntity be)) return false;
        if (be.isGenerating() || be.getKillCount() >= 64) return false; 
        
        if (entity.distanceToSqr(Vec3.atCenterOf(corePos)) > 48*48) return false;
        return true;
    }

    @Override
    public void stop() {
        corePos = null;
        entity.getNavigation().stop();
    }
}