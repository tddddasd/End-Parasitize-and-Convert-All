package org.tdddd.epca.impl.client.render;

import org.tdddd.epca.impl.overworld.registry.ModTags;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.layer.CorruptionLayer;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.epca;

public final class EpcaRenderClient {

    private static boolean bindingsRegistered;

    private EpcaRenderClient() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EpcaRenderClient::registerItemBindings);
    }

    public static void registerItemBindings() {
        if (bindingsRegistered) {
            return;
        }
        bindingsRegistered = true;

        // 「影刃残片」（epca:ender_blade_scrap）曾经在这里被挂上 CORRUPTION 物品 shader 层：
        //   EpcaRenderClient.registerItemBindings()
        //     -> ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION, ...)
        //     -> ItemCorruptionRenderer 每帧 resolve()
        //     -> epca:core/corruption shader 用 ItemShaderPipelines 的 pipeline 再画一遍物品
        // 它只是个普通材料物品，不需要任何物品 shader，所以这里不再注册任何绑定
        //（ItemRenderRegistry.size() == 0，物品恢复普通渲染，twitch 抖动也随之消失）。
        // （物品 tag 那条链路与本特效无关：ItemAndEntityTagsData 里 ender_blade_scrap 只进 epca:organ_part。）

        epca.LOGGER.info("[epca-render] item shader layer bindings = {} | corruption layer ready = {} | Iris/Oculus installed = {}",
                ItemRenderRegistry.size(),
                CorruptionLayer.INSTANCE.isReady(),
                IrisShaderCompat.isIrisLoaded());
    }

    public static void resetBindingGuard() {
        bindingsRegistered = false;
    }
}
