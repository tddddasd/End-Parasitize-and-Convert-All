package org.tdddd.epca.impl.client.render;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.layer.CorruptionLayer;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;

/**
 * Client render subsystem entry point.
 *
 * <p>{@link #onClientSetup(FMLClientSetupEvent)} is forwarded from {@code ClientSetup}, exactly as the
 * 1.20.1 tree forwarded it. The 1.20.1 constructor also called {@code init(IEventBus)} to add the
 * shader-loading listeners; on 26.1.2 those listeners do not exist (the pipelines are registered from
 * {@code ClientHandler#onRegisterRenderPipelines}), so {@code init} is gone rather than empty.</p>
 */
public final class EpcaRenderClient {

    private static boolean bindingsRegistered;

    private EpcaRenderClient() {
    }

    /**
     * Client initialisation: registers the default item bindings.
     *
     * <p>Uses {@code enqueueWork} so it runs once the registries are available, which is what lets it
     * reference {@code ModItems.XXX} directly.</p>
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EpcaRenderClient::registerItemBindings);
    }

    /**
     * Attaches the shipped item render bindings.
     *
     * <p>The 1.20.1 tree documented a catalogue of usage examples here (custom mask, batch attach,
     * predicate attach, per-item uniform shaping, NBT-driven constant corruption). Two of those are no
     * longer expressible and are called out below; the rest still work verbatim.</p>
     *
     * <pre>{@code
     * // 1. simplest: corruption with the item's own texture as the mask
     * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION);
     *
     * // 2. with parameters
     * ItemRenderRegistry.attach(ModItems.CLUSTER, ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder().strength(0.85f).twitch(true).showInGui(false).build());
     *
     * // 3. a different mask texture
     * ItemRenderRegistry.attach(ModItems.DISEASED_HEART, ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder().mask("epca:item/diseased_heart_corrupt").build());
     *
     * // 4. a batch
     * ItemRenderRegistry.attachAll(ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder().strength(0.6f).build(),
     *         ModItems.INFESTED_BONE.get(), ModItems.TWISTED_BONE.get());
     *
     * // 5. by predicate
     * ItemRenderRegistry.attach(stack -> stack.is(ModTags.Items.INFESTED_MATERIALS),
     *         ItemShaderLayers.CORRUPTION, ItemLayerConfig.DEFAULT);
     *
     * // 6. NBT-driven constant corruption (no periodic burst):
     * //   /data modify entity @s SelectedItem.components."minecraft:custom_data".epca_corruption set value 0.7f
     *
     * // 7. NOT PORTED: `ItemLayerConfig.Builder#uniforms(...)`, which used a ShaderInstance. The tint
     * //    and split strength are fields of ItemLayerPayload now; see CorruptionLayer.
     * }</pre>
     */
    public static void registerItemBindings() {
        if (bindingsRegistered) {
            return;
        }
        bindingsRegistered = true;

        // -- the shipped binding, identical to the 1.20.1 tree --
        // The ender blade scrap gets the corruption layer. The mask is the item's own texture
        // (assets/epca/textures/item/ender_blade_scrap.png, which carries an .mcmeta animation), so
        // the layer lines up with the item's silhouette by construction and animates with it.
        // twitch(true) adds the geometric jitter inside the burst window, driven by the same strength
        // curve as the shader.
        ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION,
                ItemLayerConfig.builder()
                        .strength(1.0f)
                        .twitch(true)
                        .build());

        // Deliberately does not touch CorruptionLayer's shader fields here: those only exist once the
        // pipeline has been registered (RegisterRenderPipelinesEvent), and reading them now would force
        // class initialisation for no benefit.
        epca.LOGGER.info("[epca-render] item shader layer bindings = {} | corruption layer ready = {} | Iris/Oculus installed = {}",
                ItemRenderRegistry.size(),
                CorruptionLayer.INSTANCE.isReady(),
                IrisShaderCompat.isIrisLoaded());
    }

    /** Forces the registration guard to reset (hot reload / debugging). */
    public static void resetBindingGuard() {
        bindingsRegistered = false;
    }
}
