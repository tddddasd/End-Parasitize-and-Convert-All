package org.tdddd.epca.impl.overworld.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;

public class WorldDifficultyData extends SavedData {
    private static final String DATA_NAME = "epca_world_difficulty";
    private DifficultyLevel difficulty = DifficultyLevel.NORMAL;

    
    private DifficultyLevel customBaseDifficulty = DifficultyLevel.NORMAL;
    private float customSpawnRate = 1.0f;      
    private boolean customRewardEnabled = true;

    public DifficultyLevel getDifficulty() { return difficulty; }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
        setDirty();
    }

    
    public DifficultyLevel getCustomBaseDifficulty() { return customBaseDifficulty; }
    public void setCustomBaseDifficulty(DifficultyLevel baseDifficulty) {
        this.customBaseDifficulty = baseDifficulty;
        setDirty();
    }

    public float getCustomSpawnRate() { return customSpawnRate; }
    public void setCustomSpawnRate(float rate) {
        this.customSpawnRate = Math.max(0, Math.min(5, rate));
        setDirty();
    }

    public boolean isCustomRewardEnabled() { return customRewardEnabled; }
    public void setCustomRewardEnabled(boolean enabled) {
        this.customRewardEnabled = enabled;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("difficulty", difficulty.getId());

        tag.putInt("customBaseDifficulty", customBaseDifficulty.getId());
        tag.putFloat("customSpawnRate", customSpawnRate);
        tag.putBoolean("customRewardEnabled", customRewardEnabled);
        return tag;
    }

    public static WorldDifficultyData load(CompoundTag tag) {
        WorldDifficultyData data = new WorldDifficultyData();
        data.difficulty = DifficultyLevel.fromId(tag.getInt("difficulty"));

        if (tag.contains("customBaseDifficulty")) {
            data.customBaseDifficulty = DifficultyLevel.fromId(tag.getInt("customBaseDifficulty"));
        }
        if (tag.contains("customSpawnRate")) {
            data.customSpawnRate = tag.getFloat("customSpawnRate");
        }
        if (tag.contains("customRewardEnabled")) {
            data.customRewardEnabled = tag.getBoolean("customRewardEnabled");
        }
        return data;
    }

    public static WorldDifficultyData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WorldDifficultyData::load,
                WorldDifficultyData::new,
                DATA_NAME
        );
    }
}