package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;
import org.tdddd.epca.impl.client.organ.HiveLeaderOrganClientData;

import java.util.function.Supplier;

public class SyncHiveLeaderOrgansPacket {

    private final CompoundTag organData;
    private final boolean torsoInnerOpen;
    private final boolean headInnerOpen;
    private final boolean openScreen;

    public SyncHiveLeaderOrgansPacket(HiveLeaderOrganData data, HiveLeaderOrganGate.Gates gates) {
        this(data, gates, true);
    }

    public SyncHiveLeaderOrgansPacket(HiveLeaderOrganData data, HiveLeaderOrganGate.Gates gates,
                                      boolean openScreen) {
        CompoundTag tag = new CompoundTag();
        data.save(tag);
        this.organData = tag;
        this.torsoInnerOpen = gates != null && gates.torsoInnerOpen();
        this.headInnerOpen = gates != null && gates.headInnerOpen();
        this.openScreen = openScreen;
    }

    public SyncHiveLeaderOrgansPacket(CompoundTag organData, boolean torsoInnerOpen, boolean headInnerOpen) {
        this(organData, torsoInnerOpen, headInnerOpen, true);
    }

    public SyncHiveLeaderOrgansPacket(CompoundTag organData, boolean torsoInnerOpen, boolean headInnerOpen,
                                      boolean openScreen) {
        this.organData = organData == null ? new CompoundTag() : organData;
        this.torsoInnerOpen = torsoInnerOpen;
        this.headInnerOpen = headInnerOpen;
        this.openScreen = openScreen;
    }

    public boolean openScreen() {
        return openScreen;
    }

    public static void encode(SyncHiveLeaderOrgansPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.organData);
        buf.writeBoolean(msg.torsoInnerOpen);
        buf.writeBoolean(msg.headInnerOpen);
        buf.writeBoolean(msg.openScreen);
    }

    public static SyncHiveLeaderOrgansPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        boolean torso = buf.readBoolean();
        boolean head = buf.readBoolean();
        boolean openScreen = buf.readBoolean();
        return new SyncHiveLeaderOrgansPacket(tag, torso, head, openScreen);
    }

    public static void handle(SyncHiveLeaderOrgansPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            boolean screenAlreadyOpen = HiveLeaderOrganClientData.isScreenOpen();
            HiveLeaderOrganClientData.onSync(msg.organData, msg.torsoInnerOpen, msg.headInnerOpen,
                    screenAlreadyOpen);
            if (msg.openScreen && !screenAlreadyOpen) {
                HiveLeaderOrganClientData.openScreen();
            }
        });
        context.setPacketHandled(true);
    }
}
