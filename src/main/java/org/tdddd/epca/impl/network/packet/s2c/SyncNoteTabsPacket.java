package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.EPCANoteTabData;

import java.util.List;

/**
 * 服务端 → 客户端：笔记标签页同步。
 *
 * <p><b>26.1.2 改动</b>：{@code SimpleChannel} → {@link CustomPacketPayload}。
 * 读写仍走 {@code EPCANoteTabData#writeParentTabList/readParentTabList}
 * （它们接收的 {@code FriendlyByteBuf} 是 {@code RegistryFriendlyByteBuf} 的父类型，
 * 调用点无需改动，线上格式不变）。
 */
public class SyncNoteTabsPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncNoteTabsPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_note_tabs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNoteTabsPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncNoteTabsPacket::encode, SyncNoteTabsPacket::decode);

    private final List<EPCANoteTabData.ParentTab> tabs;

    public SyncNoteTabsPacket(List<EPCANoteTabData.ParentTab> tabs) {
        this.tabs = tabs;
    }

    public static void encode(SyncNoteTabsPacket msg, RegistryFriendlyByteBuf buf) {
        EPCANoteTabData.writeParentTabList(buf, msg.tabs);
    }

    public static SyncNoteTabsPacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncNoteTabsPacket(EPCANoteTabData.readParentTabList(buf));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncNoteTabsPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            EPCANoteTabData.setClientTabs(msg.tabs);
        });
    }
}
