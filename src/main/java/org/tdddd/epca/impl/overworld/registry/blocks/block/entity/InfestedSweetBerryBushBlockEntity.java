package org.tdddd.epca.impl.overworld.registry.blocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSweetBerryBush;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.List;
import java.util.UUID;

public class InfestedSweetBerryBushBlockEntity extends BlockEntity {
    public InfestedSweetBerryBushBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFESTED_SWEET_BERRY_BUSH.get(), pos, state);
    }

    private UUID targetUUID;
    private int attractCooldown = 0; 

    public static void tick(Level level, BlockPos pos, BlockState state, InfestedSweetBerryBushBlockEntity entity) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        
        if (state.getValue(InfestedSweetBerryBush.AGE) < 3) {
            entity.targetUUID = null;
            return;
        }

        
        if (entity.targetUUID != null) {
            Entity target = serverLevel.getEntity(entity.targetUUID);
            if (target == null || !target.isAlive() || !(target instanceof Animal)) {
                entity.targetUUID = null;
                return;
            }

            double dist = target.distanceToSqr(Vec3.atCenterOf(pos));
            if (dist <= 1.5 * 1.5) { 
                
                serverLevel.setBlock(pos, state.setValue(InfestedSweetBerryBush.AGE, 0), 3);
                serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (target instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 200, 0)); 
                }
                entity.targetUUID = null;
                return;
            } else if (dist > 24 * 24) { 
                entity.targetUUID = null;
            }
            return;
        }

        
        if (entity.attractCooldown > 0) {
            entity.attractCooldown--;
            return;
        }

        
        AABB box = new AABB(pos).inflate(12.0);
        List<Animal> animals = serverLevel.getEntitiesOfClass(Animal.class, box,
                animal -> !(animal instanceof IParasite) && animal.isAlive());
        if (!animals.isEmpty()) {
            Animal chosen = null;
            double minDist = Double.MAX_VALUE;
            for (Animal a : animals) {
                double d = a.distanceToSqr(Vec3.atCenterOf(pos));
                if (d < minDist) {
                    minDist = d;
                    chosen = a;
                }
            }
            if (chosen != null) {
                
                chosen.getNavigation().moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0);
                entity.targetUUID = chosen.getUUID();
                entity.attractCooldown = 20; 
            }
        } else {
            entity.attractCooldown = 40; 
        }
    }

    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("Target")) {
            targetUUID = tag.getUUID("Target");
        }
        attractCooldown = tag.getInt("Cooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (targetUUID != null) {
            tag.putUUID("Target", targetUUID);
        }
        tag.putInt("Cooldown", attractCooldown);
    }
}