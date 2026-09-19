package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.List;

/**
 * 客户端 → 服务端：巢穴领袖切换随从跟随。
 *
 * <p><b>26.1.2 改动</b>：{@code SimpleChannel} → {@link CustomPacketPayload}（无字段负载）。
 * {@code player.level()} 在 26.1.2 返回 {@code Level}，这里改为
 * {@code ctx.player().level()} 并直接取 {@link ServerLevel} 语义等价的玩家所在维度
 * （服务端负载的 {@code ctx.player()} 一定是 {@link ServerPlayer}）。
 */
public class ToggleFollowPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToggleFollowPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("toggle_follow"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleFollowPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ToggleFollowPacket::encode, ToggleFollowPacket::new);

    public ToggleFollowPacket() {}

    public ToggleFollowPacket(RegistryFriendlyByteBuf buf) {}

    public void encode(RegistryFriendlyByteBuf buf) {}

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleFollowPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!NestLeaderManager.isNestLeader(player.getUUID())) return;

            ServerLevel level = player.level();
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
    }
}
