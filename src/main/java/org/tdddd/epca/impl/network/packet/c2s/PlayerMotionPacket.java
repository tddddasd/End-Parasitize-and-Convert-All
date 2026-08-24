package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerMotionPacket {
    private final UUID playerId;
    private final Vec3 motion;

    public PlayerMotionPacket(UUID playerId, Vec3 motion) {
        this.playerId = playerId;
        this.motion = motion;
    }

    public PlayerMotionPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.motion = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeDouble(motion.x);
        buf.writeDouble(motion.y);
        buf.writeDouble(motion.z);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                ServerPlayer targetPlayer = sender.server.getPlayerList().getPlayer(playerId);
                if (targetPlayer != null) {
                    targetPlayer.setDeltaMovement(motion);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}