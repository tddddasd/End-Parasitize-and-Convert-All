package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.ClientColorEffect;
import org.tdddd.epca.impl.network.ModNetwork;

/**
 * 服务端 → 客户端：给生物叠一层颜色效果。
 *
 * <p><b>26.1.2 改动</b>：{@code SimpleChannel} → {@link CustomPacketPayload}。
 * <b>线上字段与顺序不变</b>：{@code int entityId} + {@code int type} + {@code int duration}。
 */
public class ColorEffectPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ColorEffectPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("color_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ColorEffectPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ColorEffectPacket::encode, ColorEffectPacket::new);

    private final int entityId;
    private final int type;
    private final int duration; 

    public ColorEffectPacket(LivingEntity entity, int type) {
        this.entityId = entity.getId();
        this.type = type;
        this.duration = 10;
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
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);
                if (entity instanceof LivingEntity) {
                    ClientColorEffect.setEffect(packet.entityId, packet.type, packet.duration);
                }
            }
        });
    }
}
