package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.client.organ.NestLeaderOrganClientData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganGate;

/**
 *  -&gt;  GUI  /  /
 * SPEC  2  1{@code S2C  + }
 *
 * <h2></h2>
 * <p> {@link NestLeaderOrganData}  {@code writeNbt}
 *  {@code Items}52  {@code ItemStack#save}
 * {@code Unlocked}52 52 " + 52 "
 *  27  {@code FriendlyByteBuf#readNbt()}
 * </p>
 *
 * <p>26.1.2{@code FriendlyByteBuf#writeNbt(Tag)} / {@code #readNbt()}
 * _tmp_26src FriendlyByteBuf.java  517 / 534  1.20.1 </p>
 *
 * <h2>{@code openScreen}</h2>
 * <p>STAGE 2 <b></b>
 *  {@code setScreen(new NestLeaderOrganScreen())}
 * </p>
 * <ul>
 *   <li>{@code openScreen = true}""
 *        + </li>
 *   <li>{@code openScreen = false}
 *       //</li>
 * </ul>
 *
 * <h2></h2>
 * <ol>
 *   <li></li>
 *   <li>{@code openScreen}  true  {@code NestLeaderOrganScreen}</li>
 * </ol>
 * <p> {@code org.tdddd.epca.impl.client.organ.NestLeaderOrganClientData}
 *  {@code handle}  {@code enqueueWork} lambda <b></b>
 *  {@code BiomassSyncPacket}  {@code BiomassClientData}
 * </p>
 */
public class SyncNestLeaderOrgansPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncNestLeaderOrgansPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("nestleader_sync_organs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNestLeaderOrgansPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncNestLeaderOrgansPacket::encode,
                    SyncNestLeaderOrgansPacket::decode);

    private final CompoundTag organData;
    private final boolean torsoInnerOpen;
    private final boolean headInnerOpen;
    private final boolean openScreen;

    /**  */
    public SyncNestLeaderOrgansPacket(NestLeaderOrganData data, NestLeaderOrganGate.Gates gates) {
        this(data, gates, true);
    }

    public SyncNestLeaderOrgansPacket(NestLeaderOrganData data, NestLeaderOrganGate.Gates gates,
                                      boolean openScreen) {
        CompoundTag tag = new CompoundTag();
        data.save(tag);
        this.organData = tag;
        this.torsoInnerOpen = gates != null && gates.torsoInnerOpen();
        this.headInnerOpen = gates != null && gates.headInnerOpen();
        this.openScreen = openScreen;
    }

    public SyncNestLeaderOrgansPacket(CompoundTag organData, boolean torsoInnerOpen, boolean headInnerOpen) {
        this(organData, torsoInnerOpen, headInnerOpen, true);
    }

    public SyncNestLeaderOrgansPacket(CompoundTag organData, boolean torsoInnerOpen, boolean headInnerOpen,
                                      boolean openScreen) {
        this.organData = organData == null ? new CompoundTag() : organData;
        this.torsoInnerOpen = torsoInnerOpen;
        this.headInnerOpen = headInnerOpen;
        this.openScreen = openScreen;
    }

    public boolean openScreen() {
        return openScreen;
    }

    public static void encode(SyncNestLeaderOrgansPacket packet, RegistryFriendlyByteBuf buf) {
        buf.writeNbt(packet.organData);
        buf.writeBoolean(packet.torsoInnerOpen);
        buf.writeBoolean(packet.headInnerOpen);
        buf.writeBoolean(packet.openScreen);
    }

    public static SyncNestLeaderOrgansPacket decode(RegistryFriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        boolean torso = buf.readBoolean();
        boolean head = buf.readBoolean();
        boolean openScreen = buf.readBoolean();
        return new SyncNestLeaderOrgansPacket(tag, torso, head, openScreen);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncNestLeaderOrgansPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // =
            //  setScreen
            boolean screenAlreadyOpen = NestLeaderOrganClientData.isScreenOpen();
            NestLeaderOrganClientData.onSync(packet.organData, packet.torsoInnerOpen, packet.headInnerOpen,
                    screenAlreadyOpen);
            if (packet.openScreen && !screenAlreadyOpen) {
                NestLeaderOrganClientData.openScreen();
            }
        });
    }
}

