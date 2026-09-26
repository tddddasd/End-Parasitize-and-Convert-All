package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.ArayaSlashClientCache;

import java.util.function.Supplier;

/**
 * Server -&gt; client: one Alayavijnana slash was cut, draw it here.
 *
 * <p>Sent to every player that can see the victim, for every kill of another player by the named staff.
 * The slash is a pure visual, so it is not an entity: an entity would need a spawn packet, a tracker, an
 * update packet and a removal packet, and would tick on the server for something that only ever draws.
 * One of these packets instead carries the three things the renderer cannot derive - where the blade
 * goes, which way the cut came from (that is what fixes the 50-degree blade frame) and the world time it
 * started at, so growth, hold and fade run on the world clock on every client alike.</p>
 *
 * <p>The packet is deliberately fire-and-forget and has no lifetime field: the client drops the entry
 * after {@code SLASH_HOLD_TICKS + SLASH_FADE_TICKS}, and a client that joins later simply never saw it.</p>
 */
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
