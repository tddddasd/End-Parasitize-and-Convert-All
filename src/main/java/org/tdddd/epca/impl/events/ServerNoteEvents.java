package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.data.EPCANoteTabData;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNoteTabsPacket;

@EventBusSubscriber(modid = epca.MODID)
public class ServerNoteEvents {
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        
        EPCANoteTabData.reloadFromServerResources(event.getServer());
        
    }

    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            var tabs = EPCANoteTabData.getCurrentTabs();
            ModNetwork.sendToPlayer(serverPlayer, new SyncNoteTabsPacket(tabs));
        }
    }
}