package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.render.EpcaGibRenderer;
import org.tdddd.epca.impl.network.ModNetwork;

/** Server to client: break the dead parasite into render-only gibs. */
public class DismemberS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DismemberS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("dismember"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DismemberS2CPacket> STREAM_CODEC =
            CustomPacketPayload.codec(DismemberS2CPacket::encode, DismemberS2CPacket::decode);

    private final String typeId;
    private final double x, y, z;
    private final float yaw, pitch, width, height;

    public DismemberS2CPacket(String typeId, double x, double y, double z,
                              float yaw, float pitch, float width, float height) {
        this.typeId = typeId;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.width = width; this.height = height;
    }

    public static DismemberS2CPacket of(LivingEntity entity) {
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return new DismemberS2CPacket(id == null ? "minecraft:pig" : id.toString(),
                entity.getX(), entity.getY(), entity.getZ(),
                entity.yBodyRot, entity.getXRot(),
                entity.getBbWidth(), entity.getBbHeight());
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.typeId);
        buf.writeDouble(this.x); buf.writeDouble(this.y); buf.writeDouble(this.z);
        buf.writeFloat(this.yaw); buf.writeFloat(this.pitch);
        buf.writeFloat(this.width); buf.writeFloat(this.height);
    }

    public static DismemberS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new DismemberS2CPacket(buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DismemberS2CPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> EpcaGibRenderer.add(packet.typeId, packet.x, packet.y, packet.z,
                packet.yaw, packet.pitch, packet.width, packet.height));
    }

    public static void send(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        PacketDistributor.sendToPlayersTrackingEntity(entity, of(entity));
    }
}