package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.events.NoteUnlockEvents;

import java.util.function.Supplier;

/**
 * 「把笔记解锁状态发我一份」（C2S），空包。
 *
 * <p>客户端在打开笔记界面时发一次，服务端回 {@code SyncNoteUnlocksPacket}。
 * 这样即使玩家是在登录之后才解锁过（登录包早已发过），或者中途换了维度 / 重开了界面，
 * 打开书的那一刻客户端手里的解锁集合也一定是最新的。</p>
 */
public class NoteSyncRequestPacket {

    public static void encode(NoteSyncRequestPacket msg, FriendlyByteBuf buf) {
    }

    public static NoteSyncRequestPacket decode(FriendlyByteBuf buf) {
        return new NoteSyncRequestPacket();
    }

    public static void handle(NoteSyncRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            NoteUnlockEvents.syncTo(sender);
        });
        context.setPacketHandled(true);
    }
}
