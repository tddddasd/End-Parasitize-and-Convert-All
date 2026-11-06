package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class SafetyDaySavedData extends SavedData {

    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "epca_safety_day");

    public static final Codec<SafetyDaySavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("EndTick", 0L).forGetter(data -> data.safetyDayEndTick),
            Codec.BOOL.optionalFieldOf("TriggeredEffect", false).forGetter(data -> data.hasTriggeredEffect)
    ).apply(i, SafetyDaySavedData::new));

    public static final SavedDataType<SafetyDaySavedData> TYPE =
            new SavedDataType<SafetyDaySavedData>(DATA_ID, SafetyDaySavedData::new, CODEC);

    public long safetyDayEndTick = -1;
    private boolean hasTriggeredEffect = false;

    public SafetyDaySavedData() {}

    private SafetyDaySavedData(long safetyDayEndTick, boolean hasTriggeredEffect) {
        this.safetyDayEndTick = safetyDayEndTick;
        this.hasTriggeredEffect = hasTriggeredEffect;
    }

    public static SafetyDaySavedData get(Level level) {
        if (level.isClientSide()) throw new IllegalStateException("Only server side");
        ServerLevel serverLevel = (ServerLevel) level;
        return serverLevel.getDataStorage().computeIfAbsent(TYPE);
    }

    public void startSafetyDay(long endTick) {
        this.safetyDayEndTick = endTick;
        this.hasTriggeredEffect = false;
        setDirty();
    }

    public boolean isSafetyDayActive(long currentTick) {
        return safetyDayEndTick != -1 && currentTick < safetyDayEndTick;
    }

    public boolean isSafetyDayFinished(long currentTick) {
        return safetyDayEndTick != -1 && currentTick >= safetyDayEndTick;
    }

    public void markEffectTriggered() {
        this.hasTriggeredEffect = true;
        setDirty();
    }

    public boolean isEffectTriggered() {
        return hasTriggeredEffect;
    }

    public void reset() {
        safetyDayEndTick = -1;
        hasTriggeredEffect = false;
        setDirty();
    }
}
