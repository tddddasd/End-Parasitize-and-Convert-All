package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.List;
import java.util.function.Supplier;

public class ToggleFollowPacket {
    public ToggleFollowPacket() {}

    public ToggleFollowPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ToggleFollowPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!NestLeaderManager.isNestLeader(player.getUUID())) return;

            ServerLevel level = (ServerLevel) player.level();
            AABB box = player.getBoundingBox().inflate(16);
            List<Entity> parasites = level.getEntities(player, box, e -> e instanceof IParasite);

            boolean hasFollower = false;
            for (Entity e : parasites) {
                if (e instanceof IParasite p) {
                    if (player.getUUID().equals(p.getFollowTarget())) {
                        hasFollower = true;
                        break;
                    }
                }
            }

            boolean follow = !hasFollower;
            for (Entity e : parasites) {
                ((IParasite) e).setFollowTarget(follow ? player.getUUID() : null);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}