package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;

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
 *  sender </p>
 * <ol>
 *   <li>{@code ctx.player()}  {@link ServerPlayer}</li>
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
public class NestLeaderDecomposeParasitePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NestLeaderDecomposeParasitePacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("nestleader_decompose_parasite"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NestLeaderDecomposeParasitePacket> STREAM_CODEC =
            CustomPacketPayload.codec(NestLeaderDecomposeParasitePacket::encode,
                    NestLeaderDecomposeParasitePacket::decode);

    public NestLeaderDecomposeParasitePacket() {
    }

    public static void encode(NestLeaderDecomposeParasitePacket packet, RegistryFriendlyByteBuf buf) {
    }

    public static NestLeaderDecomposeParasitePacket decode(RegistryFriendlyByteBuf buf) {
        return new NestLeaderDecomposeParasitePacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NestLeaderDecomposeParasitePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            NestLeaderDecomposeParasiteHandler.handleRequest(sender);
        });
    }
}

