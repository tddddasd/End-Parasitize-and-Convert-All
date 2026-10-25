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

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ALLOW_PARA_CURSE_LEVEL_4;
    public static final ForgeConfigSpec.BooleanValue PARASITE_PEACEFUL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_TARGET_WHITELIST;
    private static final Set<ResourceLocation> TARGET_WHITELIST = ConcurrentHashMap.newKeySet();
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_IMMUNITY_WHITELIST;
    private static final Set<ResourceLocation> IMMUNITY_WHITELIST = ConcurrentHashMap.newKeySet();
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_CONVERSION_MOD_IMMUNITY_WHITELIST;
    private static final Set<String> CONVERSION_MOD_IMMUNITY_WHITELIST = ConcurrentHashMap.newKeySet();
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARASITE_MOD_PEACEFUL_PAIRS;
    private static final Map<String, Set<String>> MOD_PEACEFUL_MAP = new ConcurrentHashMap<>();
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITIES_WHITELIST;
    private static final Set<ResourceLocation> DISABLED_ENTITIES = ConcurrentHashMap.newKeySet();
    public static final ForgeConfigSpec.ConfigValue<String> DEFAULT_EXTRA_DIFFICULTY;
    public static final ForgeConfigSpec.BooleanValue SAFETY_DAY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BECKON_CORE_DEGRADE;
    public static final ForgeConfigSpec.IntValue SAFETY_DAY_DURATION_TICKS;
    static {
        BUILDER.push("End-Parasitize and Convert All Configuration");

        ALLOW_PARA_CURSE_LEVEL_4 = BUILDER
                .comment("If true, the effect of Call of The Hive can be upgraded to level IV. Default: false")
                .define("allowParaCurseLevel4", false);

        PARASITE_PEACEFUL = BUILDER
                .comment(" ",
                        "If true, the parasite won't attack or convert any creatures (except those on the whitelist). Default: false")
                .define("parasitePeaceful", false);

        PARASITE_TARGET_WHITELIST = BUILDER
                .comment(" ",
                        "Parasite attack target whitelist (effective when parasitePeaceful is enabled), you need to enter the entity registration name")
                .defineList("parasiteTargetWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);

        PARASITE_IMMUNITY_WHITELIST = BUILDER
                .comment(" ",
                        "Parasites don’t attack the target whitelist (parasites will never attack these creatures, highest priority), you need to enter the entity registration name")
                .defineList("parasiteImmunityWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);


        PARASITE_CONVERSION_MOD_IMMUNITY_WHITELIST = BUILDER
                .comment(" ",
                        "Mod creature immune parasite transformation whitelist (all creatures with the entered mod ID will not be transformed, highest priority)")
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
                        "Disable the mob whitelist (after entering the mob's registered name in this whitelist, the mob in the currently loaded chunks will be removed)")
                .defineList("disabledEntitiesWhitelist", Collections.emptyList(),
                        entry -> entry instanceof String);

        DEFAULT_EXTRA_DIFFICULTY = BUILDER
                .comment(" ",
                        "The default extra difficulty when creating a new world. Options: easy, normal, expert, master, legendary, custom")
                .define("defaultExtraDifficulty", "normal");

        SAFETY_DAY_ENABLED = BUILDER
                .comment(" ",
                        "Enable Safe Day, Default: false")
                .define("safetyDayEnabled", false);

        BECKON_CORE_DEGRADE = BUILDER
                .comment("If true, a beckon core that gained no biomass for 5 minutes degrades into a hive node. Default: true",
                        "\u5982\u679c\u4e3a true\uff0c\u53ec\u5524\u67f1\u6838\u5fc3\u5728 5 \u5206\u949f\u5185\u6ca1\u6709\u83b7\u5f97\u751f\u7269\u8d28\u65f6\u4f1a\u9000\u5316\u4e3a\u866b\u5de2\u8282\u70b9\u3002\u9ed8\u8ba4\uff1atrue")
                .define("beckonCoreDegrade", true);

        SAFETY_DAY_DURATION_TICKS = BUILDER
                .comment(" ",
                        "Safe day duration (game tick)")
                .defineInRange("safetyDayDurationTicks", 60000, 1, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        
        Path configPath = Paths.get("E-PCA", "epca_main_config.toml");

        ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                SPEC,
                configPath.toString() 
        );
    }

    public static boolean isParaCurseLevel4Allowed() {
        return ALLOW_PARA_CURSE_LEVEL_4.get();
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