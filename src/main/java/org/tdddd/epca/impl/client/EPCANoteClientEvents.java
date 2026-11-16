package org.tdddd.epca.impl.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.EPCANoteClientUnlocks;
import org.tdddd.epca.impl.overworld.data.EPCANoteLang;

/**
 * 虫咒笔记的客户端总线挂钩（只在物理客户端注册）。
 *
 * <p>两件事：</p>
 * <ol>
 *   <li><b>注册拆分语言表的 reload listener</b>：笔记专用语言放在
 *       {@code assets/epca/lang/para_curse_note/}（MC 不会自动加载子目录里的语言文件），
 *       必须自己挂到客户端资源重载链上。1.20.1 用的是
 *       {@code RegisterClientReloadListenersEvent}；26 改名为
 *       {@link AddClientReloadListenersEvent}，注册方式是
 *       {@code addListener(Identifier, PreparableReloadListener)}。</li>
 *   <li><b>断线清空解锁缓存</b>：避免换服务器 / 换存档后拿到上一个存档的解锁集合。</li>
 * </ol>
 */
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class EPCANoteClientEvents {

    private EPCANoteClientEvents() {
    }

    @SubscribeEvent
    public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(epca.asResource("para_curse_note_lang"), EPCANoteLang.LISTENER);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EPCANoteClientUnlocks.clear();
    }
}
