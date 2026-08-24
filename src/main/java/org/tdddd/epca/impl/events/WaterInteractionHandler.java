package org.tdddd.epca.impl.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.fluid.AcidDamageSystem;
import org.tdddd.epca.impl.fluid.AcidSolutionBlock;

import java.util.*;

@Mod.EventBusSubscriber
public class WaterInteractionHandler {
    private static final int DAMAGE_INTERVAL = 10; 

    
    private static final Map<UUID, Long> lastDamageTickMap = new HashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        
        if (level.isClientSide) return;

        
        if (entity.isInWater()) {
            BlockPos entityPos = entity.blockPosition();

            
            if (shouldSkipEntity(entity)) return;

            
            if (AcidDamageSystem.hasAcidDamageAt(level, entityPos)) {
                
                AcidDamageSystem.applyAcidWaterDamage(entity, level, entityPos);
            } else if (AcidDamageSystem.isWaterInAcidRange(level, entityPos, 8)) {
                
                registerAcidWaterIfNeeded(level, entityPos);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide()) {
            Level level = event.level;

            
            if (level.getGameTime() % 600 == 0) {
                AcidDamageSystem.cleanupOldData(level);
            }
        }
    }

    private static boolean shouldSkipEntity(LivingEntity entity) {
        
        if (IParasite.isParasiteByTagOrInterface(entity)) return true;

        
        if (entity instanceof Player player) {
            return player.isCreative() || player.isSpectator();
        }

        return false;
    }

    private static void registerAcidWaterIfNeeded(Level level, BlockPos waterPos) {
        
        BlockPos nearestAcid = findNearestAcid(level, waterPos, 8);
        if (nearestAcid != null) {
            int distance = Math.abs(nearestAcid.getX() - waterPos.getX()) +
                    Math.abs(nearestAcid.getY() - waterPos.getY()) +
                    Math.abs(nearestAcid.getZ() - waterPos.getZ());

            
            AcidDamageSystem.registerAcidDamageArea(
                    level,
                    nearestAcid,
                    java.util.Collections.singleton(waterPos)
            );
        }
    }

    private static BlockPos findNearestAcid(Level level, BlockPos center, int range) {
        BlockPos nearest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > range) continue;

                    BlockPos checkPos = center.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).getBlock() instanceof AcidSolutionBlock) {
                        int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    private static boolean isWaterAcidified(Level level, BlockPos pos) {
        
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.getType() != Fluids.WATER &&
                fluidState.getType() != Fluids.FLOWING_WATER) {
            return false;
        }

        
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 8) continue;

                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (checkState.getBlock() instanceof AcidSolutionBlock) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void applyDamage(LivingEntity entity, Level level, BlockPos pos) {
        UUID entityId = entity.getUUID();
        long currentTick = level.getGameTime();

        
        Long lastDamageTick = lastDamageTickMap.get(entityId);
        if (lastDamageTick != null && currentTick - lastDamageTick < DAMAGE_INTERVAL) {
            return;
        }

        
        lastDamageTickMap.put(entityId, currentTick);


            
            entity.addEffect(new MobEffectInstance(
                    ModEffects.COTH.get(),
                    600, 
                    0,    
                    false,
                    true
            ));

            
            entity.addEffect(new MobEffectInstance(
                    ModEffects.CORROSIVE.get(),
                    100, 
                    0,   
                    false,
                    true
            ));

            
            int viralLevel = 0;
            MobEffectInstance viralEffect = entity.getEffect(ModEffects.VIRAL.get());
            if (viralEffect != null) {
                viralLevel = viralEffect.getAmplifier() + 1;
            }

            
            float totalDamage = 0.1f + (viralLevel + 1) / 2.0f;
            float currentHealth = entity.getHealth();
            
            entity.hurt(entity.damageSources().magic(), 1.0F);

            if (currentHealth <= totalDamage) {
                if (entity instanceof LivingEntity) {
                    entity.setHealth(0);
                    entity.die(entity.damageSources().magic());
                }
            }
    }

    
    public static Set<BlockPos> getAcidifiedWaterBlocks(Level level, BlockPos acidPos, int range) {
        Set<BlockPos> result = new HashSet<>();

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > range) continue;

                    BlockPos checkPos = acidPos.offset(dx, dy, dz);
                    FluidState fluidState = level.getFluidState(checkPos);

                    if (fluidState.getType() == Fluids.WATER ||
                            fluidState.getType() == Fluids.FLOWING_WATER) {
                        result.add(checkPos);
                    }
                }
            }
        }

        return result;
    }

    
    public static boolean isPositionAcidified(Level level, BlockPos pos) {
        return isWaterAcidified(level, pos);
    }
}