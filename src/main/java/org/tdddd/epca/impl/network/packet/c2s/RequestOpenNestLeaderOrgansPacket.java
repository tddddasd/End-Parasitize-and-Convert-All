package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganSavedData;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNestLeaderOrgansPacket;

import java.util.function.Supplier;

/**
 *  -&gt;  GUISPEC  2  1
 *
 * <h2></h2>
 * <p><b></b> GUI
 * {@code NetworkEvent.Context#getSender()}
 * </p>
 *
 * <h2></h2>
 * <ol>
 *   <li>{@code sender != null}/</li>
 *   <li>{@link NestLeaderManager#isNestLeader(java.util.UUID)}  true
 *
 *       {@code NestLeaderHandler} /  /
 *       {@code IParasite#isParasiteByTagOrInterface}{@code BiomassEventHandler}
 *       <b></b></li>
 *   <li> {@link NestLeaderOrganSavedData#readOrCreate} SPEC
 *        {@link NestLeaderOrganGate#scan}
 *       {@link SyncNestLeaderOrgansPacket}</li>
 * </ol>
 *
 * <p> GUI
 *  {@code NestLeaderOrganScreen}</p>
 */
public class RequestOpenNestLeaderOrgansPacket {

    public RequestOpenNestLeaderOrgansPacket() {
    }

    public RequestOpenNestLeaderOrgansPacket(FriendlyByteBuf buf) {
    }

    public static void encode(RequestOpenNestLeaderOrgansPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestOpenNestLeaderOrgansPacket decode(FriendlyByteBuf buf) {
        return new RequestOpenNestLeaderOrgansPacket();
    }

    public static void handle(RequestOpenNestLeaderOrgansPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!NestLeaderManager.isNestLeader(sender.getUUID())) {
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("只有领巢者才能打开器官界面"), true);
                return;
            }

            NestLeaderOrganData data = NestLeaderOrganSavedData.readOrCreate(sender);
            NestLeaderOrganGate.Gates gates = NestLeaderOrganGate.scan(sender);
            ModNetwork.sendToPlayer(sender, new SyncNestLeaderOrgansPacket(data, gates));
        });
        context.setPacketHandled(true);
    }
}

