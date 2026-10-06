package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 *  -&gt; SPEC  B
 *
 * <h2></h2>
 * <p><b></b> {@link RequestOpenNestLeaderOrgansPacket}
 *
 * /
 * <b></b>{@code ServerPlayer#getLookAngle()}
 *  {@link NestLeaderOrganTeleportHandler}</p>
 *
 * <h2></h2>
 * <p> {@link NestLeaderOrganTeleportHandler#handleRequest(ServerPlayer)}
 *  +  sender </p>
 * <ol>
 *   <li> C2S{@code getDirection().getReceptionSide().isServer()}
 *        {@code getSender()}  null</li>
 *   <li>{@code NestLeaderManager#isNestLeader(sender.getUUID())}</li>
 *   <li>{@code NestLeaderOrganSavedData#readOrCreate}
 *        33   2 <b></b></li>
 *   <li>20 /</li>
 *   <li><b></b></li>
 * </ol>
 */
public class NestLeaderOrganTeleportPacket {

    public NestLeaderOrganTeleportPacket() {
    }

    public NestLeaderOrganTeleportPacket(FriendlyByteBuf buf) {
    }

    public static void encode(NestLeaderOrganTeleportPacket msg, FriendlyByteBuf buf) {
    }

    public static NestLeaderOrganTeleportPacket decode(FriendlyByteBuf buf) {
        return new NestLeaderOrganTeleportPacket();
    }

    public static void handle(NestLeaderOrganTeleportPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            NestLeaderOrganTeleportHandler.handleRequest(sender);
        });
        context.setPacketHandled(true);
    }
}

