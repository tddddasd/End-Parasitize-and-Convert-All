package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;

import java.util.UUID;


public class PlayerMotionPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerMotionPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("player_motion"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerMotionPacket> STREAM_CODEC =
            CustomPacketPayload.codec(PlayerMotionPacket::toBytes, PlayerMotionPacket::new);

    private final UUID playerId;
    private final Vec3 motion;

    public PlayerMotionPacket(UUID playerId, Vec3 motion) {
        this.playerId = playerId;
        this.motion = motion;
    }

    public PlayerMotionPacket(RegistryFriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.motion = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeDouble(motion.x);
        buf.writeDouble(motion.y);
        buf.writeDouble(motion.z);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerMotionPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sender) {
                ServerPlayer targetPlayer = sender.level().getServer()
                        .getPlayerList().getPlayer(packet.playerId);
                if (targetPlayer != null) {
                    targetPlayer.setDeltaMovement(packet.motion);
                }
            }
        });
    }
}
