package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.overworld.data.EPCANoteTabData;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNoteTabsPacket;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerNoteEvents {
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        
        EPCANoteTabData.reloadFromServerResources(event.getServer());
        
    }

    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            var tabs = EPCANoteTabData.getCurrentTabs();
            ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncNoteTabsPacket(tabs));
        }
    }
}