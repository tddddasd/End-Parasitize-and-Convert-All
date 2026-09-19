package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;
import org.tdddd.epca.impl.network.ModNetwork;

/**
 * 服务端 → 客户端：酸液对水体的染色效果。
 *
 * <p><b>26.1.2 改动</b>：{@code SimpleChannel} → {@link CustomPacketPayload}。
 * <b>线上字段与顺序不变</b>：{@code BlockPos waterPos} + {@code BlockPos acidPos}
 * + {@code int distance} + {@code boolean add}。
 */
public class AcidWaterColorPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AcidWaterColorPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("acid_water_color"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AcidWaterColorPacket> STREAM_CODEC =
            CustomPacketPayload.codec(AcidWaterColorPacket::encode, AcidWaterColorPacket::new);

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

    public AcidWaterColorPacket(RegistryFriendlyByteBuf buf) {
        this.waterPos = buf.readBlockPos();
        this.acidPos = buf.readBlockPos();
        this.distance = buf.readInt();
        this.add = buf.readBoolean();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(waterPos);
        buf.writeBlockPos(acidPos);
        buf.writeInt(distance);
        buf.writeBoolean(add);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AcidWaterColorPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.add) {
                WaterColorEffectsManager.updateClientEffect(packet.waterPos, packet.acidPos, packet.distance);
            } else {
                WaterColorEffectsManager.removeClientEffect(packet.waterPos);
            }
        });
    }
}
