package org.tdddd.epca.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ALLOW_COTH_LEVEL_4;
    public static final ModConfigSpec.BooleanValue PARASITE_PEACEFUL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PARASITE_TARGET_WHITELIST;
    private static final Set<Identifier> TARGET_WHITELIST = ConcurrentHashMap.newKeySet();
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PARASITE_IMMUNITY_WHITELIST;
    private static final Set<Identifier> IMMUNITY_WHITELIST = ConcurrentHashMap.newKeySet();
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PARASITE_CONVERSION_MOD_IMMUNITY_WHITELIST;
    private static final Set<String> CONVERSION_MOD_IMMUNITY_WHITELIST = ConcurrentHashMap.newKeySet();
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PARASITE_MOD_PEACEFUL_PAIRS;
    private static final Map<String, Set<String>> MOD_PEACEFUL_MAP = new ConcurrentHashMap<>();
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITIES_WHITELIST;
    private static final Set<Identifier> DISABLED_ENTITIES = ConcurrentHashMap.newKeySet();
    public static final ModConfigSpec.ConfigValue<String> DEFAULT_EXTRA_DIFFICULTY;
    public static final ModConfigSpec.BooleanValue SAFETY_DAY_ENABLED;
    public static final ModConfigSpec.IntValue SAFETY_DAY_DURATION_TICKS;
    static {
        BUILDER.push("End-Parasitize and Convert All Configuration");

        ALLOW_COTH_LEVEL_4 = BUILDER
                .comment("If true, the effect of Call of The Hive can be upgraded to level IV. Default: false",
                        "如果为true，寄巢之唤效果可以提升至IV级。默认值：false")
                .define("allowCothLevel4", false);

        PARASITE_PEACEFUL = BUILDER
                .comment(" ",
                        "If true, the parasite won't attack or convert any creatures (except those on the whitelist). Default: false",
                        "如果为true，寄生体将不会攻击和转化任何生物（白名单中的生物除外）。默认值：false")
                .define("parasitePeaceful", false);

        PARASITE_TARGET_WHITELIST = BUILDER
                .comment(" ",
                        "Parasite attack target whitelist (effective when parasitePeaceful is enabled), you need to enter the entity registration name",
                        "寄生体攻击目标白名单（当parasitePeaceful启用时生效），需填入实体注册名")
                .defineList("parasiteTargetWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);

        PARASITE_IMMUNITY_WHITELIST = BUILDER
                .comment(" ",
                        "Parasites don’t attack the target whitelist (parasites will never attack these creatures, highest priority), you need to enter the entity registration name",
                        "寄生体不攻击目标白名单（寄生体永远不会攻击这些生物，最高优先级），需填入实体注册名")
                .defineList("parasiteImmunityWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);


        PARASITE_CONVERSION_MOD_IMMUNITY_WHITELIST = BUILDER
                .comment(" ",
                        "Mod creature immune parasite transformation whitelist (all creatures with the entered mod ID will not be transformed, highest priority)",
                        "模组生物免疫寄生体转化白名单（填入的模组ID的所有生物不会被转化,最高优先级）")
                .defineList("parasiteConversionModImmunityWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);

        PARASITE_MOD_PEACEFUL_PAIRS = BUILDER
                .comment(" ",
                        "List of mod creatures that don’t attack each other, format: [\"mod1:mod2\", \"mod3:mod4\"], you need to fill in the mod IDs",
                        "模组生物互不攻击列表，格式：[\"mod1:mod2\", \"mod3:mod4\"]，需填入模组ID")
                .defineList("parasiteModPeacefulPairs", Collections.emptyList(),
                        entry -> entry instanceof String);

        DISABLED_ENTITIES_WHITELIST = BUILDER
                .comment(" ",
                        "Disable the mob whitelist (after entering the mob's registered name in this whitelist, the mob in the currently loaded chunks will be removed)",
                        "禁用生物白名单（在该白名单填入生物注册名后，会清除当前加载区块内的该生物）")
                .defineList("disabledEntitiesWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);

        DEFAULT_EXTRA_DIFFICULTY = BUILDER
                .comment(" ",
                        "The default extra difficulty when creating a new world. Options: easy, normal, expert, master, legendary, custom",
                        "新建世界时的默认额外难度。可选值: easy, normal, expert, master, legendary, custom")
                .define("defaultExtraDifficulty", "normal");

        SAFETY_DAY_ENABLED = BUILDER
                .comment(" ",
                        "Enable Safe Day, Default: false",
                        "是否启用安全日，默认值：false")
                .define("safetyDayEnabled", false);

        SAFETY_DAY_DURATION_TICKS = BUILDER
                .comment(" ",
                        "Safe day duration (game tick)",
                        "安全日持续时间（游戏刻）")
                .defineInRange("safetyDayDurationTicks", 60000, 1, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /**
     * 26.1.2: ModLoadingContext#registerConfig is gone; config registration moved to
     * {@link ModContainer#registerConfig(net.neoforged.fml.config.ModConfig.Type,
     * net.neoforged.fml.config.IConfigSpec, String)}. The old "look the container up
     * through ModLoadingContext.get()" route no longer exists, so the mod constructor
     * passes its injected {@link ModContainer} in
     * (see {@code epca(IEventBus, ModContainer)}).
     */
    public static void register(ModContainer modContainer) {
        
        Path configPath = Paths.get("E-PCA", "epca_main_config.toml");

        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON,
                SPEC,
                configPath.toString() 
        );
    }

    public static boolean isCothLevel4Allowed() {
        return ALLOW_COTH_LEVEL_4.get();
    }

    public static boolean isParasitePeaceful() {
        return PARASITE_PEACEFUL.get();
    }

    
    public static boolean isInTargetWhitelist(Identifier entityId) {
        
        if (TARGET_WHITELIST.isEmpty()) {
            for (String id : PARASITE_TARGET_WHITELIST.get()) {
                try {
                    TARGET_WHITELIST.add(Identifier.parse(id));
                } catch (Exception e) {
                    
                }
            }
        }
        return TARGET_WHITELIST.contains(entityId);
    }

    public static boolean isInTargetWhitelist(LivingEntity entity) {
        return isInTargetWhitelist(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static boolean isInImmunityWhitelist(Identifier entityId) {
        
        if (IMMUNITY_WHITELIST.isEmpty()) {
            for (String id : PARASITE_IMMUNITY_WHITELIST.get()) {
                try {
                    IMMUNITY_WHITELIST.add(Identifier.parse(id));
                } catch (Exception e) {
                    
                }
            }
        }
        return IMMUNITY_WHITELIST.contains(entityId);
    }
    
    public static boolean isInImmunityWhitelist(LivingEntity entity) {
        return isInImmunityWhitelist(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }
    
    public static boolean isInConversionModImmunityWhitelist(Identifier entityId) {
        if (CONVERSION_MOD_IMMUNITY_WHITELIST.isEmpty()) {
            for (String modId : PARASITE_CONVERSION_MOD_IMMUNITY_WHITELIST.get()) {
                CONVERSION_MOD_IMMUNITY_WHITELIST.add(modId);
            }
        }
        return CONVERSION_MOD_IMMUNITY_WHITELIST.contains(entityId.getNamespace());
    }

    public static boolean isInConversionModImmunityWhitelist(LivingEntity entity) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entityId != null && isInConversionModImmunityWhitelist(entityId);
    }

    public static boolean areModsPeaceful(LivingEntity entity1, LivingEntity entity2) {
        
        if (MOD_PEACEFUL_MAP.isEmpty()) {
            initModPeacefulMap();
        }

        Identifier key1 = BuiltInRegistries.ENTITY_TYPE.getKey(entity1.getType());
        Identifier key2 = BuiltInRegistries.ENTITY_TYPE.getKey(entity2.getType());

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

    
    public static boolean isInDisabledEntitiesWhitelist(Identifier entityId) {
        if (DISABLED_ENTITIES.isEmpty()) {
            for (String id : DISABLED_ENTITIES_WHITELIST.get()) {
                try {
                    DISABLED_ENTITIES.add(Identifier.parse(id));
                } catch (Exception e) {
                    
                }
            }
        }
        return DISABLED_ENTITIES.contains(entityId);
    }

    public static boolean isInDisabledEntitiesWhitelist(LivingEntity entity) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entityId != null && isInDisabledEntitiesWhitelist(entityId);
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
}