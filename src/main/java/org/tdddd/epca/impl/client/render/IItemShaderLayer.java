package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * A shader render layer that can be attached to an item.
 *
 * <h2>This is the extension point of the unified item-render system</h2>
 * Implement this interface, register the layer and its pipeline, and you get a complete additional
 * item render pass, without touching any Mixin. The reference project (RottenRuinsSplendiding) wrote a
 * separate RenderType per effect (cosmic, corruption); this interface unifies them.
 *
 * <h2>Minimum steps for a new layer</h2>
 * <ol>
 *   <li>write the shader: {@code assets/epca/shaders/core/NAME.vsh} and {@code .fsh} (see below);</li>
 *   <li>register a pipeline + render type for it (see {@link ItemShaderPipelines});</li>
 *   <li>implement this interface and set the per-draw payload in {@link #prepare};</li>
 *   <li>attach it with
 *       {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry#attach}.</li>
 * </ol>
 *
 * <h2>Render contract</h2>
 * The caller guarantees that the base item model has already been submitted. The layer is then
 * submitted as an EXTRA pass through the same {@code SubmitNodeCollector}, so it inherits the item's
 * pose.
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * The 1.20.1 twin returned a {@code ShaderInstance} and set per-draw {@code Uniform}s in
 * {@link #prepare}. 26.1.2 has neither {@code ShaderInstance} nor a reachable per-draw uniform on the
 * submit path (see {@link SkyRuptureShaders} for the full argument), so the payload a layer produces
 * in {@link #prepare} is a small value object ({@link ItemLayerPayload}) that the renderer writes
 * into the vertex attributes. The interface shape is otherwise unchanged.
 */
public interface IItemShaderLayer {

    /** Layer name, must be unique (for example {@code "corruption"}). */
    String name();

    /** Default id: {@code epca:item_layer/<name>}. */
    default Identifier id() {
        return Identifier.fromNamespaceAndPath("epca", "item_layer/" + name());
    }

    /**
     * Whether the layer's shader pipeline is ready. Returns {@code false} until the pipeline has been
     * registered, so callers skip the draw instead of crashing.
     */
    boolean isReady();

    /**
     * Whether this layer supports the "replay after the level has been rendered" path used when a
     * shader pack is active.
     *
     * <p>Always effectively unused on 26.1.2, because no Iris/Oculus port exists; see
     * {@link org.tdddd.epca.impl.client.render.compat.IrisShaderCompat}.</p>
     */
    default boolean supportsDeferredReplay() {
        return true;
    }

    /**
     * Which mask texture an item should use.
     *
     * <p>Default convention: the item's own texture
     * ({@code assets/<namespace>/textures/item/<path>.png}), which is a 1:1 item silhouette, so the
     * layer lines up with the item by construction.</p>
     *
     * @param config the binding's config; {@link ItemLayerConfig#maskOverride()} can override this
     */
    Identifier maskTexture(ItemStack stack, ItemLayerConfig config);

    /**
     * Computes the per-draw payload and decides whether to draw at all.
     *
     * @param stack      the item being rendered
     * @param config     the binding's config ({@link ItemLayerConfig#strength()} is the generic
     *                   strength multiplier)
     * @param ctx        the display context
     * @param gameTime   {@code level.getGameTime()}
     * @return {@code null} to skip this draw
     */
    ItemLayerPayload prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx,
                             long gameTime);

    /**
     * Whether to additionally apply the geometric "twitch" jitter to the layer's own pass.
     *
     * <p>Off by default; the corruption layer enables it through {@link ItemLayerConfig#twitch()}.</p>
     */
    default boolean usesTwitchTransform(ItemStack stack, ItemLayerConfig config) {
        return config.twitch();
    }

    /**
     * Applies the geometric twitch to the layer's pose. Only called when
     * {@link #usesTwitchTransform} is true.
     *
     * <p>Kept from 1.20.1 with its exact signature, including the {@link PoseStack}, because the
     * jitter is a geometry transform and the renderer is the only place that owns a pose.</p>
     */
    default void applyTwitch(com.mojang.blaze3d.vertex.PoseStack poseStack, ItemStack stack,
                             ItemLayerConfig config, long gameTime) {
    }
}
