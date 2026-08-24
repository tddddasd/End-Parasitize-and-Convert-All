package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.*;

public class ParasiteAttractionManager {
    private static final Map<UUID, AttractionData> ATTRACTION_MAP = new HashMap<>();
    private static final int ATTRACTION_DURATION = 20 * 60; 

    
    public static final String FLYING = "flying";
    public static final String RANGED = "ranged";
    public static final String GROUND = "ground";
    public static final String BLOCK_BREAKER = "block_breaker";

    
    private static class AttractionData {
        public long startTime;
        public final Set<UUID> attractedParasites = new HashSet<>();
        public final Set<UUID> attackers = new HashSet<>();

        public AttractionData(long startTime) {
            this.startTime = startTime;
        }
    }

    
    public static void onParasiteAttacked(LivingEntity parasite, LivingEntity attacker) {
        if (!(parasite.level() instanceof ServerLevel serverLevel)) return;
        if (!IParasite.isParasiteByTagOrInterface(parasite)) return;

        
        EvolutionManager evolutionManager = EvolutionManager.forDimension(serverLevel);
        int stage = evolutionManager.getStage();

        
        if (stage < 5) return;

        UUID attackerId = attacker.getUUID();
        UUID parasiteId = parasite.getUUID();

        
        AttractionData data = ATTRACTION_MAP.computeIfAbsent(
                attackerId, k -> new AttractionData(serverLevel.getGameTime())
        );

        
        data.attackers.add(parasiteId);
        data.startTime = serverLevel.getGameTime();

        
        int radius = evolutionManager.getAttractionRadius();

        
        AABB area = new AABB(
                attacker.getX() - radius, attacker.getY() - radius, attacker.getZ() - radius,
                attacker.getX() + radius, attacker.getY() + radius, attacker.getZ() + radius
        );

        
        List<Mob> parasites = serverLevel.getEntitiesOfClass(
                Mob.class, area, e -> IParasite.isParasiteByTagOrInterface(e) && e.isAlive()
        );

        for (LivingEntity p : parasites) {
            if (IParasite.isParasiteByTagOrInterface(p) && !data.attractedParasites.contains(p.getUUID())) {
                
                attractParasite((Mob) p, attacker, getParasiteType((Mob) p));
                data.attractedParasites.add(p.getUUID());
            }
        }
    }

    
    private static String getParasiteType(Mob parasite) {
        
        
        if (parasite.getType().getDescriptionId().contains("flying")) {
            return FLYING;
        } else if (parasite.getType().getDescriptionId().contains("ranged")) {
            return RANGED;
        } else if (parasite.getType().getDescriptionId().contains("breaker")) {
            return BLOCK_BREAKER;
        }
        return GROUND; 
    }

    
    private static void attractParasite(Mob parasite, LivingEntity target, String type) {
        
        parasite.setTarget(target);

        
        switch (type) {
            case FLYING:
                
                break;
            case RANGED:
                
                break;
            case BLOCK_BREAKER:
                
                break;
        }
    }

    
    public static void tick(ServerLevel level) {
        long currentTime = level.getGameTime();
        Iterator<Map.Entry<UUID, AttractionData>> iterator = ATTRACTION_MAP.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, AttractionData> entry = iterator.next();
            AttractionData data = entry.getValue();

            
            if (currentTime - data.startTime > ATTRACTION_DURATION) {
                iterator.remove();
                continue;
            }

            
            LivingEntity attacker = (LivingEntity) level.getEntity(entry.getKey());
            if (attacker == null || !attacker.isAlive()) {
                iterator.remove();
                continue;
            }

            
            boolean hasInteraction = checkInteractions(level, data, attacker);
            if (!hasInteraction) {
                iterator.remove();
            }
        }
    }

    
    private static boolean checkInteractions(ServerLevel level, AttractionData data, LivingEntity attacker) {
        for (UUID parasiteId : data.attackers) {
            
            LivingEntity entity = (LivingEntity) level.getEntity(parasiteId);
            if (entity instanceof Mob parasite) {
                
                if (parasite.getLastHurtByMob() == attacker) {
                    return true;
                }

                
                if (parasite.getTarget() == attacker && parasite.getSensing().hasLineOfSight(attacker)) {
                    return true;
                }
            }
        }
        return false;
    }

    
    public static void clearAll() {
        ATTRACTION_MAP.clear();
    }
}