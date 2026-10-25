package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 *  -&gt;
 *
 * <h2></h2>
 * <p><b></b> {@link HiveLeaderOrganTeleportPacket} /
 * {@link RequestOpenHiveLeaderOrgansPacket}
 *  +  +
 *  id /
 *
 * <b></b>{@code ServerPlayer#getEyePosition()} /
 * {@code getLookAngle()}
 * {@link HiveLeaderDecomposeParasiteHandler#pickParasite}</p>
 *
 * <h2></h2>
 * <p> {@link HiveLeaderDecomposeParasiteHandler#handleRequest(ServerPlayer)}
 *  +  sender </p>
 * <ol>
 *   <li> C2S{@code getDirection().getReceptionSide().isServer()}
 *        {@code getSender()}  null</li>
 *   <li>{@code HiveLeaderManager#isHiveLeader(sender.getUUID())}</li>
 *   <li>{@code LivingEntity}  {@code instanceof IParasite}
 *        {@code Player} </li>
 *   <li> {@code BiomassEventHandler#computeParasiteBiomass}
 *       </li>
 *   <li><b></b>{@code IParasite#isBiomassSpawned()}
 *       </li>
 *   <li>{@code Entity#discard()} <b></b> {@code die()}
 *        {@code LivingDeathEvent}</li>
 *   <li> + {@code BiomassManager#addBiomassPoints} + {@code BiomassSyncPacket} </li>
 * </ol>
 */
public class HiveLeaderDecomposeParasitePacket {

    public HiveLeaderDecomposeParasitePacket() {
    }

    public HiveLeaderDecomposeParasitePacket(FriendlyByteBuf buf) {
    }

    public static void encode(HiveLeaderDecomposeParasitePacket msg, FriendlyByteBuf buf) {
    }

    public static HiveLeaderDecomposeParasitePacket decode(FriendlyByteBuf buf) {
        return new HiveLeaderDecomposeParasitePacket();
    }

    public static void handle(HiveLeaderDecomposeParasitePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            HiveLeaderDecomposeParasiteHandler.handleRequest(sender);
        });
        context.setPacketHandled(true);
    }
}

