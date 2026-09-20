package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.ClientColorEffect;
import org.tdddd.epca.impl.network.ModNetwork;


public class ColorEffectPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ColorEffectPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("color_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ColorEffectPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ColorEffectPacket::encode, ColorEffectPacket::new);

    private final int entityId;
    private final int type;
    private final int duration; 

    
    private static final int DEFAULT_DURATION = 10;

    public ColorEffectPacket(LivingEntity entity, int type) {
        this(entity, type, DEFAULT_DURATION);
    }

    public ColorEffectPacket(LivingEntity entity, int type, int duration) {
        this.entityId = entity.getId();
        this.type = type;
        this.duration = duration;
    }

    public ColorEffectPacket(RegistryFriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.type = buf.readInt();
        this.duration = buf.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(type);
        buf.writeInt(duration);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ColorEffectPacket packet, IPayloadContext ctx) {
        // 26.1.2: the effect is recorded by entity id without requiring the entity to be present
        // yet, so a packet that arrives before the new entity is spawned still takes effect.
        ctx.enqueueWork(() -> ClientColorEffect.setEffect(packet.entityId, packet.type, packet.duration));
    }
}
