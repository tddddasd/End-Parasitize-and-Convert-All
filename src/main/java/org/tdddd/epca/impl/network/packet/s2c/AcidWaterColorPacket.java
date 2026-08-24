package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;

import java.util.function.Supplier;

public class AcidWaterColorPacket {
    private final BlockPos waterPos;
    private final BlockPos acidPos;
    private final int distance;
    private final boolean add;

    public AcidWaterColorPacket(BlockPos waterPos, BlockPos acidPos, int distance, boolean add) {
        this.waterPos = waterPos;
        this.acidPos = acidPos;
        this.distance = distance;
        this.add = add;
    }

    public AcidWaterColorPacket(FriendlyByteBuf buf) {
        this.waterPos = buf.readBlockPos();
        this.acidPos = buf.readBlockPos();
        this.distance = buf.readInt();
        this.add = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(waterPos);
        buf.writeBlockPos(acidPos);
        buf.writeInt(distance);
        buf.writeBoolean(add);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (add) {
                WaterColorEffectsManager.updateClientEffect(waterPos, acidPos, distance);
            } else {
                WaterColorEffectsManager.removeClientEffect(waterPos);
            }
        });
        context.get().setPacketHandled(true);
    }
}