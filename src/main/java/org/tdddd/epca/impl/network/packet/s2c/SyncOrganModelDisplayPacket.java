package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.organ.model.OrganModelClientData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.OrganModelDisplay;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：某个玩家的「内器官模型显示」开关 + 头部/躯干内部物品。
 *
 * <p>渲染层需要任意玩家（自己以及其他玩家）的数据，所以按 UUID 广播，而不是塞进
 * 只服务于本机 UI 的 {@link SyncHiveLeaderOrgansPacket}。</p>
 */
public class SyncOrganModelDisplayPacket {

    private final UUID playerId;
    private final boolean visible;
    private final CompoundTag organs;

    public SyncOrganModelDisplayPacket(UUID playerId, boolean visible,
                                       HiveLeaderOrganData data) {
        this.playerId = playerId;
        this.visible = visible;
        this.organs = OrganModelDisplay.encodeVisibleOrgans(data);
    }

    private SyncOrganModelDisplayPacket(UUID playerId, boolean visible, CompoundTag organs) {
        this.playerId = playerId;
        this.visible = visible;
        this.organs = organs == null ? new CompoundTag() : organs;
    }

    public static void encode(SyncOrganModelDisplayPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId == null ? new UUID(0L, 0L) : msg.playerId);
        buf.writeBoolean(msg.visible);
        buf.writeNbt(msg.organs);
    }

    public static SyncOrganModelDisplayPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        boolean visible = buf.readBoolean();
        CompoundTag tag = buf.readNbt();
        return new SyncOrganModelDisplayPacket(id, visible, tag);
    }

    public static void handle(SyncOrganModelDisplayPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                OrganModelClientData.onSync(msg.playerId, msg.visible, msg.organs));
        context.setPacketHandled(true);
    }
}
