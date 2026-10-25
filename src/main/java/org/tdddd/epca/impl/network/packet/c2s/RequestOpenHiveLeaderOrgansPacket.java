package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveLeaderOrgansPacket;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;

/**
 *  -&gt;  GUISPEC  2  1
 *
 * <h2></h2>
 * <p><b></b> GUI {@code IPayloadContext#player()}
 * </p>
 *
 * <h2></h2>
 * <ol>
 *   <li>{@code ctx.player()}  {@link ServerPlayer}</li>
 *   <li>{@link HiveLeaderManager#isHiveLeader(java.util.UUID)}  true
 *
 *       {@code HiveLeaderHandler} /  /
 *       {@code IParasite}  {@code BiomassEventHandler}
 *       <b></b></li>
 *   <li> {@link HiveLeaderOrganSavedData#readOrCreate} SPEC
 *        {@link HiveLeaderOrganGate#scan}
 *       {@link SyncHiveLeaderOrgansPacket}</li>
 * </ol>
 *
 * <p> GUI
 * </p>
 *
 * <h2>1.20.1 -&gt; 26.1.2 </h2>
 * <p>1.20.1  {@code SimpleChannel#registerMessage}{@code FriendlyByteBuf} +
 * {@code NetworkEvent.Context} + {@code Supplier} 26.1.2
 * {@code CustomPacketPayload} + {@link StreamCodec} + {@link IPayloadContext}
 *  {@link CustomPacketPayload} {@code TYPE}  {@code STREAM_CODEC}
 *  {@code static void handle(P, IPayloadContext)}
 *  playToServer/playToClient+ {@code ctx.player()}
 *  {@code getDirection().getReceptionSide().isServer()}
 *  {@code KeyPressPacket} / {@code BiomassSyncPacket} </p>
 */
public class RequestOpenHiveLeaderOrgansPacket implements CustomPacketPayload {

    /** 26.1.2  id */
    public static final CustomPacketPayload.Type<RequestOpenHiveLeaderOrgansPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("hiveleader_open_organs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestOpenHiveLeaderOrgansPacket> STREAM_CODEC =
            CustomPacketPayload.codec(RequestOpenHiveLeaderOrgansPacket::encode,
                    RequestOpenHiveLeaderOrgansPacket::decode);

    public RequestOpenHiveLeaderOrgansPacket() {
    }

    /**  codec  */
    public static void encode(RequestOpenHiveLeaderOrgansPacket packet, RegistryFriendlyByteBuf buf) {
    }

    public static RequestOpenHiveLeaderOrgansPacket decode(RegistryFriendlyByteBuf buf) {
        return new RequestOpenHiveLeaderOrgansPacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenHiveLeaderOrgansPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            if (!HiveLeaderManager.isHiveLeader(sender.getUUID())) {
                // 26.1.2displayClientMessage(Component, true) -> ServerPlayer#sendSystemMessage(Component, true)
                sender.sendSystemMessage(Component.literal("只有领巢者才能打开器官界面"), true);
                return;
            }

            HiveLeaderOrganData data = HiveLeaderOrganSavedData.readOrCreate(sender);
            HiveLeaderOrganGate.Gates gates = HiveLeaderOrganGate.scan(sender);
            ModNetwork.sendToPlayer(sender, new SyncHiveLeaderOrgansPacket(data, gates));
        });
    }
}

