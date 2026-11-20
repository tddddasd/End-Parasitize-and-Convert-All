package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.organ.OrganModelDisplay;

/**
 * 客户端 → 服务端：切换「显示 / 隐藏内器官模型」。
 *
 * <p>状态由服务端写进玩家持久化数据，再通过 {@code SyncOrganModelDisplayPacket} 广播给
 * 所有客户端（包括自己），所以自己和其他玩家看到的开关效果一定一致。</p>
 */
public class ToggleOrganModelPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToggleOrganModelPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("toggle_organ_model"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleOrganModelPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ToggleOrganModelPacket::encode,
                    ToggleOrganModelPacket::decode);

    public ToggleOrganModelPacket() {
    }

    public static void encode(ToggleOrganModelPacket msg, RegistryFriendlyByteBuf buf) {
    }

    public static ToggleOrganModelPacket decode(RegistryFriendlyByteBuf buf) {
        return new ToggleOrganModelPacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleOrganModelPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;

            boolean visible = OrganModelDisplay.toggle(sender);
            OrganModelDisplay.broadcast(sender);
            sender.sendSystemMessage(Component.literal(visible
                    ? "内器官模型：显示"
                    : "内器官模型：隐藏"), true);
        });
    }
}
