package org.tdddd.epca.impl.overworld.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class EntityIntegrationManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<String, List<EntityIntegrationRule>> INTEGRATION_RULES = new HashMap<>();
    
    private static final Map<String, IntegrationProcess> ACTIVE_INTEGRATIONS = new HashMap<>();

    
    private static final Set<String> PROCESSED_GROUPS_THIS_TICK = new HashSet<>();
    private static long lastTickProcessed = -1;

    public static class EntityIntegrationRule {
        public List<EntityRequirement> entities;
        public double range;
        public int time;
        public List<EntityResult> results;
        public String ruleId;
        public Boolean randomResult = false;
        public Map<String, Double> resultProbabilities;
        public Integer minResultCount = 1;
        public Integer maxResultCount = 1;
        public Boolean allowDuplicateResults = true;
        public List<EntityResult> fixedResults;   

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityIntegrationRule that = (EntityIntegrationRule) o;
            return Double.compare(that.range, range) == 0 &&
                    time == that.time &&
                    Objects.equals(entities, that.entities) &&
                    Objects.equals(results, that.results) &&
                    Objects.equals(ruleId, that.ruleId) &&
                    Objects.equals(randomResult, that.randomResult) &&
                    Objects.equals(resultProbabilities, that.resultProbabilities) &&
                    Objects.equals(minResultCount, that.minResultCount) &&
                    Objects.equals(maxResultCount, that.maxResultCount) &&
                    Objects.equals(allowDuplicateResults, that.allowDuplicateResults) &&
                    Objects.equals(fixedResults, that.fixedResults);

        }

        @Override
        public int hashCode() {
            return Objects.hash(entities, range, time, results, ruleId,
                    randomResult, resultProbabilities, minResultCount,
                    maxResultCount, allowDuplicateResults, fixedResults);
        }
    }

    public static class EntityRequirement {
        public String entity;
        public Integer requiredKillCount;
        public Integer minCount;
        public Integer maxCount;
        public Double chance = 1.0;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityRequirement that = (EntityRequirement) o;
            return Objects.equals(entity, that.entity) &&
                    Objects.equals(requiredKillCount, that.requiredKillCount) &&
                    Objects.equals(minCount, that.minCount) &&
                    Objects.equals(maxCount, that.maxCount) &&
                    Double.compare(that.chance, chance) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(entity, requiredKillCount, minCount, maxCount, chance);
        }
    }

    public static class EntityResult {
        public String entity;
        public int count;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityResult that = (EntityResult) o;
            return count == that.count &&
                    Objects.equals(entity, that.entity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entity, count);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        INTEGRATION_RULES.clear();
        ACTIVE_INTEGRATIONS.clear();
        PROCESSED_GROUPS_THIS_TICK.clear();
        lastTickProcessed = -1;

        resourceManager.listResources("entity_integrations", file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        EntityIntegrationRule[] rules = GSON.fromJson(new InputStreamReader(stream), EntityIntegrationRule[].class);

                        for (EntityIntegrationRule rule : rules) {
                            if (isValidRule(rule)) {
                                
                                standardizeRule(rule);

                                
                                String fileName = resourceLocation.getPath().substring(
                                        resourceLocation.getPath().lastIndexOf("/") + 1,
                                        resourceLocation.getPath().lastIndexOf(".")
                                );
                                INTEGRATION_RULES.computeIfAbsent(fileName, k -> new ArrayList<>()).add(rule);

                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    
    private static void standardizeRule(EntityIntegrationRule rule) {
        
        if (rule.randomResult == null) rule.randomResult = false;
        if (rule.minResultCount == null) rule.minResultCount = 1;
        if (rule.maxResultCount == null) rule.maxResultCount = 1;
        if (rule.allowDuplicateResults == null) rule.allowDuplicateResults = true;
        if (rule.fixedResults == null) rule.fixedResults = new ArrayList<>();
        
        if (rule.maxResultCount < rule.minResultCount) {
            rule.maxResultCount = rule.minResultCount;
        }

        
        if (rule.resultProbabilities != null) {
            double totalProbability = rule.resultProbabilities.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            
            if (totalProbability > 1.0) {
                for (Map.Entry<String, Double> entry : rule.resultProbabilities.entrySet()) {
                    entry.setValue(entry.getValue() / totalProbability);
                }
            }
        }
    }

    
    private boolean isValidRule(EntityIntegrationRule rule) {
        return rule.entities != null && !rule.entities.isEmpty() &&
                rule.results != null && !rule.results.isEmpty() &&
                rule.range >= 0 && rule.range <= 16 &&
                rule.time >= 0;
    }

    
    public static void checkIntegration(ServerLevel level, List<Mob> entities) {
        if (entities.isEmpty() || level.isClientSide()) return;

        
        long currentTick = level.getGameTime();
        if (currentTick != lastTickProcessed) {
            PROCESSED_GROUPS_THIS_TICK.clear();
            lastTickProcessed = currentTick;
        }

        
        String groupId = generateGroupId(entities);

        
        if (ACTIVE_INTEGRATIONS.containsKey(groupId)) {
            return;
        }

        
        if (PROCESSED_GROUPS_THIS_TICK.contains(groupId)) {
            return;
        }

        
        for (Map.Entry<String, List<EntityIntegrationRule>> entry : INTEGRATION_RULES.entrySet()) {
            String ruleGroup = entry.getKey();
            List<EntityIntegrationRule> ruleList = entry.getValue();

            for (EntityIntegrationRule rule : ruleList) {
                if (matchesRule(level, entities, rule)) {
                    
                    PROCESSED_GROUPS_THIS_TICK.add(groupId);

                    
                    startIntegration(level, entities, rule, groupId);
                    return; 
                }
            }
        }

        
        PROCESSED_GROUPS_THIS_TICK.add(groupId);
    }

    
    private static boolean matchesRule(ServerLevel level, List<Mob> entities, EntityIntegrationRule rule) {
        
        if (!matchesEntityRequirements(level, entities, rule)) {
            return false;
        }

        
        return checkEntityProbabilities(level, entities, rule);
    }

    
    private static boolean matchesEntityRequirements(ServerLevel level, List<Mob> entities, EntityIntegrationRule rule) {
        
        List<EntityRequirement> requirements = new ArrayList<>(rule.entities);
        List<Mob> remainingEntities = new ArrayList<>(entities);

        
        for (EntityRequirement requirement : requirements) {
            int matchedCount = 0;
            int minRequired = requirement.minCount != null ? requirement.minCount : 1;
            int maxRequired = requirement.maxCount != null ? requirement.maxCount : Integer.MAX_VALUE;

            
            Iterator<Mob> iterator = remainingEntities.iterator();
            while (iterator.hasNext() && matchedCount < maxRequired) {
                Mob entity = iterator.next();
                if (matchesEntityRequirement(entity, requirement)) {
                    
                    if (requirement.chance < 1.0) {
                        if (level.random.nextDouble() > requirement.chance) {
                            continue; 
                        }
                    }

                    matchedCount++;
                    iterator.remove(); 
                }
            }

            
            if (matchedCount < minRequired || matchedCount > maxRequired) {
                return false;
            }
        }

        
        boolean result = remainingEntities.isEmpty();
        return result;
    }

    
    public static boolean matchesEntityRequirement(Mob entity, EntityRequirement requirement) {
        
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null || !entityId.toString().equals(requirement.entity)) {
            return false;
        }

        
        if (requirement.requiredKillCount != null) {
            int currentKillCount = EntityKillCountManager.getCurrentKillCount(entity);
            if (currentKillCount < requirement.requiredKillCount) {
                return false;
            }
        }

        return true;
    }

    
    private static boolean checkEntityProbabilities(ServerLevel level, List<Mob> entities, EntityIntegrationRule rule) {
        
        return matchesEntityRequirements(level, entities, rule);
    }

    
    private static String generateGroupId(List<Mob> entities) {
        List<String> entityIds = new ArrayList<>();
        for (Mob entity : entities) {
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityId != null) {
                entityIds.add(entityId.toString());
            }
        }
        Collections.sort(entityIds);
        return String.join("|", entityIds);
    }

    
    private static void startIntegration(ServerLevel level, List<Mob> entities, EntityIntegrationRule rule, String groupId) {
        BlockPos spawnPos = findNearestToCenter(entities);
        if (spawnPos == null) return;

        IntegrationProcess process = new IntegrationProcess(level, entities, rule, spawnPos);
        ACTIVE_INTEGRATIONS.put(groupId, process);

        
        if (rule.time == 0) {
            process.tick();
        }
    }

    
    public static void tick() {
        Iterator<Map.Entry<String, IntegrationProcess>> iterator = ACTIVE_INTEGRATIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, IntegrationProcess> entry = iterator.next();
            IntegrationProcess process = entry.getValue();

            if (process.isFinished()) {
                iterator.remove();
            } else {
                process.tick();
            }
        }
    }

    
    private static BlockPos findNearestToCenter(List<Mob> entities) {
        if (entities.isEmpty()) return null;

        
        double centerX = 0, centerY = 0, centerZ = 0;
        for (Mob entity : entities) {
            centerX += entity.getX();
            centerY += entity.getY();
            centerZ += entity.getZ();
        }
        centerX /= entities.size();
        centerY /= entities.size();
        centerZ /= entities.size();

        
        Mob nearestEntity = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Mob entity : entities) {
            double distanceSq = entity.distanceToSqr(centerX, centerY, centerZ);
            if (distanceSq < minDistanceSq) {
                minDistanceSq = distanceSq;
                nearestEntity = entity;
            }
        }

        return nearestEntity != null ? nearestEntity.blockPosition() : null;
    }

    public static Map<String, List<EntityIntegrationRule>> getAllIntegrationRules() {
        return INTEGRATION_RULES;
    }

    
    private static class IntegrationProcess {
        private final ServerLevel level;
        private final List<Mob> entities;
        private final EntityIntegrationRule rule;
        private final BlockPos spawnPos;
        private int ticksRemaining;
        private boolean hasPlayedEffects = false;
        private boolean isFinished = false;

        
        private CompoundTag inheritedVariantData = new CompoundTag();

        public IntegrationProcess(ServerLevel level, List<Mob> entities, EntityIntegrationRule rule, BlockPos spawnPos) {
            this.level = level;
            this.entities = new ArrayList<>(entities);
            this.rule = rule;
            this.spawnPos = spawnPos;
            this.ticksRemaining = rule.time;

            
            for (Mob entity : entities) {
                entity.getNavigation().stop();
            }

            
            extractInheritedVariantData();
        }

        
        private void extractInheritedVariantData() {
            for (Mob entity : entities) {
                if (entity instanceof IParasite parasite) {
                    CompoundTag data = parasite.getVariantData();
                    if (data != null && !data.isEmpty()) {
                        inheritedVariantData = data;
                        break; 
                    }
                }
            }
        }

        
        private void applyInheritedVariant(Entity newEntity) {
            if (inheritedVariantData == null || inheritedVariantData.isEmpty()) return;
            if (newEntity instanceof IParasite parasite) {
                parasite.setVariantData(inheritedVariantData);
            }
        }


        public void tick() {
            if (isFinished) return;

            
            if (rule.time == 0) {
                if (!hasPlayedEffects) {
                    playIntegrationEffects();
                    hasPlayedEffects = true;
                }
                completeIntegration();
                return;
            }

            ticksRemaining--;

            
            if (!validateEntities()) {
                cancelIntegration();
                return;
            }

            
            if (ticksRemaining == 1 && !hasPlayedEffects) {
                playIntegrationEffects();
                hasPlayedEffects = true;
            }

            
            if (ticksRemaining <= 0) {
                completeIntegration();
            }
        }

        
        private boolean validateEntities() {
            double rangeSq = rule.range * rule.range;

            for (int i = entities.size() - 1; i >= 0; i--) {
                Mob entity = entities.get(i);

                
                if (!entity.isAlive() || entity.isRemoved()) {
                    return false;
                }

                
                if (entity.distanceToSqr(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()) > rangeSq) {
                    return false;
                }
            }

            return true;
        }

        
        private void playIntegrationEffects() {
            
            level.playSound(null,
                    spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(),
                    SoundEvents.ZOMBIE_INFECT,
                    entities.get(0).getSoundSource(),
                    1.0F, 1.0F
            );
        }

        
        private void completeIntegration() {
            if (isFinished) return;
            
            spawnFixedResults();
            
            if (rule.randomResult) {
                spawnRandomResults();
            } else {
                spawnAllResults();
            }

            
            for (Mob entity : entities) {
                entity.discard();
            }

            
            playCompletionEffects();

            isFinished = true;
        }

        private void spawnFixedResults() {
            if (rule.fixedResults == null) return;
            for (EntityResult result : rule.fixedResults) {
                spawnEntityResult(result);
            }
        }

        
        private void spawnAllResults() {
            for (EntityResult result : rule.results) {
                spawnEntityResult(result);
            }
        }

        
        private void spawnRandomResults() {
            RandomSource random = level.random;   

            int resultCount = rule.minResultCount;
            if (rule.maxResultCount > rule.minResultCount) {
                resultCount = rule.minResultCount + random.nextInt(rule.maxResultCount - rule.minResultCount + 1);
            }

            List<EntityResult> availableResults = new ArrayList<>(rule.results);
            List<EntityResult> selectedResults = new ArrayList<>();

            for (int i = 0; i < resultCount; i++) {
                if (availableResults.isEmpty()) break;
                EntityResult selected = selectRandomResult(availableResults, random);
                if (selected != null) {
                    selectedResults.add(selected);
                    if (!rule.allowDuplicateResults) {
                        availableResults.remove(selected);
                    }
                }
            }
            for (EntityResult result : selectedResults) {
                spawnEntityResult(result);
            }
        }

        private EntityResult selectRandomResult(List<EntityResult> availableResults, RandomSource random) {
            if (rule.resultProbabilities != null && !rule.resultProbabilities.isEmpty()) {
                return selectByProbabilityMap(availableResults, random);
            } else {
                return availableResults.get(random.nextInt(availableResults.size()));
            }
        }

        private EntityResult selectByProbabilityMap(List<EntityResult> availableResults, RandomSource random) {
            double totalProbability = 0.0;
            for (EntityResult result : availableResults) {
                Double probability = rule.resultProbabilities.get(result.entity);
                totalProbability += (probability != null ? probability : 1.0 / availableResults.size());
            }
            if (totalProbability <= 0) return null;

            double randomValue = random.nextDouble() * totalProbability;
            double cumulative = 0.0;
            for (EntityResult result : availableResults) {
                Double probability = rule.resultProbabilities.get(result.entity);
                double prob = probability != null ? probability : 1.0 / availableResults.size();
                cumulative += prob;
                if (randomValue <= cumulative) return result;
            }
            return availableResults.get(availableResults.size() - 1);
        }

        private void spawnEntityResult(EntityResult result) {
            try {
                EntityType<?> entityType = EntityType.byString(result.entity).orElse(null);
                if (entityType != null) {
                    for (int i = 0; i < result.count; i++) {
                        Entity newEntity = entityType.create(level);
                        if (newEntity != null) {
                            double offsetX = (level.random.nextDouble() - 0.5);
                            double offsetZ = (level.random.nextDouble() - 0.5);
                            newEntity.moveTo(spawnPos.getX() + offsetX, spawnPos.getY(), spawnPos.getZ() + offsetZ,
                                    level.random.nextFloat() * 360.0F, 0.0F);
                            applyInheritedVariant(newEntity);
                            level.addFreshEntity(newEntity);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void playCompletionEffects() {
            level.sendParticles(ParticleTypes.EXPLOSION, spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ(),
                    3, 1.0, 1.0, 1.0, 0.1);
        }

        private void cancelIntegration() {
            isFinished = true;
        }

        public boolean isFinished() {
            return isFinished;
        }
    }
}