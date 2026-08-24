package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.ClientErosionData;

import java.util.UUID;
import java.util.function.Supplier;


public class EnderErosionSyncPacket {
    private final UUID playerUUID;
    private final float erosionValue;
    private final int effectLevel;
    private final float maxHealth;
    private final float currentHealth;

    public EnderErosionSyncPacket(UUID playerUUID, float erosionValue, int effectLevel, float maxHealth, float currentHealth) {
        this.playerUUID = playerUUID;
        this.erosionValue = erosionValue;
        this.effectLevel = effectLevel;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
    }

    public static void encode(EnderErosionSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerUUID);
        buffer.writeFloat(packet.erosionValue);
        buffer.writeInt(packet.effectLevel);
        buffer.writeFloat(packet.maxHealth);
        buffer.writeFloat(packet.currentHealth);
    }

    public static EnderErosionSyncPacket decode(FriendlyByteBuf buffer) {
        UUID playerUUID = buffer.readUUID();
        float erosionValue = buffer.readFloat();
        int effectLevel = buffer.readInt();
        float maxHealth = buffer.readFloat();
        float currentHealth = buffer.readFloat();
        return new EnderErosionSyncPacket(playerUUID, erosionValue, effectLevel, maxHealth, currentHealth);
    }

    public static void handle(EnderErosionSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            
            if (context.getDirection().getReceptionSide().isClient()) {
                ClientErosionData.setErosionData(
                        packet.playerUUID,
                        packet.erosionValue,
                        packet.effectLevel,
                        packet.maxHealth,
                        packet.currentHealth
                );
            }
        });
        context.setPacketHandled(true);
    }

    
    public UUID getPlayerUUID() { return playerUUID; }
    public float getErosionValue() { return erosionValue; }
    public int getEffectLevel() { return effectLevel; }
    public float getMaxHealth() { return maxHealth; }
    public float getCurrentHealth() { return currentHealth; }
}