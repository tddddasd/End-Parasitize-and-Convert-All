package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.events.NoteUnlockEvents;
import org.tdddd.epca.impl.network.ModNetwork;

/**
 * 请求解锁某条笔记的「详细数据」（C2S）。
 *
 * <p>客户端只负责“我想解锁这个实体”，**不做任何本地判定**：</p>
 * <ul>
 *   <li>服务端校验实体 id、该条目是否已发现、是否已解锁过、背包材料是否足够；</li>
 *   <li>校验通过才扣除材料、写持久化，然后回一份
 *       {@link org.tdddd.epca.impl.network.packet.s2c.SyncNoteUnlocksPacket}；</li>
 *   <li>材料不足 / 其它失败时只同步一次状态，不改任何数据。</li>
 * </ul>
 * 界面上“已解锁详细数据”的状态完全由服务端同步驱动，客户端不会自己点亮。
 *
 * <p><b>26.1.2 移植说明</b>：{@code ResourceLocation} -&gt; {@code Identifier}；
 * 处理函数从 {@code (msg, Supplier<NetworkEvent.Context>)} 换成 {@code (msg, IPayloadContext)}，
 * 方向判断（{@code getDirection().getReceptionSide().isServer()}）在 payload 体系里天然成立
 * —— 注册成 {@code playToServer} 的包只会在服务端被调用。</p>
 */
public class NoteDetailUnlockRequestPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NoteDetailUnlockRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("note_detail_unlock_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NoteDetailUnlockRequestPacket> STREAM_CODEC =
            CustomPacketPayload.codec(NoteDetailUnlockRequestPacket::encode, NoteDetailUnlockRequestPacket::decode);

    private final Identifier entityId;

    public NoteDetailUnlockRequestPacket(Identifier entityId) {
        this.entityId = entityId;
    }

    public Identifier entityId() {
        return entityId;
    }

    public static void encode(NoteDetailUnlockRequestPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeUtf(msg.entityId == null ? "" : msg.entityId.toString(), 256);
    }

    public static NoteDetailUnlockRequestPacket decode(RegistryFriendlyByteBuf buf) {
        return new NoteDetailUnlockRequestPacket(Identifier.tryParse(buf.readUtf(256)));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NoteDetailUnlockRequestPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!(player instanceof ServerPlayer sender) || msg.entityId == null) {
                return;
            }
            NoteUnlockEvents.handleDetailUnlockRequest(sender, msg.entityId);
        });
    }
}
