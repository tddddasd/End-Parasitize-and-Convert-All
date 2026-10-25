package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.HiveLeaderClientCache;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncHiveLeadersPacket {
    private final Set<UUID> leaders;

    public SyncHiveLeadersPacket(Set<UUID> leaders) {
        this.leaders = new HashSet<>(leaders);
    }

    public SyncHiveLeadersPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        leaders = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            leaders.add(buf.readUUID());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(leaders.size());
        for (UUID uuid : leaders) {
            buf.writeUUID(uuid);
        }
    }

    public static void handle(SyncHiveLeadersPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> HiveLeaderClientCache.updateLeaders(packet.leaders));
        ctx.get().setPacketHandled(true);
    }
}