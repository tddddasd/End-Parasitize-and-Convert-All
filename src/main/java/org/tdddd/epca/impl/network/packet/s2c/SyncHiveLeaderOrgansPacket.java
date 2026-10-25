package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;
import org.tdddd.epca.impl.client.organ.HiveLeaderOrganClientData;

import java.util.function.Supplier;

/**
 *  -&gt;  GUI  /  /
 * SPEC  2  1{@code S2C  + }
 *
 * <h2></h2>
 * <p> {@link HiveLeaderOrganData}  {@code writeNbt}
 *  {@code Items}52  {@code ItemStack#save}
 * {@code Unlocked}52 52 " + 52 "
 *  27  {@code FriendlyByteBuf#readNbt()}  2 MiB
 * </p>
 *
 * <p> {@link HiveLeaderOrganGate.Gates} <b></b>
 * 39 / 33 STAGE 2
 *
 * {@code StageIBeckon}/{@code StageIIBeckon} </p>
 *
 * <h2>STAGE 2 {@code openScreen}</h2>
 * <p>STAGE 2 <b></b>
 *  {@code setScreen(new HiveLeaderOrganScreen())}
 * </p>
 * <ul>
 *   <li>{@code openScreen = true}STAGE 1 ""
 *        + </li>
 *   <li>{@code openScreen = false}STAGE 2
 *       //</li>
 * </ul>
 * <p>{@code ModNetwork#PROTOCOL_VERSION}
 * </p>
 *
 * <h2></h2>
 * <ol>
 *   <li></li>
 *   <li>{@code openScreen}  true  {@code HiveLeaderOrganScreen}</li>
 * </ol>
 * <p> {@code org.tdddd.epca.impl.client.organ.HiveLeaderOrganClientData}
 *  {@code handle}  {@code enqueueWork} lambda <b></b>
 *  {@code SyncHotbarMarkPacket} / {@code ScreenCorruptionPacket}
 *  {@code BioTortClientState}
 * </p>
 */
public class SyncHiveLeaderOrgansPacket {

    private final CompoundTag organData;
    private final boolean torsoInnerOpen;
    private final boolean headInnerOpen;
    private final boolean openScreen;

    /** STAGE 1  */
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
            // =  STAGE 1
            //  setScreen
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

