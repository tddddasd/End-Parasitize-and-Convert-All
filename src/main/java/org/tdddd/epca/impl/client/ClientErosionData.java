package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ClientErosionData {
    private static final Map<UUID, ErosionData> erosionDataMap = new HashMap<>();

    public static class ErosionData {
        public final float erosionValue;
        public final int effectLevel;
        public final float maxHealth;
        public final float currentHealth;
        public final long timestamp;

        public ErosionData(float erosionValue, int effectLevel, float maxHealth, float currentHealth) {
            this.erosionValue = erosionValue;
            this.effectLevel = effectLevel;
            this.maxHealth = maxHealth;
            this.currentHealth = currentHealth;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 5000; 
        }
    }

    public static void setErosionData(UUID playerUUID, float erosionValue, int effectLevel, float maxHealth, float currentHealth) {
        erosionDataMap.put(playerUUID, new ErosionData(erosionValue, effectLevel, maxHealth, currentHealth));
    }

    public static void clearErosionData(UUID playerUUID) {
        erosionDataMap.remove(playerUUID);
    }

    public static ErosionData getErosionData(UUID playerUUID) {
        ErosionData data = erosionDataMap.get(playerUUID);
        if (data != null && data.isExpired()) {
            erosionDataMap.remove(playerUUID);
            return null;
        }
        return data;
    }

    public static ErosionData getLocalPlayerErosionData() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            return getErosionData(player.getUUID());
        }
        return null;
    }

    public static boolean hasErosionEffect(UUID playerUUID) {
        return getErosionData(playerUUID) != null;
    }
}