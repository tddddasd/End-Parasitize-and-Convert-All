package org.tdddd.epca.impl.client.render;

import net.minecraftforge.eventbus.api.IEventBus;
import org.tdddd.epca.impl.overworld.registry.ModTags;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.araya.ArayaSlashShaders;
import org.tdddd.epca.impl.client.render.shader.EpcaShaders;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureShaders;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;

public final class EpcaRenderClient {

    private static boolean bindingsRegistered;

    private EpcaRenderClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(EpcaShaders::onRegisterShaders);
        modEventBus.addListener(SkyRuptureShaders::onRegisterShaders);

        modEventBus.addListener(ArayaSlashShaders::onRegisterShaders);

        modEventBus.addListener(SkyRuptureShaders::onTextureAtlasStitched);
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
        //     -> ItemRendererShaderLayerMixin / ItemLayerLateRenderQueue 每帧 resolve()
        //     -> ItemShaderRenderHelper.drawBinding() 用 corruption shader 再画一遍物品
        // 它只是个普通材料物品，不需要任何物品 shader，所以这里不再注册任何绑定。
        // （物品 tag 那条链路与本特效无关：ItemAndEntityTagsData 里 ender_blade_scrap 只进 epca:organ_part。）

        epca.LOGGER.info("[epca-render] 物品 shader 层注册数 = {} | Iris/Oculus 已安装 = {} | 光影兼容 = {}",
                ItemRenderRegistry.size(),
                IrisShaderCompat.isIrisLoaded(),
                IrisShaderCompat.isIrisLoaded() ? "已启用（自动延迟回放）" : "无需启用");
    }

    public static void resetBindingGuard() {
        bindingsRegistered = false;
    }
}
