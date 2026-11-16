package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.EPCANoteClientUnlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 虫咒笔记解锁状态同步（S2C）。
 *
 * <p>内容 = 某个玩家的两份集合：已发现的实体 id、详细数据已解锁的实体 id。
 * 发送时机：</p>
 * <ul>
 *   <li>玩家登录时（{@code ServerNoteEvents#onPlayerLoggedIn}）；</li>
 *   <li>玩家打开笔记时（客户端发 {@link org.tdddd.epca.impl.network.packet.c2s.NoteSyncRequestPacket}）；</li>
 *   <li>刚刚解锁某个条目 / 成功消耗材料解锁详细数据时（只发给该玩家）。</li>
 * </ul>
 *
 * <p>客户端收到后写进 {@link EPCANoteClientUnlocks} 的静态缓存（按 UUID），
 * 如果笔记界面正开着就顺手重排一次页，保证“刚走近怪物”这种实时解锁能立刻变成正常文字。</p>
 *
 * <p><b>26.1.2 移植说明</b>（相对 1.20.1 的 SimpleChannel/NetworkEvent 写法）：
 * NeoForge 的 payload 体系要求实现 {@link CustomPacketPayload} 并给出
 * {@code Type} + {@link StreamCodec}；收发统一走 {@code ModNetwork}（内部是 PacketDistributor）。
 * 处理函数签名从 {@code (msg, Supplier<NetworkEvent.Context>)} 换成
 * {@code (msg, IPayloadContext)}，主线程调度由 {@code ctx.enqueueWork(...)} 负责
 * （等价于 1.20.1 的 {@code context.enqueueWork}）。</p>
 */
public class SyncNoteUnlocksPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncNoteUnlocksPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_note_unlocks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNoteUnlocksPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncNoteUnlocksPacket::encode, SyncNoteUnlocksPacket::decode);

    /** 上限：一份集合最多这么多条，防止坏包把客户端内存打爆 */
    public static final int MAX_ENTRIES = 4096;

    private final UUID playerId;
    private final List<String> unlocked;
    private final List<String> detailUnlocked;

    public SyncNoteUnlocksPacket(UUID playerId, List<String> unlocked, List<String> detailUnlocked) {
        this.playerId = playerId == null ? new UUID(0L, 0L) : playerId;
        this.unlocked = unlocked == null ? List.of() : List.copyOf(unlocked);
        this.detailUnlocked = detailUnlocked == null ? List.of() : List.copyOf(detailUnlocked);
    }

    public UUID playerId() {
        return playerId;
    }

    public List<String> unlocked() {
        return unlocked;
    }

    public List<String> detailUnlocked() {
        return detailUnlocked;
    }

    public static void encode(SyncNoteUnlocksPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        writeList(buf, msg.unlocked);
        writeList(buf, msg.detailUnlocked);
    }

    public static SyncNoteUnlocksPacket decode(RegistryFriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        List<String> unlocked = readList(buf);
        List<String> detail = readList(buf);
        return new SyncNoteUnlocksPacket(playerId, unlocked, detail);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncNoteUnlocksPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                EPCANoteClientUnlocks.apply(msg.playerId, msg.unlocked, msg.detailUnlocked));
    }

    private static void writeList(RegistryFriendlyByteBuf buf, List<String> ids) {
        buf.writeVarInt(ids.size());
        for (String id : ids) {
            buf.writeUtf(id == null ? "" : id, 256);
        }
    }

    private static List<String> readList(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("SyncNoteUnlocksPacket: " + size + " entries exceeds " + MAX_ENTRIES);
        }
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(256);
            if (!id.isEmpty()) {
                out.add(id);
            }
        }
        return out;
    }
}
