package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;

import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber
public class EvolutionStageEvents {
    private static final Random RANDOM = new Random();
    
    public static boolean isParasite(LivingEntity entity) {
        return IParasite.isParasiteByTagOrInterface(entity);
    }

    private static boolean isNotParasiteAndNotCreeper(LivingEntity entity) {
        return !isParasite(entity) && !(entity instanceof Creeper);
    }

    
    public static void applyCothEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 0)); 
    }

    
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        int stage = EvolutionManager.getStageForDimension(level);

        if (stage >= 3 && entity.hasEffect(ModEffects.COTH.get())) {
            event.setCanceled(true); 
        }
    }

    
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;

        int stage = EvolutionManager.getStageForDimension(level);

        
        if (stage >= 1 && stage <= 5) {
            if (RANDOM.nextDouble() < 0.10) {
                event.setCanceled(true);
                event.damageRodBy(1);

                
                Entity fishHook = event.getHookEntity(); 
                Fins fins = new Fins(ModEntities.FINS.get(), level);
                fins.setPos(fishHook.getX(), fishHook.getY(), fishHook.getZ()); 
                level.addFreshEntity(fins);

                
                LivingEntity target = findNearestTarget(fins, 12.0);
                if (target != null) {
                    fins.setTarget(target);
                    try {
                        Method startSprintMethod = Fins.class.getDeclaredMethod("startSprint");
                        startSprintMethod.setAccessible(true);
                        startSprintMethod.invoke(fins);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

            if (RANDOM.nextDouble() < 0.05) {
                event.setCanceled(true);
                event.damageRodBy(1);

                
                Entity fishHook = event.getHookEntity(); 
                Fins fins = new Fins(ModEntities.INFESTED_DROWNED.get(), level);
                fins.setPos(fishHook.getX(), fishHook.getY(), fishHook.getZ()); 
                level.addFreshEntity(fins);
                return;
            }
        }

        
        if (stage >= 4) {
            event.setCanceled(true);
            event.damageRodBy(1);
        }
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Level level = (Level) event.getLevel();
        int stage = EvolutionManager.getStageForDimension(level);

        if (event.getEntity() instanceof LivingEntity entity) {

            if (stage >= 11 && stage <= 13 && !isParasite(entity)) {
                applyCothByStage(entity, stage);
                return; 
            }

            if (stage == 10 && !isParasite(entity)) {
                applyCothEffect(entity); 
                return;
            }

            
            if (stage >= 6 && stage <= 9 && !isParasite(entity)) {
                double chance = 0.0;
                if (stage == 6) chance = 0.20;
                else if (stage == 7) chance = 0.40;
                else if (stage == 8) chance = 0.60;
                else if (stage == 9) chance = 0.90;
                if (RANDOM.nextDouble() < chance) {
                    applyCothEffect(entity);
                }
            }
        }
    }

    
    private static void applyCothByStage(LivingEntity entity, int stage) {
        int effectLevel = 0; 
        if (stage == 11) {
            if (RANDOM.nextDouble() < 0.5) {
                effectLevel = 1; 
            } else {
                effectLevel = 0; 
            }
        } else if (stage == 12) {
            if (RANDOM.nextDouble() < 0.75) {
                effectLevel = 2; 
            } else {
                effectLevel = 1; 
            }
        } else if (stage == 13) {
            effectLevel = 3; 
        }
        entity.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, effectLevel));
    }

    
    private static LivingEntity findNearestTarget(Fins fins, double radius) {
        List<LivingEntity> list = fins.level().getEntitiesOfClass(LivingEntity.class,
                fins.getBoundingBox().inflate(radius),
                EvolutionStageEvents::isNotParasiteAndNotCreeper);
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(fins)));
        return list.isEmpty() ? null : list.get(0);
    }

    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity source = event.getSource().getEntity();
        if (source != null && IParasite.isParasiteNoLivingByTagOrInterface(source)) {
            ServerLevel level = (ServerLevel) source.level();
            EvolutionManager manager = EvolutionManager.forDimension(level);
            double multiplier = manager.getParasiteDamageMultiplier();
            if (multiplier > 1.0) {
                event.setAmount((float) (event.getAmount() * multiplier));
            }
        }
    }
}