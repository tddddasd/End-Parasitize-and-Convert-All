package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.EPCANoteClientUnlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

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
 */
public class SyncNoteUnlocksPacket {

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

    public static void encode(SyncNoteUnlocksPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        writeList(buf, msg.unlocked);
        writeList(buf, msg.detailUnlocked);
    }

    public static SyncNoteUnlocksPacket decode(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        List<String> unlocked = readList(buf);
        List<String> detail = readList(buf);
        return new SyncNoteUnlocksPacket(playerId, unlocked, detail);
    }

    public static void handle(SyncNoteUnlocksPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                EPCANoteClientUnlocks.apply(msg.playerId, msg.unlocked, msg.detailUnlocked));
        context.setPacketHandled(true);
    }

    private static void writeList(FriendlyByteBuf buf, List<String> ids) {
        buf.writeVarInt(ids.size());
        for (String id : ids) {
            buf.writeUtf(id == null ? "" : id, 256);
        }
    }

    private static List<String> readList(FriendlyByteBuf buf) {
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
