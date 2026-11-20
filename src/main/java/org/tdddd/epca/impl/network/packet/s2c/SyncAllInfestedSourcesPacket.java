package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * One page of the full "infested source" snapshot. A big world holds hundreds of thousands of
 * positions, so the snapshot is sent as several pages; only the first page clears the client cache.
 */
public class SyncAllInfestedSourcesPacket {

    /** Upper bound for the length field read from the wire, so a corrupt size cannot pre-allocate. */
    private static final int MAX_DECODED_POSITIONS = 4_194_304;

    private final Collection<BlockPos> positions;

    private final boolean reset;

    public SyncAllInfestedSourcesPacket(Collection<BlockPos> positions) {
        this(positions, false);
    }

    public SyncAllInfestedSourcesPacket(Collection<BlockPos> positions, boolean reset) {
        this.positions = positions;
        this.reset = reset;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.reset);
        buf.writeInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
    }

    public static SyncAllInfestedSourcesPacket decode(FriendlyByteBuf buf) {
        boolean reset = buf.readBoolean();
        // clamp: a corrupt length must not pre-allocate a huge list from a short buffer
        int size = Math.min(Math.max(0, buf.readInt()), MAX_DECODED_POSITIONS);
        Collection<BlockPos> positions = new ArrayList<>(Math.min(size, 8192));
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return new SyncAllInfestedSourcesPacket(positions, reset);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (this.reset) {
                WaterColorEffectsManager.clearInfestedCache();
            }
            WaterColorEffectsManager.addInfestedSourcesBatch(positions);
        });
        ctx.get().setPacketHandled(true);
    }
}
