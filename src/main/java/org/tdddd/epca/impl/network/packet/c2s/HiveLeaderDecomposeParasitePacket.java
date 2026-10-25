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
 *  sender </p>
 * <ol>
 *   <li>{@code ctx.player()}  {@link ServerPlayer}</li>
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
public class HiveLeaderDecomposeParasitePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HiveLeaderDecomposeParasitePacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("hiveleader_decompose_parasite"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HiveLeaderDecomposeParasitePacket> STREAM_CODEC =
            CustomPacketPayload.codec(HiveLeaderDecomposeParasitePacket::encode,
                    HiveLeaderDecomposeParasitePacket::decode);

    public HiveLeaderDecomposeParasitePacket() {
    }

    public static void encode(HiveLeaderDecomposeParasitePacket packet, RegistryFriendlyByteBuf buf) {
    }

    public static HiveLeaderDecomposeParasitePacket decode(RegistryFriendlyByteBuf buf) {
        return new HiveLeaderDecomposeParasitePacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HiveLeaderDecomposeParasitePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            HiveLeaderDecomposeParasiteHandler.handleRequest(sender);
        });
    }
}

