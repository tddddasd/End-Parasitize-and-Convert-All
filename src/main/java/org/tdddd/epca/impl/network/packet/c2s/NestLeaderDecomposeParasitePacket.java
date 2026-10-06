package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 *  -&gt;
 *
 * <h2></h2>
 * <p><b></b> {@link NestLeaderOrganTeleportPacket} /
 * {@link RequestOpenNestLeaderOrgansPacket}
 *  +  +
 *  id /
 *
 * <b></b>{@code ServerPlayer#getEyePosition()} /
 * {@code getLookAngle()}
 * {@link NestLeaderDecomposeParasiteHandler#pickParasite}</p>
 *
 * <h2></h2>
 * <p> {@link NestLeaderDecomposeParasiteHandler#handleRequest(ServerPlayer)}
 *  +  sender </p>
 * <ol>
 *   <li> C2S{@code getDirection().getReceptionSide().isServer()}
 *        {@code getSender()}  null</li>
 *   <li>{@code NestLeaderManager#isNestLeader(sender.getUUID())}</li>
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
public class NestLeaderDecomposeParasitePacket {

    public NestLeaderDecomposeParasitePacket() {
    }

    public NestLeaderDecomposeParasitePacket(FriendlyByteBuf buf) {
    }

    public static void encode(NestLeaderDecomposeParasitePacket msg, FriendlyByteBuf buf) {
    }

    public static NestLeaderDecomposeParasitePacket decode(FriendlyByteBuf buf) {
        return new NestLeaderDecomposeParasitePacket();
    }

    public static void handle(NestLeaderDecomposeParasitePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            NestLeaderDecomposeParasiteHandler.handleRequest(sender);
        });
        context.setPacketHandled(true);
    }
}

