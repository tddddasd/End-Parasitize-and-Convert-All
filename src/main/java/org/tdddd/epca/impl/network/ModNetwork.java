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
        //  GUISPEC  2
        //  /  /
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
        // Vanilla never syncs a mob's active effects to other clients, so the visible
        // epca:soul_protection aura needs its own batch sync; see SoulProtectionSyncHandler.
        registrar.playToClient(SyncSoulProtectionPacket.TYPE, SyncSoulProtectionPacket.STREAM_CODEC,
                SyncSoulProtectionPacket::handle);
        // Visual state of the running sacrifice rituals, so the client can draw the purple aura the
        // server-side BlockConversionManager owns and fade it out when the lightning falls.
        registrar.playToClient(SyncRitualAuraPacket.TYPE, SyncRitualAuraPacket.STREAM_CODEC,
                SyncRitualAuraPacket::handle);
        // The Alayavijnana aura: which players carry a staff whose  counter reached the threshold
        // (that is what drives the looping BGM and the fire field around them).
        registrar.playToClient(SyncArayaAuraPacket.TYPE, SyncArayaAuraPacket.STREAM_CODEC,
                SyncArayaAuraPacket::handle);
        // The render-only vanilla fire blocks around those players. The server rolls where, how tall and
        // for how long; the client only draws.
        registrar.playToClient(SyncArayaFirePacket.TYPE, SyncArayaFirePacket.STREAM_CODEC,
                SyncArayaFirePacket::handle);
        // One Alayavijnana slash: the white 50-degree cut with its refracting border, spawned by a kill of
        // another player with the renamed staff.
        registrar.playToClient(SpawnArayaSlashPacket.TYPE, SpawnArayaSlashPacket.STREAM_CODEC,
                SpawnArayaSlashPacket::handle);
        //  GUI 52  +  +  +
        registrar.playToClient(SyncHiveLeaderOrgansPacket.TYPE, SyncHiveLeaderOrgansPacket.STREAM_CODEC,
                SyncHiveLeaderOrgansPacket::handle);
        // EPCA-only render-only gibs.
        registrar.playToClient(DismemberS2CPacket.TYPE, DismemberS2CPacket.STREAM_CODEC,
                DismemberS2CPacket::handle);
        // Infested raid victory barrier state (full replace).
        registrar.playToClient(RaidBarrierS2CPacket.TYPE, RaidBarrierS2CPacket.STREAM_CODEC,
                RaidBarrierS2CPacket::handle);
    }

    
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(epca.MODID, path);
    }

    

    
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
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

    /** Sends a payload to every player in {@code level}. */
    public static void sendToDimension(net.minecraft.server.level.ServerLevel level,
                                       CustomPacketPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level, payload);
    }
}
