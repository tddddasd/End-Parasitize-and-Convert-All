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
 * 发送者从 {@code NetworkEvent.Context#getSender()} 换成 {@link IPayloadContext#player()}。</p>
 *
 * <h2>为什么不能用 {@code StreamCodec.unit(...)}（0.148n 崩溃根因）</h2>
 * <p>{@code StreamCodec.unit(x)} 的 encode 是<b>按引用相等</b>校验的：
 * 它记住构造时传进去的那个实例，发送任何<b>别的</b>实例就抛
 * {@code IllegalStateException: Can't encode 'A', expected 'B'}。
 * 本包原来写成 {@code StreamCodec.unit(new NoteSyncRequestPacket())}，而发送点又
 * {@code new NoteSyncRequestPacket()} 了第二个实例 → 客户端一打开笔记界面就抛
 * {@code EncoderException: Failed to encode packet 'serverbound/minecraft:custom_payload'}
 * → 连接被断开（玩家看到的就是“打开笔记即崩/掉线”）。</p>
 *
 * <p>现在改成与其它空包（{@code ToggleFollowPacket} / {@code RequestOpenHiveLeaderOrgansPacket}
 * 等）完全一致的真 codec：{@link CustomPacketPayload#codec} 走
 * {@code StreamCodec.ofMember}，encode 收到哪个实例都接受、只是不写任何字节，
 * decode 一律还回共享的 {@link #INSTANCE}。这样“注册实例 ≠ 发送实例”不再有任何影响。</p>
 */
public class NoteSyncRequestPacket implements CustomPacketPayload {

    /**
     * 共享单例：空包没有任何状态，全库统一发这一个实例，
     * 既省一次分配，也让“注册的 codec 与服务端收到的实例”在调试时是同一个对象。
     */
    public static final NoteSyncRequestPacket INSTANCE = new NoteSyncRequestPacket();

    public static final CustomPacketPayload.Type<NoteSyncRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("note_sync_request"));

    /**
     * 真正的空 payload codec：不写任何字段，解码得到 {@link #INSTANCE}。
     * 绝不使用 {@code StreamCodec.unit(...)} —— 那个是按引用相等的。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, NoteSyncRequestPacket> STREAM_CODEC =
            CustomPacketPayload.codec(NoteSyncRequestPacket::encode, NoteSyncRequestPacket::decode);

    public NoteSyncRequestPacket() {
    }

    /** 空包：一个字节都不写。 */
    private void encode(RegistryFriendlyByteBuf buffer) {
        // intentionally empty
    }

    private static NoteSyncRequestPacket decode(RegistryFriendlyByteBuf buffer) {
        return INSTANCE;
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
