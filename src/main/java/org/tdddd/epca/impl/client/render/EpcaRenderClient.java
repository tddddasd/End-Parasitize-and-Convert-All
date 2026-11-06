package org.tdddd.epca.impl.client.render;

import org.tdddd.epca.impl.overworld.registry.ModTags;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.layer.CorruptionLayer;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;

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

        ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION,
                ItemLayerConfig.builder()
                        .strength(1.0f)
                        .twitch(true)
                        .build());

        epca.LOGGER.info("[epca-render] item shader layer bindings = {} | corruption layer ready = {} | Iris/Oculus installed = {}",
                ItemRenderRegistry.size(),
                CorruptionLayer.INSTANCE.isReady(),
                IrisShaderCompat.isIrisLoaded());
    }

    public static void resetBindingGuard() {
        bindingsRegistered = false;
    }
}
