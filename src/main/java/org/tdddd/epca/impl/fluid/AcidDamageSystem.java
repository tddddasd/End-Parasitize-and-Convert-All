package org.tdddd.epca.impl.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

import java.util.*;

public class AcidDamageSystem {
    private static final Map<Level, Map<BlockPos, AcidDamageData>> levelDamageData = new HashMap<>();

    public static class AcidDamageData {
        public final BlockPos acidSourcePos;
        public final int distance;
        public final long createdTick;

        public AcidDamageData(BlockPos acidSourcePos, int distance, long createdTick) {
            this.acidSourcePos = acidSourcePos;
            this.distance = distance;
            this.createdTick = createdTick;
        }
    }

    
    public static void registerAcidDamageArea(Level level, BlockPos acidPos, Set<BlockPos> waterPositions) {
        if (level.isClientSide) return;

        Map<BlockPos, AcidDamageData> damageMap = levelDamageData.computeIfAbsent(
                level, k -> new HashMap<>()
        );

        for (BlockPos waterPos : waterPositions) {
            int distance = calculateDistance(acidPos, waterPos);
            damageMap.put(waterPos, new AcidDamageData(acidPos, distance, level.getGameTime()));
        }
    }

    
    public static void removeAcidDamageArea(Level level, BlockPos acidPos, Set<BlockPos> waterPositions) {
        if (level.isClientSide) return;

        Map<BlockPos, AcidDamageData> damageMap = levelDamageData.get(level);
        if (damageMap == null) return;

        for (BlockPos waterPos : waterPositions) {
            damageMap.remove(waterPos);
        }

        
        if (damageMap.isEmpty()) {
            levelDamageData.remove(level);
        }
    }

    
    public static boolean hasAcidDamageAt(Level level, BlockPos pos) {
        Map<BlockPos, AcidDamageData> damageMap = levelDamageData.get(level);
        if (damageMap == null) return false;

        return damageMap.containsKey(pos);
    }

    
    public static AcidDamageData getDamageDataAt(Level level, BlockPos pos) {
        Map<BlockPos, AcidDamageData> damageMap = levelDamageData.get(level);
        if (damageMap == null) return null;

        return damageMap.get(pos);
    }

    
    public static void applyAcidWaterDamage(LivingEntity entity, Level level, BlockPos pos) {
        AcidDamageData data = getDamageDataAt(level, pos);
        if (data == null) return;

        long currentTick = level.getGameTime();

        
        if (currentTick % 11 == 0) {
            
            float distanceFactor = 0.1f * (Math.min(data.distance, 8) / 8.0f);

            
            entity.hurt(entity.damageSources().magic(), 1.0F);

            Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
            DamageSource minimumSource = new DamageSource(holder);
            entity.hurt(minimumSource, 0.5F + distanceFactor);

            
            addAcidWaterEffects(entity, data.distance);
        }
    }

    
    private static void addAcidWaterEffects(LivingEntity entity, int distance) {
        
        
        entity.addEffect(new MobEffectInstance(
                ModEffects.CORROSIVE.get(),
                100, 
                0,   
                false,
                true
        ));

        if (distance <= 4) {
            entity.addEffect(new MobEffectInstance(
                    ModEffects.COTH.get(),
                    600, 
                    1,   
                    false,
                    true
            ));
        } else {
            entity.addEffect(new MobEffectInstance(
                    ModEffects.COTH.get(),
                    600, 
                    0,  
                    false,
                    true
            ));
        }
    }

    
    public static void cleanupOldData(Level level) {
        Map<BlockPos, AcidDamageData> damageMap = levelDamageData.get(level);
        if (damageMap == null) return;

        long currentTick = level.getGameTime();
        long threshold = currentTick - 6000; 

        
        damageMap.entrySet().removeIf(entry -> {
            long createdTick = entry.getValue().createdTick;
            return createdTick < threshold;
        });

        
        if (damageMap.isEmpty()) {
            levelDamageData.remove(level);
        }
    }

    
    private static int calculateDistance(BlockPos pos1, BlockPos pos2) {
        return Math.abs(pos1.getX() - pos2.getX()) +
                Math.abs(pos1.getY() - pos2.getY()) +
                Math.abs(pos1.getZ() - pos2.getZ());
    }

    
    public static Set<BlockPos> getAllDamagePositions(Level level) {
        Map<BlockPos, AcidDamageData> damageMap = levelDamageData.get(level);
        if (damageMap == null) return Collections.emptySet();

        return damageMap.keySet();
    }

    
    public static boolean isWaterInAcidRange(Level level, BlockPos waterPos, int range) {
        
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > range) continue;

                    BlockPos checkPos = waterPos.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).getBlock() instanceof AcidSolutionBlock) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}