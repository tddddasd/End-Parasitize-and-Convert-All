package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.ArayaSlashClientCache;

import java.util.function.Supplier;

public class SpawnArayaSlashPacket {

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

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.directionX);
        buf.writeFloat(this.directionZ);
        buf.writeVarLong(this.startTick);
    }

    public static SpawnArayaSlashPacket decode(FriendlyByteBuf buf) {
        return new SpawnArayaSlashPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readVarLong());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ArayaSlashClientCache.add(
                new net.minecraft.world.phys.Vec3(this.x, this.y, this.z),
                new net.minecraft.world.phys.Vec3(this.directionX, 0.0D, this.directionZ),
                this.startTick));
        ctx.get().setPacketHandled(true);
    }
}
