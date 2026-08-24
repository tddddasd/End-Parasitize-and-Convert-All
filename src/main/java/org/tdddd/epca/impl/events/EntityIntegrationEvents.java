package org.tdddd.epca.impl.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.data.EntityIntegrationManager;
import org.tdddd.epca.impl.overworld.data.EntityIntegrationManager.EntityIntegrationRule;
import org.tdddd.epca.impl.overworld.data.EntityIntegrationManager.EntityRequirement;
import org.tdddd.epca.impl.overworld.data.EntityKillCountManager;

import java.util.*;

@Mod.EventBusSubscriber
public class EntityIntegrationEvents {

    private static final int CHECK_INTERVAL = 5; 
    private static int checkCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EntityIntegrationManager.tick();

            checkCounter++;
            if (checkCounter >= CHECK_INTERVAL) {
                checkCounter = 0;
                checkAllWorldsForIntegrations(event.getServer());
            }
        }
    }

    private static void checkAllWorldsForIntegrations(MinecraftServer server) {
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            checkWorldForIntegrations(level);
        }
    }

    private static void checkWorldForIntegrations(ServerLevel level) {
        
        List<Mob> allEntities = level.getEntitiesOfClass(
                Mob.class,
                level.getWorldBorder().getCollisionShape().bounds(),
                entity -> entity != null && entity.isAlive()
        );

        
        for (Mob entity : allEntities) {
            checkEntityForAllIntegrationRules(level, entity);
        }
    }

    
    private static void checkEntityForAllIntegrationRules(ServerLevel level, Mob centerEntity) {
        
        Map<String, List<EntityIntegrationRule>> allRules = EntityIntegrationManager.getAllIntegrationRules();

        for (List<EntityIntegrationRule> ruleList : allRules.values()) {
            for (EntityIntegrationRule rule : ruleList) {
                checkEntityForIntegrationRule(level, centerEntity, rule);
            }
        }
    }

    
    private static void checkEntityForIntegrationRule(ServerLevel level, Mob centerEntity, EntityIntegrationRule rule) {
        
        if (!isEntityInRule(centerEntity, rule)) {
            return;
        }

        
        double searchRange = rule.range;
        List<Mob> nearbyEntities = level.getEntitiesOfClass(
                Mob.class,
                centerEntity.getBoundingBox().inflate(searchRange),
                entity -> entity != centerEntity &&
                        entity.isAlive() &&
                        isValidForRule(entity, rule)
        );

        
        List<Mob> allEntities = new ArrayList<>();
        allEntities.add(centerEntity);
        allEntities.addAll(nearbyEntities);

        
        if (isEntityGroupValidForRule(allEntities, rule)) {
            EntityIntegrationManager.checkIntegration(level, allEntities);
        }
    }

    
    private static boolean isEntityInRule(Mob entity, EntityIntegrationRule rule) {
        String entityId = getEntityId(entity);

        for (EntityRequirement requirement : rule.entities) {
            if (requirement.entity.equals(entityId)) {
                return true;
            }
        }
        return false;
    }

    
    private static boolean isEntityGroupValidForRule(List<Mob> entities, EntityIntegrationRule rule) {
        
        List<EntityRequirement> requirements = new ArrayList<>(rule.entities);
        List<Mob> remainingEntities = new ArrayList<>(entities);

        
        for (EntityRequirement requirement : requirements) {
            int matchedCount = 0;
            int requiredCount = requirement.minCount != null ? requirement.minCount : 1;

            
            Iterator<Mob> iterator = remainingEntities.iterator();
            while (iterator.hasNext()) {
                Mob entity = iterator.next();
                if (EntityIntegrationManager.matchesEntityRequirement(entity, requirement)) {
                    matchedCount++;
                    iterator.remove(); 

                    if (matchedCount >= requiredCount) {
                        break; 
                    }
                }
            }

            
            if (matchedCount < requiredCount) {
                return false;
            }
        }

        
        return remainingEntities.isEmpty();
    }

    
    private static boolean isValidForRule(Mob entity, EntityIntegrationRule rule) {
        if (entity == null || !entity.isAlive()) return false;

        
        return isEntityInRule(entity, rule);
    }

    
    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob &&
                mob.level() instanceof ServerLevel serverLevel) {

            
            String entityId = getEntityId(mob);
            if (entityId.startsWith("epca:")) {
                
                if (serverLevel.getGameTime() % 5 == 0) {
                    checkEntityForAllIntegrationRules(serverLevel, mob);
                }
            }
        }
    }

    
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob killed &&
                killed.level() instanceof ServerLevel serverLevel) {

            
            if (event.getSource().getEntity() instanceof Mob killer) {
                
                EntityKillCountManager.incrementKillCount(killer);
                
                if (EntityKillCountManager.isKillCountMaxed(killer)) {
                    checkEntityForAllIntegrationRules(serverLevel, killer);
                }
            }
        }
    }

    
    private static String getEntityId(Mob entity) {
        return ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
    }
}