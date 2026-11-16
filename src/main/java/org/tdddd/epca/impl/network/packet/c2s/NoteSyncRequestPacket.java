package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.events.NoteUnlockEvents;
import org.tdddd.epca.impl.network.ModNetwork;

/**
 * 「把笔记解锁状态发我一份」（C2S），空包。
 *
 * <p>客户端在打开笔记界面时发一次，服务端回 {@code SyncNoteUnlocksPacket}。
 * 这样即使玩家是在登录之后才解锁过（登录包早已发过），或者中途换了维度 / 重开了界面，
 * 打开书的那一刻客户端手里的解锁集合也一定是最新的。</p>
 *
 * <p><b>26.1.2 移植说明</b>：改成 {@link CustomPacketPayload} + {@link StreamCodec}，
 * 发送者从 {@code NetworkEvent.Context#getSender()} 换成 {@link IPayloadContext#player()}；
 * 空包的 codec 用 {@code StreamCodec.unit(...)}（等价于 1.20.1 里那一对空 encode/decode）。</p>
 */
public class NoteSyncRequestPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NoteSyncRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("note_sync_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NoteSyncRequestPacket> STREAM_CODEC =
            StreamCodec.unit(new NoteSyncRequestPacket());

    public NoteSyncRequestPacket() {
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NoteSyncRequestPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer sender)) {
                return;
            }
            NoteUnlockEvents.syncTo(sender);
        });
    }
}
