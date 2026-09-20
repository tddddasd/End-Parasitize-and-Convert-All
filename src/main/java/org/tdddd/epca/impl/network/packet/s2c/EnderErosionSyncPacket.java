package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.ClientErosionData;
import org.tdddd.epca.impl.network.ModNetwork;

import java.util.UUID;


public class EnderErosionSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EnderErosionSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("ender_erosion_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnderErosionSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(EnderErosionSyncPacket::encode, EnderErosionSyncPacket::decode);

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

    public static void encode(EnderErosionSyncPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerUUID);
        buffer.writeFloat(packet.erosionValue);
        buffer.writeInt(packet.effectLevel);
        buffer.writeFloat(packet.maxHealth);
        buffer.writeFloat(packet.currentHealth);
    }

    public static EnderErosionSyncPacket decode(RegistryFriendlyByteBuf buffer) {
        UUID playerUUID = buffer.readUUID();
        float erosionValue = buffer.readFloat();
        int effectLevel = buffer.readInt();
        float maxHealth = buffer.readFloat();
        float currentHealth = buffer.readFloat();
        return new EnderErosionSyncPacket(playerUUID, erosionValue, effectLevel, maxHealth, currentHealth);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnderErosionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientErosionData.setErosionData(
                packet.playerUUID,
                packet.erosionValue,
                packet.effectLevel,
                packet.maxHealth,
                packet.currentHealth
        ));
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public float getErosionValue() { return erosionValue; }
    public int getEffectLevel() { return effectLevel; }
    public float getMaxHealth() { return maxHealth; }
    public float getCurrentHealth() { return currentHealth; }
}
