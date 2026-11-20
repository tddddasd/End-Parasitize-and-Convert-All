package org.tdddd.epca.impl.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.tdddd.epca.impl.epca;

import javax.annotation.Nullable;

/**
 * 客户端屏障缓存的清理挂钩（0.148n）。
 *
 * <ul>
 *     <li><b>登出</b>：整表清空，避免跨服务器 / 跨存档残留；</li>
 *     <li><b>换维度</b>：26 没有客户端侧“维度改变”事件，用 {@link ClientTickEvent.Post}
 *     比较 {@code minecraft.level.dimension()} 是否变化：变化时调用
 *     {@link ClientRaidBarriers#clearOtherDimensions(Identifier)} 丢掉旧维度的条目
 *     （详见该类注释：不直接整表清空是为了避开“补发包先到”的时序竞争），
 *     回到主菜单（{@code level == null}）时直接整表清空。</li>
 * </ul>
 */
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class ClientRaidBarrierEvents {

    /** 上一 tick 观测到的维度；null 表示不在世界里。 */
    @Nullable
    private static Identifier lastDimension;

    private ClientRaidBarrierEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        lastDimension = null;
        ClientRaidBarriers.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier dimension = minecraft.level == null ? null : minecraft.level.dimension().identifier();
        if (dimension == null ? lastDimension == null : dimension.equals(lastDimension)) {
            return;
        }
        lastDimension = dimension;
        if (dimension == null) {
            ClientRaidBarriers.clear();
        } else {
            // 换维度：旧维度条目立即作废；目标维度的条目留着（可能是刚补发到的）。
            ClientRaidBarriers.clearOtherDimensions(dimension);
        }
    }
}
