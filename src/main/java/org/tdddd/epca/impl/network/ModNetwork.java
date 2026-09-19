package org.tdddd.epca.impl.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.packet.c2s.*;
import org.tdddd.epca.impl.network.packet.s2c.*;

/**
 * epca 的网络注册与发送入口。
 *
 * <p><b>26.1.2 改动</b>：1.20.1 用 {@code NetworkRegistry.newSimpleChannel} +
 * {@code SimpleChannel}（{@code registerMessage} / {@code messageBuilder}）。26.1.2 把整套
 * SimpleChannel API 删掉了，改为 payload 系统：在模组总线的
 * {@link RegisterPayloadHandlersEvent} 里用 {@link PayloadRegistrar} 注册
 * {@link CustomPacketPayload} 与它的 {@code StreamCodec}；发送改用
 * {@link PacketDistributor} 的静态方法。
 * <ul>
 *   <li>协议版本号仍是 {@code "1"}，负载集合与 1.20.1 完全一致（id 序号不再需要）；</li>
 *   <li>原 {@code public static final SimpleChannel INSTANCE} 字段随 {@code SimpleChannel}
 *       一起删除（平台已无该类）；</li>
 *   <li>1.20.1 是单条 "main" 通道，收发同一个；26.1.2 必须按方向分别注册：
 *       {@code playToClient}（S→C）/ {@code playToServer}（C→S）。方向按原调用点归类，
 *       见下方注册表。</li>
 *   <li>客户端 → 服务端改为 {@code ClientPacketDistributor.sendToServer(payload)}
 *       （该调用点在自己的客户端文件里，故不在此处包一层，避免把仅客户端的类带进服务端加载路径）。</li>
 * </ul>
 */
public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    /**
     * 在模组总线上注册 {@link RegisterPayloadHandlersEvent} 监听器。
     * 由 {@code epca} 的构造器调用（26.1.2 已无 {@code FMLCommonSetupEvent} 的必要）。
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // ── 客户端 → 服务端 ──
        registrar.playToServer(FlightStatePacket.TYPE, FlightStatePacket.STREAM_CODEC,
                FlightStatePacket::handle);
        registrar.playToServer(PlayerMotionPacket.TYPE, PlayerMotionPacket.STREAM_CODEC,
                PlayerMotionPacket::handle);
        registrar.playToServer(KeyPacket.TYPE, KeyPacket.STREAM_CODEC,
                KeyPacket::handle);
        registrar.playToServer(KeyPressPacket.TYPE, KeyPressPacket.STREAM_CODEC,
                KeyPressPacket::handle);
        registrar.playToServer(VKeyStatePacket.TYPE, VKeyStatePacket.STREAM_CODEC,
                VKeyStatePacket::handle);
        registrar.playToServer(ToggleFollowPacket.TYPE, ToggleFollowPacket.STREAM_CODEC,
                ToggleFollowPacket::handle);
        registrar.playToServer(InfestedSourcePacket.RequestAllInfestedSourcesPacket.TYPE,
                InfestedSourcePacket.RequestAllInfestedSourcesPacket.STREAM_CODEC,
                InfestedSourcePacket.RequestAllInfestedSourcesPacket::handle);

        // ── 服务端 → 客户端 ──
        registrar.playToClient(AcidWaterColorPacket.TYPE, AcidWaterColorPacket.STREAM_CODEC,
                AcidWaterColorPacket::handle);
        registrar.playToClient(EnderErosionSyncPacket.TYPE, EnderErosionSyncPacket.STREAM_CODEC,
                EnderErosionSyncPacket::handle);
        registrar.playToClient(ClearErosionDisplayPacket.TYPE, ClearErosionDisplayPacket.STREAM_CODEC,
                ClearErosionDisplayPacket::handle);
        registrar.playToClient(SyncEvolutionStagePacket.TYPE, SyncEvolutionStagePacket.STREAM_CODEC,
                SyncEvolutionStagePacket::handle);
        registrar.playToClient(SyncNoteTabsPacket.TYPE, SyncNoteTabsPacket.STREAM_CODEC,
                SyncNoteTabsPacket::handle);
        registrar.playToClient(ColorEffectPacket.TYPE, ColorEffectPacket.STREAM_CODEC,
                ColorEffectPacket::handle);
        registrar.playToClient(InfestedSourcePacket.AddInfestedSourcePacket.TYPE,
                InfestedSourcePacket.AddInfestedSourcePacket.STREAM_CODEC,
                InfestedSourcePacket.AddInfestedSourcePacket::handle);
        registrar.playToClient(InfestedSourcePacket.RemoveInfestedSourcePacket.TYPE,
                InfestedSourcePacket.RemoveInfestedSourcePacket.STREAM_CODEC,
                InfestedSourcePacket.RemoveInfestedSourcePacket::handle);
        registrar.playToClient(SyncAllInfestedSourcesPacket.TYPE, SyncAllInfestedSourcesPacket.STREAM_CODEC,
                SyncAllInfestedSourcesPacket::handle);
        registrar.playToClient(BiomassSyncPacket.TYPE, BiomassSyncPacket.STREAM_CODEC,
                BiomassSyncPacket::handle);
        registrar.playToClient(SyncNestLeadersPacket.TYPE, SyncNestLeadersPacket.STREAM_CODEC,
                SyncNestLeadersPacket::handle);
    }

    /** 造一个 {@code epca:} 命名空间的 {@link Identifier}（供负载 TYPE 使用）。 */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(epca.MODID, path);
    }

    // ═══════════════════ 发送辅助（对应 1.20.1 的 INSTANCE.send 各目标） ═══════════════════

    /** 发给单个玩家，等价于 {@code PacketDistributor.PLAYER.with(() -> player)}。 */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    /** 发给所有玩家，等价于 {@code PacketDistributor.ALL.noArg()}。 */
    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    /** 发给所有追踪该生物的玩家，等价于 {@code PacketDistributor.TRACKING_ENTITY.with(() -> entity)}。 */
    public static void sendToAllTracking(CustomPacketPayload payload, LivingEntity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    /** 发给所有追踪该生物的玩家（含生物自己），等价于 {@code TRACKING_ENTITY_AND_SELF}。 */
    public static void sendToAllTracking(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }
}
