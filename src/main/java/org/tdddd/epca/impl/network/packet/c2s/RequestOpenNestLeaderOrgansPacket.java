package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNestLeaderOrgansPacket;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganSavedData;

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
 *   <li>{@link NestLeaderManager#isNestLeader(java.util.UUID)}  true
 *
 *       {@code NestLeaderHandler} /  /
 *       {@code IParasite}  {@code BiomassEventHandler}
 *       <b></b></li>
 *   <li> {@link NestLeaderOrganSavedData#readOrCreate} SPEC
 *        {@link NestLeaderOrganGate#scan}
 *       {@link SyncNestLeaderOrgansPacket}</li>
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
public class RequestOpenNestLeaderOrgansPacket implements CustomPacketPayload {

    /** 26.1.2  id */
    public static final CustomPacketPayload.Type<RequestOpenNestLeaderOrgansPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("nestleader_open_organs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestOpenNestLeaderOrgansPacket> STREAM_CODEC =
            CustomPacketPayload.codec(RequestOpenNestLeaderOrgansPacket::encode,
                    RequestOpenNestLeaderOrgansPacket::decode);

    public RequestOpenNestLeaderOrgansPacket() {
    }

    /**  codec  */
    public static void encode(RequestOpenNestLeaderOrgansPacket packet, RegistryFriendlyByteBuf buf) {
    }

    public static RequestOpenNestLeaderOrgansPacket decode(RegistryFriendlyByteBuf buf) {
        return new RequestOpenNestLeaderOrgansPacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenNestLeaderOrgansPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            if (!NestLeaderManager.isNestLeader(sender.getUUID())) {
                // 26.1.2displayClientMessage(Component, true) -> ServerPlayer#sendSystemMessage(Component, true)
                sender.sendSystemMessage(Component.literal("只有领巢者才能打开器官界面"), true);
                return;
            }

            NestLeaderOrganData data = NestLeaderOrganSavedData.readOrCreate(sender);
            NestLeaderOrganGate.Gates gates = NestLeaderOrganGate.scan(sender);
            ModNetwork.sendToPlayer(sender, new SyncNestLeaderOrgansPacket(data, gates));
        });
    }
}

