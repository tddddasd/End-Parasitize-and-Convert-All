package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.InfestedBlockManager;

/**
 * 虫染方块来源同步包（1.20.1 是三个内部消息类）。
 *
 * <p><b>26.1.2 改动</b>：{@code SimpleChannel} → {@link CustomPacketPayload}：
 * 每个内部类各自拥有 {@code TYPE} / {@code STREAM_CODEC} 并单独注册（见 {@link ModNetwork}）。
 * <b>线上字段与顺序不变</b>：Add/Remove 各一个 {@code BlockPos}；Request 无字段。
 */
public class InfestedSourcePacket {

    /** 服务端 → 客户端：新增一个虫染来源。 */
    public static class AddInfestedSourcePacket implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AddInfestedSourcePacket> TYPE =
                new CustomPacketPayload.Type<>(ModNetwork.id("add_infested_source"));

        public static final StreamCodec<RegistryFriendlyByteBuf, AddInfestedSourcePacket> STREAM_CODEC =
                CustomPacketPayload.codec(AddInfestedSourcePacket::encode, AddInfestedSourcePacket::decode);

        private final BlockPos pos;

        public AddInfestedSourcePacket(BlockPos pos) {
            this.pos = pos;
        }

        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public static AddInfestedSourcePacket decode(RegistryFriendlyByteBuf buf) {
            return new AddInfestedSourcePacket(buf.readBlockPos());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(AddInfestedSourcePacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                // 客户端执行
                WaterColorEffectsManager.addInfestedSource(packet.pos);
            });
        }
    }

    // 移除虫染方块包（类似，用于反转换或破坏时同步）
    /** 服务端 → 客户端：移除一个虫染来源。 */
    public static class RemoveInfestedSourcePacket implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RemoveInfestedSourcePacket> TYPE =
                new CustomPacketPayload.Type<>(ModNetwork.id("remove_infested_source"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveInfestedSourcePacket> STREAM_CODEC =
                CustomPacketPayload.codec(RemoveInfestedSourcePacket::encode, RemoveInfestedSourcePacket::decode);

        private final BlockPos pos;

        public RemoveInfestedSourcePacket(BlockPos pos) {
            this.pos = pos;
        }
        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public static RemoveInfestedSourcePacket decode(RegistryFriendlyByteBuf buf) {
            return new RemoveInfestedSourcePacket(buf.readBlockPos());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RemoveInfestedSourcePacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                // 客户端执行
                WaterColorEffectsManager.removeInfestedSource(packet.pos);
            });
        }
    }

    /** 客户端 → 服务端：请求全量虫染来源。 */
    public static class RequestAllInfestedSourcesPacket implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestAllInfestedSourcesPacket> TYPE =
                new CustomPacketPayload.Type<>(ModNetwork.id("request_all_infested_sources"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RequestAllInfestedSourcesPacket> STREAM_CODEC =
                CustomPacketPayload.codec(RequestAllInfestedSourcesPacket::encode, RequestAllInfestedSourcesPacket::decode);

        public void encode(RegistryFriendlyByteBuf buf) {
            // 无数据
        }

        public static RequestAllInfestedSourcesPacket decode(RegistryFriendlyByteBuf buf) {
            return new RequestAllInfestedSourcesPacket();
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RequestAllInfestedSourcesPacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) {
                    InfestedBlockManager.syncAllToPlayer(player);
                }
            });
        }
    }
}
