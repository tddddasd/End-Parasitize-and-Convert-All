package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.EPCANoteTabData;

import java.util.List;
import java.util.function.Supplier;

public class SyncNoteTabsPacket {
    private final List<EPCANoteTabData.ParentTab> tabs;

    public SyncNoteTabsPacket(List<EPCANoteTabData.ParentTab> tabs) {
        this.tabs = tabs;
    }

    public static void encode(SyncNoteTabsPacket msg, FriendlyByteBuf buf) {
        EPCANoteTabData.writeParentTabList(buf, msg.tabs);
    }

    public static SyncNoteTabsPacket decode(FriendlyByteBuf buf) {
        return new SyncNoteTabsPacket(EPCANoteTabData.readParentTabList(buf));
    }

    public static void handle(SyncNoteTabsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            
            EPCANoteTabData.setClientTabs(msg.tabs);
        });
        ctx.get().setPacketHandled(true);
    }
}