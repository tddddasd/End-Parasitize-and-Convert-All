package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveLeaderOrgansPacket;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganGate;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;

public class RequestOpenHiveLeaderOrgansPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestOpenHiveLeaderOrgansPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("hiveleader_open_organs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestOpenHiveLeaderOrgansPacket> STREAM_CODEC =
            CustomPacketPayload.codec(RequestOpenHiveLeaderOrgansPacket::encode,
                    RequestOpenHiveLeaderOrgansPacket::decode);

    public RequestOpenHiveLeaderOrgansPacket() {
    }

    public static void encode(RequestOpenHiveLeaderOrgansPacket packet, RegistryFriendlyByteBuf buf) {
    }

    public static RequestOpenHiveLeaderOrgansPacket decode(RegistryFriendlyByteBuf buf) {
        return new RequestOpenHiveLeaderOrgansPacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenHiveLeaderOrgansPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            if (!HiveLeaderManager.isHiveLeader(sender.getUUID())) {

                sender.sendSystemMessage(Component.literal("只有领巢者才能打开器官界面"), true);
                return;
            }

            HiveLeaderOrganData data = HiveLeaderOrganSavedData.readOrCreate(sender);
            HiveLeaderOrganGate.Gates gates = HiveLeaderOrganGate.scan(sender);
            ModNetwork.sendToPlayer(sender, new SyncHiveLeaderOrgansPacket(data, gates));
        });
    }
}
