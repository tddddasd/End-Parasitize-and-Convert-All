package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.effect.ArayaSlashClientCache;
import org.tdddd.epca.impl.network.ModNetwork;

public class SpawnArayaSlashPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpawnArayaSlashPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("spawn_araya_slash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnArayaSlashPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SpawnArayaSlashPacket::encode, SpawnArayaSlashPacket::decode);

    private final double x;
    private final double y;
    private final double z;
    private final float directionX;
    private final float directionZ;
    private final long startTick;

    public SpawnArayaSlashPacket(double x, double y, double z, float directionX, float directionZ,
                                 long startTick) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.directionX = directionX;
        this.directionZ = directionZ;
        this.startTick = startTick;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.directionX);
        buf.writeFloat(this.directionZ);
        buf.writeVarLong(this.startTick);
    }

    public static SpawnArayaSlashPacket decode(RegistryFriendlyByteBuf buf) {
        return new SpawnArayaSlashPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readVarLong());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpawnArayaSlashPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ArayaSlashClientCache.add(new Vec3(packet.x, packet.y, packet.z),
                new Vec3(packet.directionX, 0.0D, packet.directionZ), packet.startTick));
    }
}
