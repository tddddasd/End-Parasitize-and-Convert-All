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
        // Vanilla never syncs a mob's active effects to other clients, so the visible
        // epca:soul_protection aura needs its own batch sync; see SoulProtectionSyncHandler.
        registrar.playToClient(SyncSoulProtectionPacket.TYPE, SyncSoulProtectionPacket.STREAM_CODEC,
                SyncSoulProtectionPacket::handle);
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
}
