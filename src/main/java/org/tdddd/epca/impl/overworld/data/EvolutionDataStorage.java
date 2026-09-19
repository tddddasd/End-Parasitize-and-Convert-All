package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EvolutionDataStorage extends SavedData {
    /** 1.20.1 used the file name "EvolutionData"; 26.1.2 needs a valid (lowercase) Identifier path.
     *  Old file: data/EvolutionData.dat - new file: data/epca/evolution_data.dat (see PORTING-NOTES-w2-data.md). */
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "evolution_data");

    /** Dimension keys keep the 1.20.1 "namespace:path" string form: ResourceKey.codec writes exactly what
     *  the old code wrote via dim.location().toString(). */
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    /** "Dimensions" list entry: {Dimension:"ns:dim", Points:int} - same field names as the 1.20.1 save/load. */
    private record DimensionPoints(ResourceKey<Level> dimension, int points) {
        private static final Codec<DimensionPoints> CODEC = RecordCodecBuilder.create(i -> i.group(
                DIMENSION_CODEC.fieldOf("Dimension").forGetter(DimensionPoints::dimension),
                Codec.INT.fieldOf("Points").forGetter(DimensionPoints::points)
        ).apply(i, DimensionPoints::new));
    }

    /** "Cooldowns" list entry: {Dimension:"ns:dim", CooldownEnd:long}. */
    private record DimensionCooldown(ResourceKey<Level> dimension, long endTick) {
        private static final Codec<DimensionCooldown> CODEC = RecordCodecBuilder.create(i -> i.group(
                DIMENSION_CODEC.fieldOf("Dimension").forGetter(DimensionCooldown::dimension),
                Codec.LONG.fieldOf("CooldownEnd").forGetter(DimensionCooldown::endTick)
        ).apply(i, DimensionCooldown::new));
    }

    /** "OverriddenStages" list entry: {Dimension:"ns:dim", Stage:int}. */
    private record DimensionStage(ResourceKey<Level> dimension, int stage) {
        private static final Codec<DimensionStage> CODEC = RecordCodecBuilder.create(i -> i.group(
                DIMENSION_CODEC.fieldOf("Dimension").forGetter(DimensionStage::dimension),
                Codec.INT.fieldOf("Stage").forGetter(DimensionStage::stage)
        ).apply(i, DimensionStage::new));
    }

    /**
     * 26.1.2 persists SavedData through a codec. The three legacy lists keep their 1.20.1 names, so a 1.20.1
     * data compound can be carried over by wrapping it as {"data": &lt;old root compound&gt;} in the new file.
     */
    public static final Codec<EvolutionDataStorage> CODEC = RecordCodecBuilder.create(i -> i.group(
            DimensionPoints.CODEC.listOf().optionalFieldOf("Dimensions", List.of())
                    .forGetter(EvolutionDataStorage::pointsList),
            DimensionCooldown.CODEC.listOf().optionalFieldOf("Cooldowns", List.of())
                    .forGetter(EvolutionDataStorage::cooldownsList),
            DimensionStage.CODEC.listOf().optionalFieldOf("OverriddenStages", List.of())
                    .forGetter(EvolutionDataStorage::stagesList)
    ).apply(i, EvolutionDataStorage::new));

    public static final SavedDataType<EvolutionDataStorage> TYPE =
            new SavedDataType<EvolutionDataStorage>(DATA_ID, EvolutionDataStorage::new, CODEC);

    
    private final Map<ResourceKey<Level>, Integer> dimensionPoints = new ConcurrentHashMap<>();
    
    private final Map<ResourceKey<Level>, Long> dimensionCooldowns = new ConcurrentHashMap<>();
    private final Map<ResourceKey<Level>, Integer> overriddenStages = new ConcurrentHashMap<>();

    public EvolutionDataStorage() {
        
        dimensionPoints.put(Level.OVERWORLD, 0);
        dimensionPoints.put(Level.NETHER, -50);
        dimensionPoints.put(Level.END, -50);
        
        ResourceKey<Level> twilightForest = ResourceKey.create(Registries.DIMENSION,
                Identifier.parse("twilightforest:twilight_forest"));
        dimensionPoints.put(twilightForest, -50);
    }

    /** Codec decode constructor: takes only what the file contains and seeds nothing, matching the old
     *  EvolutionDataStorage(CompoundTag) constructor (the defaults above are for brand new data only). */
    private EvolutionDataStorage(List<DimensionPoints> points, List<DimensionCooldown> cooldowns,
                                 List<DimensionStage> stages) {
        for (DimensionPoints entry : points) {
            dimensionPoints.put(entry.dimension(), entry.points());
        }
        for (DimensionCooldown entry : cooldowns) {
            dimensionCooldowns.put(entry.dimension(), entry.endTick());
        }
        for (DimensionStage entry : stages) {
            overriddenStages.put(entry.dimension(), entry.stage());
        }
    }

    private List<DimensionPoints> pointsList() {
        List<DimensionPoints> list = new ArrayList<>(dimensionPoints.size());
        dimensionPoints.forEach((dim, points) -> list.add(new DimensionPoints(dim, points)));
        return list;
    }

    private List<DimensionCooldown> cooldownsList() {
        List<DimensionCooldown> list = new ArrayList<>(dimensionCooldowns.size());
        dimensionCooldowns.forEach((dim, end) -> list.add(new DimensionCooldown(dim, end)));
        return list;
    }

    private List<DimensionStage> stagesList() {
        List<DimensionStage> list = new ArrayList<>(overriddenStages.size());
        overriddenStages.forEach((dim, stage) -> list.add(new DimensionStage(dim, stage)));
        return list;
    }

    public static EvolutionDataStorage get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    
    public int getPointsForDimension(ResourceKey<Level> dimension) {
        return dimensionPoints.getOrDefault(dimension, 0);
    }

    public void setPointsForDimension(ResourceKey<Level> dimension, int points) {
        dimensionPoints.put(dimension, points);
        setDirty();
    }

    
    public long getCooldownEndForDimension(ResourceKey<Level> dimension) {
        return dimensionCooldowns.getOrDefault(dimension, 0L);
    }

    public void setCooldownEndForDimension(ResourceKey<Level> dimension, long endTick) {
        dimensionCooldowns.put(dimension, endTick);
        setDirty();
    }

    
    public Integer getOverriddenStage(ResourceKey<Level> dimension) {
        return overriddenStages.get(dimension);
    }

    public void setOverriddenStage(ResourceKey<Level> dimension, int stage) {
        overriddenStages.put(dimension, stage);
        setDirty();
    }

    public void clearOverriddenStage(ResourceKey<Level> dimension) {
        overriddenStages.remove(dimension);
        setDirty();
    }

    public Set<ResourceKey<Level>> getOverriddenDimensions() {
        return Collections.unmodifiableSet(overriddenStages.keySet());
    }

    
    public void resetDimension(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) setPointsForDimension(dimension, 0);
        else if (dimension.equals(Level.NETHER) || dimension.equals(Level.END))
            setPointsForDimension(dimension, -50);
        else setPointsForDimension(dimension, 0);
        clearOverriddenStage(dimension); 
    }

    public void resetAllDimensions() {
        resetDimension(Level.OVERWORLD);
        resetDimension(Level.NETHER);
        resetDimension(Level.END);
        for (ResourceKey<Level> dim : dimensionPoints.keySet()) {
            if (!dim.equals(Level.OVERWORLD) && !dim.equals(Level.NETHER) && !dim.equals(Level.END)) {
                resetDimension(dim);
            }
        }
    }
}
