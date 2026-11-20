package org.tdddd.epca.impl.client.organ.model;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.client.command.HiveCommandClientData;
import org.tdddd.epca.impl.client.render.ClientRaidBarriers;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderClientCache;

/**
 * 客户端缓存清理：退出服务器 / 切换世界时丢掉所有“跟着上一个服务器走”的客户端状态，
 * 避免下次进入时用旧数据渲染一帧，更避免跨服务器误判身份。
 *
 * <ul>
 *     <li>{@link OrganModelClientData}：其他玩家的内器官显示数据；</li>
 *     <li>{@link HiveLeaderClientCache}：领巢者 UUID 集合（不清理会跨服务器残留，
 *     让 {@code BiomassHUD} / {@code ParaCurseEffect} 误判）；</li>
 *     <li>{@link HiveCommandClientData}：指挥系统快照 + {@code receivedSync} 身份标记；</li>
 *     <li>{@link ClientRaidBarriers}：当前维度的屏障快照。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OrganModelClientEvents {

    private OrganModelClientEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        OrganModelClientData.clear();
        // 领巢者名单是“按服务器”的数据：不清会跨服务器残留 UUID。
        HiveLeaderClientCache.clear();
        // 指挥快照同理，并且 receivedSync 是渲染器身份守卫的一半，必须一起复位。
        HiveCommandClientData.clear();
        // 屏障快照按维度存，登出直接丢掉，避免进下一个服务器时闪一帧上个世界的屏障。
        ClientRaidBarriers.clear();
    }
}
