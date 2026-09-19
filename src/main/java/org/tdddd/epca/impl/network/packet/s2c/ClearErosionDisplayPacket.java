package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.ClientErosionData;
import org.tdddd.epca.impl.network.ModNetwork;

import java.util.UUID;

/**
 * 服务端 → 客户端：清除某玩家的侵蚀显示数据。
 *
 * <p><b>26.1.2 改动</b>：{@code SimpleChannel} → {@link CustomPacketPayload}；
 * 原 {@code context.getDirection().getReceptionSide().isClient()} 判断由
 * “注册在 {@code playToClient}”取代。
 * <b>线上字段与顺序不变</b>：单个 {@code uuid}。
 */
public class ClearErosionDisplayPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClearErosionDisplayPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("clear_erosion_display"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearErosionDisplayPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ClearErosionDisplayPacket::encode, ClearErosionDisplayPacket::decode);

    private final UUID playerUUID;

    public ClearErosionDisplayPacket(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public static void encode(ClearErosionDisplayPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerUUID);
    }

    public static ClearErosionDisplayPacket decode(RegistryFriendlyByteBuf buffer) {
        UUID playerUUID = buffer.readUUID();
        return new ClearErosionDisplayPacket(playerUUID);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearErosionDisplayPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientErosionData.clearErosionData(packet.playerUUID));
    }

    public UUID getPlayerUUID() { return playerUUID; }
}
