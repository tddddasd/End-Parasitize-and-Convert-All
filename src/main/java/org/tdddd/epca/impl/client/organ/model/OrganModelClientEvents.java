package org.tdddd.epca.impl.client.organ.model;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.tdddd.epca.impl.client.command.HiveCommandClientData;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderClientCache;

/**
 * 客户端缓存清理：退出服务器 / 切换世界时丢掉其他玩家的内器官显示数据，
 * 避免下次进入时用旧数据渲染一帧。
 *
 * <p>0.148n 追加：同一个 {@code LoggingOut} 里一并清掉两份会“跨服务器残留”的客户端缓存：</p>
 * <ul>
 *     <li>{@link HiveLeaderClientCache}：领巢者 UUID 集合（以前从不清理，换服务器后
 *     {@code BiomassHUD}、{@code ParaCurseEffect} 会拿上一个存档的领巢者误判）；</li>
 *     <li>{@link HiveCommandClientData}：指挥高亮 / 路线 / 编辑态（含 {@code receivedSync} 标记，
 *     清掉它渲染守卫才会在下次登录前保持关闭）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class OrganModelClientEvents {

    private OrganModelClientEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        OrganModelClientData.clear();
        // 以下是 0.148n 的跨服务器残留修复（与 1.20.1 侧同款）。
        HiveLeaderClientCache.clear();
        HiveCommandClientData.clear();
    }
}
