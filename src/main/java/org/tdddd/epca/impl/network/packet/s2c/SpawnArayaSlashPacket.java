package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.effect.ArayaSlashClientCache;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;

/**
 * Server to client: one Alayavijnana slash was cut, draw it here.
 *
 * <p>Sent to every player in the level for every kill of another player by the named staff. The slash is
 * a pure visual, so it is not an entity: an entity would need a spawn packet, a tracker, an update packet
 * and a removal packet, and would tick on the server for something that only ever draws. One of these
 * packets instead carries the three things the renderer cannot derive - where the blade goes, which way
 * the cut came from (that is what fixes the 50-degree blade frame) and the world time it started at, so
 * growth, hold and fade run on the world clock on every client alike.</p>
 *
 * <p>The packet is deliberately fire-and-forget: the client drops the entry after
 * {@code SLASH_HOLD_TICKS + SLASH_FADE_TICKS}, and a client that joins later simply never saw it.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>A {@link CustomPacketPayload} with a {@link StreamCodec} instead of a {@code SimpleChannel} message;
 * the wire format is byte-for-byte the same.</p>
 */
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
        ctx.enqueueWork(() -> {
            // TEMP DIAGNOSTIC (remove once the slash is confirmed on screen): proves the packet arrived
            // and which start time the client will compare its own game time against.
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            long clientTime = minecraft.level != null ? minecraft.level.getGameTime() : Long.MIN_VALUE;
            epca.LOGGER.info("[araya] slash packet: pos=({}, {}, {}) dir=({}, {}) startTick={} clientTime={}",
                    packet.x, packet.y, packet.z, packet.directionX, packet.directionZ,
                    packet.startTick, clientTime);
            ArayaSlashClientCache.add(new Vec3(packet.x, packet.y, packet.z),
                    new Vec3(packet.directionX, 0.0D, packet.directionZ), packet.startTick);
        });
    }
}
