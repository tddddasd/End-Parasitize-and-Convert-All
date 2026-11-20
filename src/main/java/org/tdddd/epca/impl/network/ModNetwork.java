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

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

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

        registrar.playToServer(RequestOpenHiveLeaderOrgansPacket.TYPE,
                RequestOpenHiveLeaderOrgansPacket.STREAM_CODEC,
                RequestOpenHiveLeaderOrgansPacket::handle);
        registrar.playToServer(HiveLeaderOrganActionPacket.TYPE,
                HiveLeaderOrganActionPacket.STREAM_CODEC,
                HiveLeaderOrganActionPacket::handle);
        registrar.playToServer(HiveLeaderOrganTeleportPacket.TYPE,
                HiveLeaderOrganTeleportPacket.STREAM_CODEC,
                HiveLeaderOrganTeleportPacket::handle);
        registrar.playToServer(HiveLeaderDecomposeParasitePacket.TYPE,
                HiveLeaderDecomposeParasitePacket.STREAM_CODEC,
                HiveLeaderDecomposeParasitePacket::handle);

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
        registrar.playToClient(
                org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomesPacket.TYPE,
                org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomesPacket.STREAM_CODEC,
                org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomesPacket::handle);
        registrar.playToClient(
                org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomeBatchPacket.TYPE,
                org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomeBatchPacket.STREAM_CODEC,
                org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomeBatchPacket::handle);
        registrar.playToClient(InfestedSourcePacket.AddInfestedSourcePacket.TYPE,
                InfestedSourcePacket.AddInfestedSourcePacket.STREAM_CODEC,
                InfestedSourcePacket.AddInfestedSourcePacket::handle);

        registrar.playToClient(
                InfestedSourcePacket.BatchInfestedSourcePacket.TYPE,
                InfestedSourcePacket.BatchInfestedSourcePacket.STREAM_CODEC,
                InfestedSourcePacket.BatchInfestedSourcePacket::handle);
        registrar.playToClient(InfestedSourcePacket.RemoveInfestedSourcePacket.TYPE,
                InfestedSourcePacket.RemoveInfestedSourcePacket.STREAM_CODEC,
                InfestedSourcePacket.RemoveInfestedSourcePacket::handle);
        registrar.playToClient(SyncAllInfestedSourcesPacket.TYPE, SyncAllInfestedSourcesPacket.STREAM_CODEC,
                SyncAllInfestedSourcesPacket::handle);
        registrar.playToClient(BiomassSyncPacket.TYPE, BiomassSyncPacket.STREAM_CODEC,
                BiomassSyncPacket::handle);
        registrar.playToClient(SyncHiveLeadersPacket.TYPE, SyncHiveLeadersPacket.STREAM_CODEC,
                SyncHiveLeadersPacket::handle);

        registrar.playToClient(SyncSoulProtectionPacket.TYPE, SyncSoulProtectionPacket.STREAM_CODEC,
                SyncSoulProtectionPacket::handle);

        registrar.playToClient(SyncRitualAuraPacket.TYPE, SyncRitualAuraPacket.STREAM_CODEC,
                SyncRitualAuraPacket::handle);

        registrar.playToClient(SyncArayaAuraPacket.TYPE, SyncArayaAuraPacket.STREAM_CODEC,
                SyncArayaAuraPacket::handle);

        registrar.playToClient(SyncArayaFirePacket.TYPE, SyncArayaFirePacket.STREAM_CODEC,
                SyncArayaFirePacket::handle);

        registrar.playToClient(SpawnArayaSlashPacket.TYPE, SpawnArayaSlashPacket.STREAM_CODEC,
                SpawnArayaSlashPacket::handle);

        registrar.playToClient(SyncHiveLeaderOrgansPacket.TYPE, SyncHiveLeaderOrgansPacket.STREAM_CODEC,
                SyncHiveLeaderOrgansPacket::handle);

        // 领巢者种阶特化 + 内器官模型显示（2026-11 新增）
        registrar.playToClient(SyncHiveLeaderSpeciesPacket.TYPE, SyncHiveLeaderSpeciesPacket.STREAM_CODEC,
                SyncHiveLeaderSpeciesPacket::handle);
        registrar.playToClient(SyncOrganModelDisplayPacket.TYPE, SyncOrganModelDisplayPacket.STREAM_CODEC,
                SyncOrganModelDisplayPacket::handle);
        registrar.playToServer(ToggleOrganModelPacket.TYPE, ToggleOrganModelPacket.STREAM_CODEC,
                ToggleOrganModelPacket::handle);
        registrar.playToServer(HiveLeaderSpeciesAbilityPacket.TYPE,
                HiveLeaderSpeciesAbilityPacket.STREAM_CODEC,
                HiveLeaderSpeciesAbilityPacket::handle);

        // 寄生体指挥系统：菜单/按键 -> 服务端（C2S），服务端状态广播 -> 领巢者（S2C）
        registrar.playToServer(
                org.tdddd.epca.impl.network.packet.c2s.HiveCommandActionPacket.TYPE,
                org.tdddd.epca.impl.network.packet.c2s.HiveCommandActionPacket.STREAM_CODEC,
                org.tdddd.epca.impl.network.packet.c2s.HiveCommandActionPacket::handle);
        registrar.playToClient(
                org.tdddd.epca.impl.network.packet.s2c.SyncHiveCommandsPacket.TYPE,
                org.tdddd.epca.impl.network.packet.s2c.SyncHiveCommandsPacket.STREAM_CODEC,
                org.tdddd.epca.impl.network.packet.s2c.SyncHiveCommandsPacket::handle);

        registrar.playToClient(DismemberS2CPacket.TYPE, DismemberS2CPacket.STREAM_CODEC,
                DismemberS2CPacket::handle);

        registrar.playToClient(RaidBarrierS2CPacket.TYPE, RaidBarrierS2CPacket.STREAM_CODEC,
                RaidBarrierS2CPacket::handle);

        // 虫咒笔记：解锁状态同步（S2C）+ 打开界面时主动要一份 / 点击解锁详情（C2S）
        registrar.playToClient(SyncNoteUnlocksPacket.TYPE, SyncNoteUnlocksPacket.STREAM_CODEC,
                SyncNoteUnlocksPacket::handle);
        registrar.playToServer(NoteSyncRequestPacket.TYPE, NoteSyncRequestPacket.STREAM_CODEC,
                NoteSyncRequestPacket::handle);
        registrar.playToServer(NoteDetailUnlockRequestPacket.TYPE, NoteDetailUnlockRequestPacket.STREAM_CODEC,
                NoteDetailUnlockRequestPacket::handle);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(epca.MODID, path);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    /**
     * 客户端 -&gt; 服务端（26 的等价写法：1.20.1 的 {@code SimpleChannel#sendToServer}）。
     * 26 把这条通道挪到了 {@code net.neoforged.neoforge.client.network.ClientPacketDistributor}
     * （client 包）；这里直接转发，只在客户端代码里调用，所以专用服务器不会加载那个类
     * （JVM 对方法体里的引用是惰性解析的）。
     */
    public static void sendToServer(CustomPacketPayload payload) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
    }

    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void sendToAllTracking(CustomPacketPayload payload, LivingEntity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    public static void sendToAllTracking(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    public static void sendToDimension(net.minecraft.server.level.ServerLevel level,
                                       CustomPacketPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level, payload);
    }
}
