package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;

public class WorldDifficultyData extends SavedData {
    /** 1.20.1 stored this as "epca_world_difficulty"; it stays a valid Identifier path. */
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "epca_world_difficulty");

    /**
     * Legacy field names kept. Defaults mirror the old load(CompoundTag): "customBaseDifficulty",
     * "customSpawnRate" and "customRewardEnabled" only overwrote the in-memory default when present, and
     * "difficulty" fell back to id 0 (EASY) because the old read used getInt(...).orElse(0).
     */
    public static final Codec<WorldDifficultyData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("difficulty", 0)
                    .forGetter(data -> data.difficulty.getId()),
            Codec.INT.optionalFieldOf("customBaseDifficulty", DifficultyLevel.NORMAL.getId())
                    .forGetter(data -> data.customBaseDifficulty.getId()),
            Codec.FLOAT.optionalFieldOf("customSpawnRate", 1.0F)
                    .forGetter(data -> data.customSpawnRate),
            Codec.BOOL.optionalFieldOf("customRewardEnabled", true)
                    .forGetter(data -> data.customRewardEnabled)
    ).apply(i, WorldDifficultyData::new));

    public static final SavedDataType<WorldDifficultyData> TYPE =
            new SavedDataType<WorldDifficultyData>(DATA_ID, WorldDifficultyData::new, CODEC);

    private DifficultyLevel difficulty = DifficultyLevel.NORMAL;

    
    private DifficultyLevel customBaseDifficulty = DifficultyLevel.NORMAL;
    private float customSpawnRate = 1.0f;      
    private boolean customRewardEnabled = true;

    public WorldDifficultyData() {}

    /** Codec decode constructor. Values are assigned as stored (the setters' clamping is a user-input guard). */
    private WorldDifficultyData(int difficultyId, int customBaseDifficultyId, float customSpawnRate,
                                boolean customRewardEnabled) {
        this.difficulty = DifficultyLevel.fromId(difficultyId);
        this.customBaseDifficulty = DifficultyLevel.fromId(customBaseDifficultyId);
        this.customSpawnRate = customSpawnRate;
        this.customRewardEnabled = customRewardEnabled;
    }

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

    public static WorldDifficultyData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
