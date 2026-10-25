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
 * <p><b></b> {@link RequestOpenHiveLeaderOrgansPacket}
 *
 * /
 * <b></b>{@code ServerPlayer#getLookAngle()}
 *  {@link HiveLeaderOrganTeleportHandler}</p>
 *
 * <h2></h2>
 * <p> {@link HiveLeaderOrganTeleportHandler#handleRequest(ServerPlayer)}
 *  sender </p>
 * <ol>
 *   <li>{@code ctx.player()}  {@link ServerPlayer}</li>
 *   <li>{@code HiveLeaderManager#isHiveLeader(sender.getUUID())}</li>
 *   <li>{@code HiveLeaderOrganSavedData#readOrCreate}
 *        33   2 <b></b></li>
 *   <li>20 /</li>
 *   <li><b></b></li>
 * </ol>
 */
public class HiveLeaderOrganTeleportPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HiveLeaderOrganTeleportPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("hiveleader_organ_teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HiveLeaderOrganTeleportPacket> STREAM_CODEC =
            CustomPacketPayload.codec(HiveLeaderOrganTeleportPacket::encode,
                    HiveLeaderOrganTeleportPacket::decode);

    public HiveLeaderOrganTeleportPacket() {
    }

    public static void encode(HiveLeaderOrganTeleportPacket packet, RegistryFriendlyByteBuf buf) {
    }

    public static HiveLeaderOrganTeleportPacket decode(RegistryFriendlyByteBuf buf) {
        return new HiveLeaderOrganTeleportPacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HiveLeaderOrganTeleportPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            HiveLeaderOrganTeleportHandler.handleRequest(sender);
        });
    }
}

