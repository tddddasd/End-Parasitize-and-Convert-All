package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.ClientColorEffect;

import java.util.function.Supplier;

public class ColorEffectPacket {
    private final int entityId;
    private final int type;
    private final int duration; 

    public ColorEffectPacket(LivingEntity entity, int type) {
        this.entityId = entity.getId();
        this.type = type;
        this.duration = 10;
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
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);
                if (entity instanceof LivingEntity) {
                    ClientColorEffect.setEffect(packet.entityId, packet.type, packet.duration);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}