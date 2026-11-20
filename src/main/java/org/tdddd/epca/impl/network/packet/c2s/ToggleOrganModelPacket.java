package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.organ.OrganModelDisplay;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：切换「显示 / 隐藏内器官模型」。
 *
 * <p>状态由服务端写进玩家持久化数据，再通过 {@code SyncOrganModelDisplayPacket} 广播给
 * 所有客户端（包括自己），所以自己和其他玩家看到的开关效果一定一致。</p>
 */
public class ToggleOrganModelPacket {

    public ToggleOrganModelPacket() {
    }

    public static void encode(ToggleOrganModelPacket msg, FriendlyByteBuf buf) {
    }

    public static ToggleOrganModelPacket decode(FriendlyByteBuf buf) {
        return new ToggleOrganModelPacket();
    }

    public static void handle(ToggleOrganModelPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) return;
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            boolean visible = OrganModelDisplay.toggle(sender);
            OrganModelDisplay.broadcast(sender);
            sender.displayClientMessage(Component.literal(visible
                    ? "内器官模型：显示"
                    : "内器官模型：隐藏"), true);
        });
        context.setPacketHandled(true);
    }
}
