package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;
import org.tdddd.epca.impl.network.ModNetwork;

import java.util.ArrayList;
import java.util.Collection;


public class SyncAllInfestedSourcesPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAllInfestedSourcesPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_all_infested_sources"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAllInfestedSourcesPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncAllInfestedSourcesPacket::encode, SyncAllInfestedSourcesPacket::decode);

    private final Collection<BlockPos> positions;

    public SyncAllInfestedSourcesPacket(Collection<BlockPos> positions) {
        this.positions = positions;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
    }

    public static SyncAllInfestedSourcesPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Collection<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return new SyncAllInfestedSourcesPacket(positions);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncAllInfestedSourcesPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            WaterColorEffectsManager.clearInfestedCache();
            WaterColorEffectsManager.addInfestedSourcesBatch(packet.positions);
        });
    }
}
