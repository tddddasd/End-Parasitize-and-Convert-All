package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;

import net.minecraft.world.entity.LivingEntity;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Supplier;

public class ToggleFollowPacket {
    public ToggleFollowPacket() {}

    public ToggleFollowPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ToggleFollowPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;

            ServerLevel level = (ServerLevel) player.level();
            AABB box = player.getBoundingBox().inflate(16);
            List<Entity> parasites = level.getEntities(player, box, e -> ParasiteEvents.isParasiteEntity(e));

            boolean hasFollower = false;
            for (Entity e : parasites) {
                if (player.getUUID().equals(ParasiteEvents.getFollowTarget((LivingEntity) e))) {
                    hasFollower = true;
                    break;
                }
            }

            boolean follow = !hasFollower;
            for (Entity e : parasites) {

                ParasiteEvents.setFollowTarget((LivingEntity) e, follow ? player.getUUID() : null);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
