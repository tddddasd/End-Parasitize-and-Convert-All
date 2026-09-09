package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

public class SyncAllInfestedSourcesPacket {
    private final Collection<BlockPos> positions;

    public SyncAllInfestedSourcesPacket(Collection<BlockPos> positions) {
        this.positions = positions;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
    }

    public static SyncAllInfestedSourcesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Collection<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return new SyncAllInfestedSourcesPacket(positions);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            WaterColorEffectsManager.clearInfestedCache();
            WaterColorEffectsManager.addInfestedSourcesBatch(positions);
        });
        ctx.get().setPacketHandled(true);
    }
}