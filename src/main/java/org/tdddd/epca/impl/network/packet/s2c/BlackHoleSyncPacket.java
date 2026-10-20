package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.events.BlackHoleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Mirrors the server's black hole list to the client so it can be rendered. */
public class BlackHoleSyncPacket {

    private final List<BlackHoleManager.Hole> holes;

    public BlackHoleSyncPacket(List<BlackHoleManager.Hole> holes) {
        this.holes = holes;
    }

    public BlackHoleSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BlackHoleManager.Hole> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new BlackHoleManager.Hole(
                    new net.minecraft.world.phys.Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarInt()));
        }
        this.holes = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.holes.size());
        for (BlackHoleManager.Hole hole : this.holes) {
            buf.writeDouble(hole.pos().x);
            buf.writeDouble(hole.pos().y);
            buf.writeDouble(hole.pos().z);
            buf.writeVarInt(hole.age());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> BlackHoleManager.setClientHoles(this.holes));
        ctx.get().setPacketHandled(true);
    }
}
