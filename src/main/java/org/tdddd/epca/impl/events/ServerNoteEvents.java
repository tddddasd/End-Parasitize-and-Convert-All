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
            // 登录时把该玩家的「已发现 / 详情已解锁」两份集合一并推下去（与 1.20.1 一致）：
            // 否则客户端在打开笔记之前手里是空的，会先把所有条目显示成黑色乱码再闪一下
            NoteUnlockEvents.syncTo(serverPlayer);
        }
    }
}
