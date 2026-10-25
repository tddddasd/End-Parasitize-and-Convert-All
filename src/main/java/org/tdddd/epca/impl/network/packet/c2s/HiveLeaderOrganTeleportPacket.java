package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 *  -&gt; SPEC  B
 *
 * <h2></h2>
 * <p><b></b> {@link RequestOpenHiveLeaderOrgansPacket}
 *
 * /
 * <b></b>{@code ServerPlayer#getLookAngle()}
 *  {@link HiveLeaderOrganTeleportHandler}</p>
 *
 * <h2></h2>
 * <p> {@link HiveLeaderOrganTeleportHandler#handleRequest(ServerPlayer)}
 *  +  sender </p>
 * <ol>
 *   <li> C2S{@code getDirection().getReceptionSide().isServer()}
 *        {@code getSender()}  null</li>
 *   <li>{@code HiveLeaderManager#isHiveLeader(sender.getUUID())}</li>
 *   <li>{@code HiveLeaderOrganSavedData#readOrCreate}
 *        33   2 <b></b></li>
 *   <li>20 /</li>
 *   <li><b></b></li>
 * </ol>
 */
public class HiveLeaderOrganTeleportPacket {

    public HiveLeaderOrganTeleportPacket() {
    }

    public HiveLeaderOrganTeleportPacket(FriendlyByteBuf buf) {
    }

    public static void encode(HiveLeaderOrganTeleportPacket msg, FriendlyByteBuf buf) {
    }

    public static HiveLeaderOrganTeleportPacket decode(FriendlyByteBuf buf) {
        return new HiveLeaderOrganTeleportPacket();
    }

    public static void handle(HiveLeaderOrganTeleportPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            HiveLeaderOrganTeleportHandler.handleRequest(sender);
        });
        context.setPacketHandled(true);
    }
}

