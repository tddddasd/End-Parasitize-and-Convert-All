package org.tdddd.epca.impl.client.render;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.araya.ArayaSlashShaders;
import org.tdddd.epca.impl.client.render.shader.EpcaShaders;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureShaders;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.overworld.registry.ModItems;

/**
 *
 *
 * <p> {@link org.tdddd.epca.impl.epca}
 * {@link #init(IEventBus)}{@link #onClientSetup(FMLClientSetupEvent)}
 *  {@code ClientSetup} </p>
 */
public final class EpcaRenderClient {

    private static boolean bindingsRegistered;

    private EpcaRenderClient() {
    }

    /**  shader mod  */
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(EpcaShaders::onRegisterShaders);
        modEventBus.addListener(SkyRuptureShaders::onRegisterShaders);
        //  render type  shader
        modEventBus.addListener(ArayaSlashShaders::onRegisterShaders);
        //  12  sprite  UV
        modEventBus.addListener(SkyRuptureShaders::onTextureAtlasStitched);
    }

    /**
     *
     *
     * <p> {@code enqueueWork}
     * {@code ModItems.XXX.get()}</p>
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EpcaRenderClient::registerItemBindings);
    }

    /**
     *
     *
     * <p><b></b>
     * </p>
     *
     * <pre>{@code
     * //  1.
     * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION);
     *
     * //  2.  /  /
     * ItemRenderRegistry.attach(ModItems.CLUSTER, ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder()
     *                 .strength(0.85f)     //
     *                 .twitch(true)        //
     *                 .showInGui(false)    //
     *                 .build());
     *
     * //  3.
     * ItemRenderRegistry.attach(ModItems.DISEASED_HEART, ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder()
     *                 .mask("epca:item/diseased_heart_corrupt")
     *                 .build());
     *
     * //  4.
     * ItemRenderRegistry.attachAll(ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder().strength(0.6f).build(),
     *         ModItems.INFESTED_BONE.get(), ModItems.TWISTED_BONE.get());
     *
     * //  5.
     * ItemRenderRegistry.attach(
     *         stack -> stack.is(ModTags.Items.INFESTED_MATERIALS),
     *         ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.DEFAULT);
     *
     * //  6.  shader uniform
     * ItemRenderRegistry.attach(ModItems.KILL_STICK, ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder()
     *                 .uniforms((stack, shader) -> {
     *                     var tint = shader.getUniform("tint");
     *                     if (tint != null) tint.set(0.10f, 0.55f, 0.20f); //
     *                 })
     *                 .build());
     *
     * //  7. NBT
     * //   /data modify entity @s SelectedItem.tag.epca_corruption set value 0.7f
     * }</pre>
     */
    public static void registerItemBindings() {
        if (bindingsRegistered) {
            return;
        }
        bindingsRegistered = true;

        //
        // assets/epca/textures/item/ender_blade_scrap.png
        //  .mcmeta
        // twitch(true)
        ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION,
                ItemLayerConfig.builder()
                        .strength(1.0f)
                        .twitch(true)
                        .build());

        //  EpcaShaders   shader
        // (RegisterShadersEvent)
        epca.LOGGER.info("[epca-render] 物品 shader 层注册数 = {} | Iris/Oculus 已安装 = {} | 光影兼容 = {}",
                ItemRenderRegistry.size(),
                IrisShaderCompat.isIrisLoaded(),
                IrisShaderCompat.isIrisLoaded() ? "已启用（自动延迟回放）" : "无需启用");
    }

    /**  /  */
    public static void resetBindingGuard() {
        bindingsRegistered = false;
    }
}

