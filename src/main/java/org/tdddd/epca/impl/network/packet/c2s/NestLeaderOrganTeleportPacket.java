package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;

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
 *  sender </p>
 * <ol>
 *   <li>{@code ctx.player()}  {@link ServerPlayer}</li>
 *   <li>{@code NestLeaderManager#isNestLeader(sender.getUUID())}</li>
 *   <li>{@code NestLeaderOrganSavedData#readOrCreate}
 *        33   2 <b></b></li>
 *   <li>20 /</li>
 *   <li><b></b></li>
 * </ol>
 */
public class NestLeaderOrganTeleportPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NestLeaderOrganTeleportPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("nestleader_organ_teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NestLeaderOrganTeleportPacket> STREAM_CODEC =
            CustomPacketPayload.codec(NestLeaderOrganTeleportPacket::encode,
                    NestLeaderOrganTeleportPacket::decode);

    public NestLeaderOrganTeleportPacket() {
    }

    public static void encode(NestLeaderOrganTeleportPacket packet, RegistryFriendlyByteBuf buf) {
    }

    public static NestLeaderOrganTeleportPacket decode(RegistryFriendlyByteBuf buf) {
        return new NestLeaderOrganTeleportPacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NestLeaderOrganTeleportPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            NestLeaderOrganTeleportHandler.handleRequest(sender);
        });
    }
}

