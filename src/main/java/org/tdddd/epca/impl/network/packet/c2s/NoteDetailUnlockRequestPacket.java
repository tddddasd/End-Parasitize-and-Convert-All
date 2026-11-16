package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.events.NoteUnlockEvents;

import java.util.function.Supplier;

/**
 * 请求解锁某条笔记的「详细数据」（C2S）。
 *
 * <p>客户端只负责“我想解锁这个实体”，**不做任何本地判定**：</p>
 * <ul>
 *   <li>服务端校验实体 id、该条目是否已发现、是否已解锁过、背包材料是否足够；</li>
 *   <li>校验通过才扣除材料、写持久化，然后回一份
 *       {@link org.tdddd.epca.impl.network.packet.s2c.SyncNoteUnlocksPacket}；</li>
 *   <li>材料不足 / 其它失败时只给玩家一条聊天栏提示，不改任何状态。</li>
 * </ul>
 * 界面上“已解锁详细数据”的状态完全由服务端同步驱动，客户端不会自己点亮。
 */
public class NoteDetailUnlockRequestPacket {

    private final ResourceLocation entityId;

    public NoteDetailUnlockRequestPacket(ResourceLocation entityId) {
        this.entityId = entityId;
    }

    public ResourceLocation entityId() {
        return entityId;
    }

    public static void encode(NoteDetailUnlockRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.entityId == null ? "" : msg.entityId.toString(), 256);
    }

    public static NoteDetailUnlockRequestPacket decode(FriendlyByteBuf buf) {
        return new NoteDetailUnlockRequestPacket(ResourceLocation.tryParse(buf.readUtf(256)));
    }

    public static void handle(NoteDetailUnlockRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null || msg.entityId == null) {
                return;
            }
            NoteUnlockEvents.handleDetailUnlockRequest(sender, msg.entityId);
        });
        context.setPacketHandled(true);
    }
}
