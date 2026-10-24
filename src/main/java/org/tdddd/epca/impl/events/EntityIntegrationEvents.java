package org.tdddd.epca.impl.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Mod.EventBusSubscriber
public class EntityIntegrationEvents {

    private static final int CHECK_INTERVAL = 5;
    private static int checkCounter = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "E-PCA", "entity_check_namespaces.json");
    private static Set<String> checkedNamespaces = new HashSet<>(Collections.singletonList("epca")); 
    private static long lastConfigLoadTime = -1;
    private static final long CONFIG_RELOAD_INTERVAL = 1200;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EntityIntegrationManager.tick();

            long currentTick = event.getServer().getTickCount();
            if (currentTick - lastConfigLoadTime >= CONFIG_RELOAD_INTERVAL) {
                loadConfig();
                lastConfigLoadTime = currentTick;
            }

            checkCounter++;
            if (checkCounter >= CHECK_INTERVAL) {
                checkCounter = 0;
                checkAllWorldsForIntegrations(event.getServer());
            }
        }
    }

    private static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                Map<String, List<String>> defaultConfig = new HashMap<>();
                defaultConfig.put("namespaces", Collections.singletonList("epca"));
                String json = GSON.toJson(defaultConfig);
                Files.write(CONFIG_PATH, json.getBytes());
                checkedNamespaces = new HashSet<>(defaultConfig.get("namespaces"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            Map<String, List<String>> config = GSON.fromJson(reader, Map.class);
            List<String> namespaces = config.get("namespaces");
            if (namespaces != null && !namespaces.isEmpty()) {
                checkedNamespaces = new HashSet<>(namespaces);
            } else {
                checkedNamespaces = new HashSet<>(Collections.singletonList("epca"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkAllWorldsForIntegrations(MinecraftServer server) {
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            checkWorldForIntegrations(level);
        }
    }

    /**
     * Only epca's own mobs can take part in an integration rule, so the world wide scan is filtered to
     * them before any rule work happens.
     *
     * <p>It used to collect <b>every</b> mob inside the world border every five ticks and then run every
     * rule against each of them, and each rule that matched did its own {@code getEntitiesOfClass} area
     * query. On a world with a few hundred mobs that is thousands of entity queries per five ticks, which
     * showed up as server lag.</p>
     */
    private static void checkWorldForIntegrations(ServerLevel level) {
        List<Mob> allEntities = level.getEntitiesOfClass(
                Mob.class,
                level.getWorldBorder().getCollisionShape().bounds(),
                EntityIntegrationEvents::isEpcaMob
        );
        for (Mob entity : allEntities) {
            checkEntityForAllIntegrationRules(level, entity);
        }
    }

    /** True for mobs registered by epca, i.e. the only ones an integration rule can mention. */
    private static boolean isEpcaMob(Mob entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && org.tdddd.epca.impl.epca.MODID.equals(key.getNamespace());
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
                        isEntityInRule(entity, rule)
        );

        List<Mob> allEntities = new ArrayList<>();
        allEntities.add(centerEntity);
        allEntities.addAll(nearbyEntities);

        if (isEntityGroupValidForRule(allEntities, rule)) {
            EntityIntegrationManager.checkIntegration(level, allEntities);
        }
    }

    private static boolean isEntityInRule(Mob entity, EntityIntegrationRule rule) {
        for (EntityRequirement requirement : rule.entities) {
            if (EntityIntegrationManager.matchesEntityRequirement(entity, requirement)) {
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
            int maxRequired = requirement.maxCount != null ? requirement.maxCount : Integer.MAX_VALUE;

            Iterator<Mob> iterator = remainingEntities.iterator();
            while (iterator.hasNext() && matchedCount < maxRequired) {
                Mob entity = iterator.next();
                if (EntityIntegrationManager.matchesEntityRequirement(entity, requirement)) {
                    matchedCount++;
                    iterator.remove();
                    if (matchedCount >= maxRequired) {
                        break;
                    }
                }
            }

            if (matchedCount < requiredCount) {
                return false;
            }
            if (matchedCount > maxRequired) {
                return false;
            }
        }
        return remainingEntities.isEmpty();
    }

    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob &&
                mob.level() instanceof ServerLevel serverLevel) {
            String entityId = getEntityId(mob);
            
            String namespace = entityId.contains(":") ? entityId.split(":")[0] : "";
            if (checkedNamespaces.contains(namespace)) {
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
                EntityBiomassCountManager.incrementBiomassCount(killer);
                if (EntityBiomassCountManager.isBiomassCountMaxed(killer)) {
                    checkEntityForAllIntegrationRules(serverLevel, killer);
                }
            }
        }
    }

    private static String getEntityId(Mob entity) {
        return ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
    }
}