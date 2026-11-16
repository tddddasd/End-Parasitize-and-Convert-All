package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNoteTabsPacket;
import org.tdddd.epca.impl.overworld.data.EPCANoteDetailCosts;
import org.tdddd.epca.impl.overworld.data.EPCANoteTabData;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerNoteEvents {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {

        EPCANoteTabData.reloadFromServerResources(event.getServer());

        // 服务器启动（含数据包重载后的重启）时让笔记的消耗表重新读一遍数据包
        EPCANoteDetailCosts.invalidate();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            var tabs = EPCANoteTabData.getCurrentTabs();
            ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncNoteTabsPacket(tabs));

            // 虫咒笔记的解锁集合：登录先同步一份，打开笔记时客户端还会再拉一次
            NoteUnlockEvents.syncTo(serverPlayer);
        }
    }
}
