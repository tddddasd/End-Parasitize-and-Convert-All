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
import org.tdddd.epca.impl.overworld.registry.ModItems;

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

        ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION,
                ItemLayerConfig.builder()
                        .strength(1.0f)
                        .twitch(true)
                        .build());

        epca.LOGGER.info("[epca-render] 物品 shader 层注册数 = {} | Iris/Oculus 已安装 = {} | 光影兼容 = {}",
                ItemRenderRegistry.size(),
                IrisShaderCompat.isIrisLoaded(),
                IrisShaderCompat.isIrisLoaded() ? "已启用（自动延迟回放）" : "无需启用");
    }

    public static void resetBindingGuard() {
        bindingsRegistered = false;
    }
}
