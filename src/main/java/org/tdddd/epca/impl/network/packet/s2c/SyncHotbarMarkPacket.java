package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.BioTortClientState;

import java.util.function.Supplier;

/**
 *  -&gt; "" 1
 *
 * <h2></h2>
 * <p> 20
 * {@code BioTortIncarnation} /</p>
 *
 * <h2></h2>
 * <p>{@code slotMask}  9 bit 0..8 =  0..8{@code 0}
 * ""
 * ""{@code durationTicks}
 * </p>
 *
 * <p>{@code PacketDistributor.PLAYER}</p>
 */
public class SyncHotbarMarkPacket {

    /** bit 0..8 */
    public static final int SLOT_MASK_BITS = 9;

    private final int slotMask;
    private final int durationTicks;

    public SyncHotbarMarkPacket(int slotMask, int durationTicks) {
        this.slotMask = slotMask;
        this.durationTicks = durationTicks;
    }

    /**  {@code slotMask == 0}  */
    public static SyncHotbarMarkPacket mark(int slotMask, int durationTicks) {
        return new SyncHotbarMarkPacket(slotMask, durationTicks);
    }

    /**  */
    public static SyncHotbarMarkPacket clear() {
        return new SyncHotbarMarkPacket(0, 0);
    }

    public static void encode(SyncHotbarMarkPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slotMask);
        buf.writeVarInt(msg.durationTicks);
    }

    public static SyncHotbarMarkPacket decode(FriendlyByteBuf buf) {
        return new SyncHotbarMarkPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SyncHotbarMarkPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> BioTortClientState.applyHotbarMark(msg.slotMask, msg.durationTicks));
        ctx.get().setPacketHandled(true);
    }
}

