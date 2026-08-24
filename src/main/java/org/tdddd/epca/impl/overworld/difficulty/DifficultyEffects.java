package org.tdddd.epca.impl.overworld.difficulty;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.data.WorldDifficultyData;

public class DifficultyEffects {

    
    public static DifficultyLevel getEffectiveDifficulty(Level level) {
        if (level.isClientSide) return DifficultyLevel.NORMAL;
        WorldDifficultyData data = WorldDifficultyData.get((ServerLevel) level);
        DifficultyLevel diff = data.getDifficulty();
        if (diff == DifficultyLevel.CUSTOM) {
            
            return data.getCustomBaseDifficulty();
        }
        return diff;
    }

    
    public static float getSpawnRateMultiplier(Level level) {
        if (level.isClientSide) return 1.0f;
        WorldDifficultyData data = WorldDifficultyData.get((ServerLevel) level);
        if (data.getDifficulty() == DifficultyLevel.CUSTOM) {
            return data.getCustomSpawnRate();
        }
        return 1.0f;
    }

    
    
    
    public static boolean isRewardEnabled(Level level) {
        if (level.isClientSide) return true;
        WorldDifficultyData data = WorldDifficultyData.get((ServerLevel) level);
        DifficultyLevel diff = data.getDifficulty();
        if (diff == DifficultyLevel.CUSTOM) {
            return data.isCustomRewardEnabled();
        }
        
        return true;
    }

    
    public static float getParasiteStatMultiplier(Level level) {
        DifficultyLevel diff = getEffectiveDifficulty(level);
        return switch (diff) {
            case EASY -> 0.5f;
            case NORMAL -> 1.0f;
            case EXPERT -> 1.5f;
            case MASTER -> 1.25f;
            default -> 1.0f;
        };
    }

    public static float getBleedingDamageMultiplier(Level level) {
        DifficultyLevel diff = getEffectiveDifficulty(level);
        return diff == DifficultyLevel.EASY ? 0.5f : 1.0f;
    }

    public static float getEvolutionPointsMultiplier(Level level) {
        DifficultyLevel diff = getEffectiveDifficulty(level);
        return switch (diff) {
            case EXPERT -> 1.5f;
            case MASTER -> 2.0f;
            default -> 1.0f;
        };
    }

    
    public static float getExtraLootChance(Level level) {
        if (!isRewardEnabled(level)) return 0f;
        DifficultyLevel diff = getEffectiveDifficulty(level);
        return switch (diff) {
            case EXPERT -> 0.5f;
            case MASTER -> 1.0f;
            default -> 0f;
        };
    }

    public static float getExtraLootMultiplier(Level level) {
        if (!isRewardEnabled(level)) return 1.0f;
        DifficultyLevel diff = getEffectiveDifficulty(level);
        return diff == DifficultyLevel.MASTER ? 1.5f : 1.0f;
    }

    
    public static boolean shouldDropLoot(Level level) {
        DifficultyLevel diff = getEffectiveDifficulty(level);
        if (diff == DifficultyLevel.EASY && level.random.nextFloat() < 0.25f) {
            return false;
        }
        return true;
    }

    
    public static boolean isAttractionRangeEnabled(Level level) {
        DifficultyLevel diff = getEffectiveDifficulty(level);
        return diff != DifficultyLevel.EASY;
    }

    public static boolean isCothEffectEnabled(Level level) {
        return getEffectiveDifficulty(level) == DifficultyLevel.LEGENDARY;
    }

    public static boolean isLegendary(Level level) {
        return getEffectiveDifficulty(level) == DifficultyLevel.LEGENDARY;
    }
}