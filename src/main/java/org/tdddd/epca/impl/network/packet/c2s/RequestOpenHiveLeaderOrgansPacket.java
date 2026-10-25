package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveLeaderOrgansPacket;

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
 *   <li>{@link HiveLeaderManager#isHiveLeader(java.util.UUID)}  true
 *
 *       {@code HiveLeaderHandler} /  /
 *       {@code IParasite#isParasiteByTagOrInterface}{@code BiomassEventHandler}
 *       <b></b></li>
 *   <li> {@link HiveLeaderOrganSavedData#readOrCreate} SPEC
 *        {@link HiveLeaderOrganGate#scan}
 *       {@link SyncHiveLeaderOrgansPacket}</li>
 * </ol>
 *
 * <p> GUI
 *  {@code HiveLeaderOrganScreen}</p>
 */
public class RequestOpenHiveLeaderOrgansPacket {

    public RequestOpenHiveLeaderOrgansPacket() {
    }

    public RequestOpenHiveLeaderOrgansPacket(FriendlyByteBuf buf) {
    }

    public static void encode(RequestOpenHiveLeaderOrgansPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestOpenHiveLeaderOrgansPacket decode(FriendlyByteBuf buf) {
        return new RequestOpenHiveLeaderOrgansPacket();
    }

    public static void handle(RequestOpenHiveLeaderOrgansPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!HiveLeaderManager.isHiveLeader(sender.getUUID())) {
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("只有领巢者才能打开器官界面"), true);
                return;
            }

            HiveLeaderOrganData data = HiveLeaderOrganSavedData.readOrCreate(sender);
            HiveLeaderOrganGate.Gates gates = HiveLeaderOrganGate.scan(sender);
            ModNetwork.sendToPlayer(sender, new SyncHiveLeaderOrgansPacket(data, gates));
        });
        context.setPacketHandled(true);
    }
}

