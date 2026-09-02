package org.tdddd.epca.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ALLOW_COTH_LEVEL_4;
    public static final ForgeConfigSpec.BooleanValue PARASITE_FRIENDLY;
    public static final ForgeConfigSpec.BooleanValue PARASITE_PEACEFUL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_TARGET_WHITELIST;
    
    private static final Set<ResourceLocation> TARGET_WHITELIST = ConcurrentHashMap.newKeySet();
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_IMMUNITY_WHITELIST;
    private static final Set<ResourceLocation> IMMUNITY_WHITELIST = ConcurrentHashMap.newKeySet();
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_CONVERSION_MOD_IMMUNITY_WHITELIST;
    private static final Set<String> CONVERSION_MOD_IMMUNITY_WHITELIST = ConcurrentHashMap.newKeySet();
    
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_MOD_PEACEFUL_PAIRS;
    private static final Map<String, Set<String>> MOD_PEACEFUL_MAP = new ConcurrentHashMap<>();
    
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_MODS_LIST;
    private static final Set<String> PARASITE_MODS = ConcurrentHashMap.newKeySet();
    
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITIES_WHITELIST;
    private static final Set<ResourceLocation> DISABLED_ENTITIES = ConcurrentHashMap.newKeySet();
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_ENEMY_PLAYERS;
    
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends Double>> STAGE_THRESHOLDS;
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends Double>> POINTS_MULTIPLIER;
    
    public static final ForgeConfigSpec.ConfigValue<String> DEFAULT_EXTRA_DIFFICULTY;
    
    public static final ForgeConfigSpec.BooleanValue SAFETY_DAY_ENABLED;
    public static final ForgeConfigSpec.IntValue SAFETY_DAY_DURATION_TICKS;
    static {
        BUILDER.push("End-Parasitize and Convert All Configuration");

        ALLOW_COTH_LEVEL_4 = BUILDER
                .comment("如果为true，寄巢之唤效果可以提升至IV级（amplifier=3）。默认值：false")
                .define("allowCothLevel4", false);

        PARASITE_FRIENDLY = BUILDER
                .comment("如果为true，寄生体不会攻击玩家。默认值：false")
                .define("parasiteFriendly", false);

        PARASITE_PEACEFUL = BUILDER
                .comment("如果为true，寄生体将不会攻击和转化任何生物（白名单中的生物除外）。默认值：false")
                .define("parasitePeaceful", false);

        PARASITE_TARGET_WHITELIST = BUILDER
                .comment("寄生体攻击目标白名单（当parasitePeaceful启用时生效）",
                        "格式: [\"minecraft:creeper\", \"minecraft:zombie\"]")
                .defineList("parasiteTargetWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);

        PARASITE_IMMUNITY_WHITELIST = BUILDER
                .comment("寄生体不攻击目标白名单（寄生体永远不会攻击这些生物，最高优先级）",
                        "格式: [\"minecraft:creeper\", \"minecraft:zombie\"]")
                .defineList("parasiteImmunityWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);


        PARASITE_CONVERSION_MOD_IMMUNITY_WHITELIST = BUILDER
                .comment("模组免疫寄生体转化白名单（这些模组的所有生物永远不会被转化,最高优先级）",
                        "格式: [\"modid1\", \"modid2\"]",
                        "此列表优先级高于其他所有转化条件")
                .defineList("parasiteConversionModImmunityWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);

        PARASITE_MOD_PEACEFUL_PAIRS = BUILDER
                .comment("模组生物互不攻击列表，格式为[\"mod1:mod2\", \"mod3:mod4\"]",
                        "表示mod1和mod2下的生物不会互相攻击，mod3和mod4下的生物不会互相攻击",
                        "示例：[\"minecraft:epca\"] 表示原版生物和此模组的生物不会互相攻击")
                .defineList("parasiteModPeacefulPairs", Collections.emptyList(),
                        entry -> entry instanceof String);

        DISABLED_ENTITIES_WHITELIST = BUILDER
                .comment("禁用生物白名单（在该白名单填入生物id后，检测到加载区块内有该生物，便会remove该生物）",
                        "格式: [\"minecraft:creeper\", \"minecraft:zombie\"]")
                .defineList("disabledEntitiesWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);

        PARASITE_MODS_LIST = BUILDER
                .comment("判定为寄生体的模组列表（这些模组的所有生物将被视为寄生体）",
                        "格式: [\"modid1\", \"modid2\"]",
                        "注意：如果填入\"minecraft\"，则不包括玩家")
                .defineList("parasiteModsList", Arrays.asList("epca"),
                        entry -> entry instanceof String);

        PARASITE_ENEMY_PLAYERS = BUILDER
                .comment("被寄生体视为敌人的玩家列表（即使开启友好模式）",
                        "格式: [\"player id1\", \"player id2\"]")
                .defineList("parasiteEnemyPlayers", Collections.emptyList(),
                        entry -> entry instanceof String);

        STAGE_THRESHOLDS = BUILDER
                .comment("演化阶段阈值列表，按顺序从阶段-2到阶段10，共13个值，支持小数（最多两位小数）",
                        "默认值：[-100, -50, 0, 400, 800, 1800, 20000, 200000, 5000000, 25000000, 500000000, 1000000000, 1800000000]")
                .defineList("stageThresholds",
                        Arrays.asList(-100.0, -50.0, 0.0, 400.0, 800.0, 1800.0, 20000.0, 200000.0, 5000000.0, 25000000.0, 500000000.0, 1000000000.0, 1800000000.0),
                        entry -> entry instanceof Double);

        POINTS_MULTIPLIER = BUILDER
                .comment("每个演化阶段的点数增加倍率，顺序从阶段-2到阶段10，共13个值，范围0.0~10.0（0%~1000%），默认全1.0")
                .defineList("pointsMultiplier",
                        Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                        entry -> entry instanceof Double && (Double) entry >= 0.0 && (Double) entry <= 10.0);

        DEFAULT_EXTRA_DIFFICULTY = BUILDER
                .comment("新建世界时的默认额外难度。可选值: easy, normal, expert, master, legendary, custom",
                        "注：custom 难度下会使用普通难度的数值，但允许通过指令调整额外参数")
                .define("defaultExtraDifficulty", "normal");

        SAFETY_DAY_ENABLED = BUILDER
                .comment("是否启用安全日")
                .define("safetyDayEnabled", false);

        SAFETY_DAY_DURATION_TICKS = BUILDER
                .comment("安全日持续时间（游戏刻）")
                .defineInRange("safetyDayDurationTicks", 60000, 1, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        
        Path configPath = Paths.get("E-PCA", "epca.toml");

        ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                SPEC,
                configPath.toString() 
        );
    }
    public static boolean isParasiteFriendly() {
        return PARASITE_FRIENDLY.get();
    }

    public static boolean isCothLevel4Allowed() {
        return ALLOW_COTH_LEVEL_4.get();
    }

    public static boolean isParasitePeaceful() {
        return PARASITE_PEACEFUL.get();
    }

    
    public static boolean isInTargetWhitelist(ResourceLocation entityId) {
        
        if (TARGET_WHITELIST.isEmpty()) {
            for (String id : PARASITE_TARGET_WHITELIST.get()) {
                try {
                    TARGET_WHITELIST.add(new ResourceLocation(id));
                } catch (Exception e) {
                    
                }
            }
        }
        return TARGET_WHITELIST.contains(entityId);
    }

    
    public static boolean isInTargetWhitelist(LivingEntity entity) {
        return isInTargetWhitelist(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    
    public static boolean isInImmunityWhitelist(ResourceLocation entityId) {
        
        if (IMMUNITY_WHITELIST.isEmpty()) {
            for (String id : PARASITE_IMMUNITY_WHITELIST.get()) {
                try {
                    IMMUNITY_WHITELIST.add(new ResourceLocation(id));
                } catch (Exception e) {
                    
                }
            }
        }
        return IMMUNITY_WHITELIST.contains(entityId);
    }

    
    public static boolean isInImmunityWhitelist(LivingEntity entity) {
        return isInImmunityWhitelist(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    
    public static boolean isInConversionModImmunityWhitelist(ResourceLocation entityId) {
        if (CONVERSION_MOD_IMMUNITY_WHITELIST.isEmpty()) {
            for (String modId : PARASITE_CONVERSION_MOD_IMMUNITY_WHITELIST.get()) {
                CONVERSION_MOD_IMMUNITY_WHITELIST.add(modId);
            }
        }
        return CONVERSION_MOD_IMMUNITY_WHITELIST.contains(entityId.getNamespace());
    }

    public static boolean isInConversionModImmunityWhitelist(LivingEntity entity) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return entityId != null && isInConversionModImmunityWhitelist(entityId);
    }
    


    
    public static boolean areModsPeaceful(LivingEntity entity1, LivingEntity entity2) {
        
        if (MOD_PEACEFUL_MAP.isEmpty()) {
            initModPeacefulMap();
        }

        
        ResourceLocation key1 = ForgeRegistries.ENTITY_TYPES.getKey(entity1.getType());
        ResourceLocation key2 = ForgeRegistries.ENTITY_TYPES.getKey(entity2.getType());

        if (key1 == null || key2 == null) {
            return false;
        }

        String mod1 = key1.getNamespace();
        String mod2 = key2.getNamespace();

        
        if (mod1.equals(mod2)) return false;

        
        Set<String> peacefulMods1 = MOD_PEACEFUL_MAP.get(mod1);
        Set<String> peacefulMods2 = MOD_PEACEFUL_MAP.get(mod2);

        return (peacefulMods1 != null && peacefulMods1.contains(mod2)) ||
                (peacefulMods2 != null && peacefulMods2.contains(mod1));
    }

    
    public static boolean isInDisabledEntitiesWhitelist(ResourceLocation entityId) {
        if (DISABLED_ENTITIES.isEmpty()) {
            for (String id : DISABLED_ENTITIES_WHITELIST.get()) {
                try {
                    DISABLED_ENTITIES.add(new ResourceLocation(id));
                } catch (Exception e) {
                    
                }
            }
        }
        return DISABLED_ENTITIES.contains(entityId);
    }

    public static boolean isInDisabledEntitiesWhitelist(LivingEntity entity) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return entityId != null && isInDisabledEntitiesWhitelist(entityId);
    }
    

    
    public static boolean isFromParasiteMod(ResourceLocation entityId) {
        if (PARASITE_MODS.isEmpty()) {
            for (String modId : PARASITE_MODS_LIST.get()) {
                PARASITE_MODS.add(modId);
            }
        }
        return PARASITE_MODS.contains(entityId.getNamespace());
    }

    public static boolean isFromParasiteMod(LivingEntity entity) {
        
        if (entity instanceof net.minecraft.world.entity.player.Player &&
                isFromParasiteMod(new ResourceLocation("minecraft", "player"))) {
            return false;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return entityId != null && isFromParasiteMod(entityId);
    }
    

    
    private static synchronized void initModPeacefulMap() {
        
        if (!MOD_PEACEFUL_MAP.isEmpty()) return;

        for (String pair : PARASITE_MOD_PEACEFUL_PAIRS.get()) {
            String[] mods = pair.split(":");
            if (mods.length != 2) continue;

            String modA = mods[0];
            String modB = mods[1];

            
            MOD_PEACEFUL_MAP.computeIfAbsent(modA, k -> ConcurrentHashMap.newKeySet()).add(modB);
            MOD_PEACEFUL_MAP.computeIfAbsent(modB, k -> ConcurrentHashMap.newKeySet()).add(modA);
        }
    }

    
    public static List<String> getModPeacefulPairs() {
        if (MOD_PEACEFUL_MAP.isEmpty()) {
            initModPeacefulMap();
        }
        return MOD_PEACEFUL_MAP.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(value -> entry.getKey() + ":" + value))
                .distinct()
                .collect(Collectors.toList());
    }
    

    
    public static double[] getStageThresholds() {
        List<? extends Double> list = STAGE_THRESHOLDS.get();
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    
    public static double getPointsMultiplier(int stage) {
        
        int index = stage + 2;
        List<? extends Double> list = POINTS_MULTIPLIER.get();
        if (index < 0 || index >= list.size()) {
            return 1.0; 
        }
        return list.get(index);
    }

    public static boolean isSafetyDayEnabled() { return SAFETY_DAY_ENABLED.get(); }
    public static int getSafetyDayDurationTicks() { return SAFETY_DAY_DURATION_TICKS.get(); }

    
    public static DifficultyLevel getDefaultExtraDifficulty() {
        String val = DEFAULT_EXTRA_DIFFICULTY.get().toLowerCase();
        return switch (val) {
            case "easy" -> DifficultyLevel.EASY;
            case "normal" -> DifficultyLevel.NORMAL;
            case "expert" -> DifficultyLevel.EXPERT;
            case "master" -> DifficultyLevel.MASTER;
            case "legendary" -> DifficultyLevel.LEGENDARY;
            case "custom" -> DifficultyLevel.CUSTOM;
            default -> DifficultyLevel.NORMAL;
        };
    }

    
    public static void clearCache() {
        TARGET_WHITELIST.clear();
        IMMUNITY_WHITELIST.clear();
        CONVERSION_MOD_IMMUNITY_WHITELIST.clear();
        DISABLED_ENTITIES.clear();
        MOD_PEACEFUL_MAP.clear();
        PARASITE_MODS.clear();
    }

    public static boolean isEnemyPlayer(String playerName) {
        return PARASITE_ENEMY_PLAYERS.get().contains(playerName);
    }
}