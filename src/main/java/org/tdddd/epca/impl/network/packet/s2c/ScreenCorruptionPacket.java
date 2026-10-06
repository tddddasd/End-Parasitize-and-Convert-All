package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.BioTortClientState;

import java.util.function.Supplier;

/**
 *  -&gt;  2 "" /  2
 *
 * <h2></h2>
 * <p>14  2 4  3
 *  5  {@code BioTortIncarnation}
 * " + / "
 *
 * ""</p>
 *
 * <h2></h2>
 * <p> {@code PacketDistributor.PLAYER}
 *
 *  {@code active = false} </p>
 */
public class ScreenCorruptionPacket {

    private final boolean active;
    private final int corruptionTicks;
    private final int decayTicks;

    public ScreenCorruptionPacket(boolean active, int corruptionTicks, int decayTicks) {
        this.active = active;
        this.corruptionTicks = corruptionTicks;
        this.decayTicks = decayTicks;
    }

    /** {@code corruptionTicks}  {@code decayTicks}  */
    public static ScreenCorruptionPacket start(int corruptionTicks, int decayTicks) {
        return new ScreenCorruptionPacket(true, corruptionTicks, decayTicks);
    }

    /**  /  /  /  */
    public static ScreenCorruptionPacket stop() {
        return new ScreenCorruptionPacket(false, 0, 0);
    }

    public static void encode(ScreenCorruptionPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeVarInt(Math.max(0, msg.corruptionTicks));
        buf.writeVarInt(Math.max(0, msg.decayTicks));
    }

    public static ScreenCorruptionPacket decode(FriendlyByteBuf buf) {
        return new ScreenCorruptionPacket(buf.readBoolean(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ScreenCorruptionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (msg.active) {
                BioTortClientState.startCorruption(msg.corruptionTicks, msg.decayTicks);
            } else {
                BioTortClientState.stopCorruption();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

