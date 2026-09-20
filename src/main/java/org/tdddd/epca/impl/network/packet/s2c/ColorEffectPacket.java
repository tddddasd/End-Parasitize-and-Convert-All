package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.ClientColorEffect;

import java.util.function.Supplier;

public class ColorEffectPacket {
    
    public static final int DEFAULT_DURATION = 10;

    private final int entityId;
    private final int type;
    private final int duration; 

    public ColorEffectPacket(LivingEntity entity, int type) {
        this(entity, type, DEFAULT_DURATION);
    }

    public ColorEffectPacket(LivingEntity entity, int type, int duration) {
        this.entityId = entity.getId();
        this.type = type;
        this.duration = duration;
    }

    public ColorEffectPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.type = buf.readInt();
        this.duration = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(type);
        buf.writeInt(duration);
    }

    public static void handle(ColorEffectPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            
            
            ClientColorEffect.setEffect(packet.entityId, packet.type, packet.duration);
        });
        ctx.get().setPacketHandled(true);
    }
}
