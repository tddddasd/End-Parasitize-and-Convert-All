package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderType;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.client.render.layer.CorruptionLayer;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The runtime half of the item shader layers: it recovers the {@link ItemStack} for a render state,
 * decides whether a layer applies, jitters the pose, and submits the extra pass.
 *
 * <h2>Why a weak map is needed at all (the central 26.1.2 problem)</h2>
 * 26.1.2 splits rendering into extract and submit. The stack is known while the model is resolved, but
 * {@code ItemStackRenderState} - the thing that is actually submitted - <b>does not carry it</b>
 * (verified with {@code javap -p}: its fields are {@code displayContext}, {@code activeLayerCount},
 * {@code animated}, {@code oversizedInGui}, {@code cachedModelBoundingBox} and {@code layers}), and
 * neither does its inner {@code LayerRenderState}. The 1.20.1 hook
 * ({@code ItemRenderer#render}, which had the stack as a parameter) no longer exists.
 *
 * <p>So {@code ItemLayerCaptureMixin} records {@code renderState -> (stack, displayContext)} at the
 * TAIL of {@code ItemModelResolver#appendItemLayers(...)} - the single funnel all three
 * {@code updateFor*} variants call, and the only place holding both objects - and this class reads it
 * back at submit time. A {@link WeakHashMap} keyed by the render state means the entry disappears with
 * the state, so a pooled/cleared state cannot leak a stale stack.
 *
 * <h2>Where the pose is valid</h2>
 * {@code ItemStackRenderState#submit} only loops over layers and is pose-balanced at its TAIL, and
 * {@code LayerRenderState#submit} does {@code pushPose() -> applyTransform -> submitItem -> popPose()}.
 * The item's <b>final model space is therefore only valid just before that {@code popPose()}</b>, which
 * is where {@code ItemLayerEmitMixin} injects.
 *
 * <h2>Mask texture and animation frames</h2>
 * The mask is bound as its own <b>texture resource</b> ({@code namespace:textures/...png}) and the render
 * type is cached per mask texture, because the layer no longer uses the block atlas at all. See
 * {@link ItemMaskTexture} for why: the atlas route threw
 * {@code IllegalArgumentException: Invalid atlas id: minecraft:textures/atlas/blocks.png} from
 * {@code AtlasManager#getAtlasOrThrow}, which wants an atlas <em>id</em>
 * ({@code minecraft:blocks}) rather than the atlas <em>texture path</em> that
 * {@code TextureAtlas.LOCATION_BLOCKS} holds.
 *
 * <p>A directly bound texture is the raw strip, and the {@code .mcmeta} animation is not applied to it, so
 * the animation frame is selected here: the frame count comes from the PNG's IHDR
 * ({@code height / width}) and the rate from the sibling {@code .mcmeta}'s {@code frametime}, and the
 * quad's V is banded into the current frame before emission. That keeps {@code corruption.fsh} untouched,
 * and it is required for correctness rather than polish: the shipped mask is a 24-frame strip, so sampling
 * the full 0..1 V range would smear all 24 frames across the item.</p>
 *
 * <h2>Overlay geometry</h2>
 * The overlay is built from <b>the item's own baked quads</b>, not from a constant plane: same positions,
 * same UVs (normalised into the bound mask texture's space and banded into the current animation frame),
 * under the same pose the item was submitted with. An earlier revision emitted a fixed 16x16-at-z=8 plane
 * and rendered too large and displaced, because that space is not the item's; see
 * {@link ItemShaderBakery} for the {@code javap} evidence and the full argument.
 *
 * <p>Every quad of every layer is overlaid, so a multi-quad or multi-layer model gets the decay on all of its
 * geometry. While the effect is active the item's <b>own</b> model is suppressed entirely, so only the overlay
 * is visible; see {@link #suppressAndOverlay} and {@code ItemLayerEmitMixins} for the redirect that does it.
 * The overlay itself is emitted once per render state per submit - two bindings for the same item would stack
 * the same overlay and double the tint.</p>
 *
 * <h2>Emitting the extra pass</h2>
 * {@code OrderedSubmitNodeCollector#submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)}
 * hands back a pose plus a raw {@code VertexConsumer}. The geometry is written vertex by vertex with the
 * four writers {@link ItemShaderPipelines#ITEM_LAYER_VERTEX_FORMAT} declares - {@code addVertex(pose, x,
 * y, z)} (which applies the pose), {@code setColor}, {@code setUv} and {@code setUv1}, plus
 * {@code setLineWidth} for the clock. No custom {@code VertexConsumer} is involved.
 *
 * <p>An earlier revision used {@code VertexConsumer#putBakedQuad(pose, BakedQuad, QuadInstance)} instead.
 * That had to be abandoned: {@code putBakedQuad} writes POSITION, COLOR, UV0, UV1, UV2 and NORMAL, and a
 * format containing those six is 35 bytes, which {@code VertexFormat.Builder#build()} rejects because it
 * requires a multiple of 4. Writing manually also removed the {@code BakedQuad} /
 * {@code MaterialInfo} / {@code QuadInstance} machinery entirely.</p>
 */
public final class ItemCorruptionRenderer {

    /** Per-render-state stack + context, recorded during model resolution. */
    private static final Map<Object, Captured> CAPTURED =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Render states already overlayed in the current submit. An item model can have more than one
     * layer, and the emit hook fires once per layer; without this the tint would be applied once per
     * layer. Cleared at the HEAD of {@code ItemStackRenderState#submit}.
     */
    private static final Set<Object> EMITTED =
            Collections.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));

    private ItemCorruptionRenderer() {
    }

    /** What was captured for one render state. */
    public record Captured(ItemStack stack, ItemDisplayContext context) {
    }

    /** Called from the capture mixin. */
    public static void capture(Object renderState, ItemStack stack, ItemDisplayContext context) {
        if (renderState == null || stack == null || stack.isEmpty()) {
            return;
        }
        CAPTURED.put(renderState, new Captured(stack, context));
    }

    /** Called at the HEAD of {@code ItemStackRenderState#submit}: start a fresh emit guard. */
    public static void beginSubmit(Object renderState) {
        EMITTED.remove(renderState);
    }

    /**
     * Called just before {@code LayerRenderState#submit} pops its pose: if a layer of this item wants
     * to draw, submit the extra pass through the item's own pose.
     *
     * @param renderState  the owning {@code ItemStackRenderState}
     * @param poseStack    the item's final pose (model space)
     * @param collector    the submit collector of this frame
     * @param packedLight  packed lightmap, passed to the quads
     * @param packedOverlay packed overlay, passed to the quads
     */
    /**
     * Decides whether the corruption overlay replaces the item this frame, and if so emits it.
     *
     * <h2>Contract</h2>
     * Called from the {@code @Redirect} on {@code SubmitNodeCollector#submitItem} inside
     * {@code LayerRenderState#submit}, i.e. with the item's own model-space pose and its own quads.
     *
     * @return {@code true} when the effect is active for this item, in which case the <b>caller must not make
     *         the vanilla submission</b> - the item is deliberately invisible and only the overlay is drawn,
     *         which is the requested "dissolving" look. {@code false} means "draw the item normally", and
     *         covers every not-active case plus the cases where no overlay could be built at all.
     */
    public static boolean suppressAndOverlay(Object renderState, List<BakedQuad> itemQuads,
                                             PoseStack poseStack, SubmitNodeCollector collector) {
        Captured captured = CAPTURED.get(renderState);
        if (captured == null) {
            return false;
        }
        ItemStack stack = captured.stack();
        ItemDisplayContext ctx = captured.context();
        if (stack.isEmpty() || ctx == null) {
            return false;
        }

        List<ItemLayerBinding> bindings = ItemRenderRegistry.resolve(stack, ctx);
        if (bindings.isEmpty()) {
            return false;
        }
        if (!ItemShaderPipelines.isPipelineRegistered()) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }
        long gameTime = mc.level.getGameTime();

        for (ItemLayerBinding binding : bindings) {
            IItemShaderLayer layer = binding.layer();
            ItemLayerConfig config = binding.config();
            if (!layer.isReady()) {
                continue;
            }
            ItemLayerPayload payload = layer.prepare(stack, config, ctx, gameTime);
            if (payload == null) {
                continue; // outside the burst window
            }

            // A special-model item (or any model that submitted no quads) has no geometry to build the overlay
            // from. Returning false keeps it VISIBLE rather than making it vanish with nothing to show: the
            // alternative would be an item that silently disappears while the effect runs. Documented.
            if (itemQuads == null || itemQuads.isEmpty()) {
                return false;
            }

            Identifier maskId = layer.maskTexture(stack, config);
            if (maskId == null) {
                maskId = ItemRenderRegistry.defaultMaskFor(stack);
            }

            // The mask is a texture resource of its own, NOT an atlas sprite. See ItemMaskTexture for the crash
            // the atlas route caused (AtlasManager#getAtlasOrThrow wants an atlas id, not the texture path held
            // by TextureAtlas.LOCATION_BLOCKS) and for the frame-count logic.
            ItemMaskTexture.MaskInfo mask = ItemMaskTexture.resolve(maskId);
            if (mask.unusable()) {
                // Missing or unreadable mask: keep the item visible rather than blanking it. ItemMaskTexture
                // logs once per id.
                continue;
            }
            RenderType renderType = ItemShaderPipelines.renderTypeFor(mask.texturePath());
            if (renderType == null) {
                return false;
            }

            int frames = mask.frames();
            int frame = ItemMaskTexture.currentFrame(mask, gameTime);
            final List<BakedQuad> quads = itemQuads;

            // The per-draw payload, packed into the slots the vertex format declares. See ItemShaderPipelines
            // for the table and ItemLayerPayload for the semantics.
            float tintR = payload.tintRed();
            float tintG = payload.tintGreen();
            float tintB = payload.tintBlue();
            int intensity16 = payload.packedIntensity();
            int split16 = payload.packedSplitStrength();
            // The animation clock goes through LINE_WIDTH as a plain float, which is exactly the precision the
            // 1.20.1 `time` uniform had.
            float animClock = payload.timeTicks();

            // Every layer of the item is suppressed (each layer's redirect returns true below), but the overlay
            // itself is emitted only once per render state per submit: two emissions would stack the identical
            // overlay and double the tint. The guard is reset by ItemCorruptionRenderer#beginSubmit.
            if (EMITTED.add(renderState)) {
                // Geometry-level decay: the jitter goes on the pose, so the overlay moves with the same
                // magnitudes and frequency as the 1.20.1 twitch.
                poseStack.pushPose();
                try {
                    if (layer.usesTwitchTransform(stack, config)) {
                        layer.applyTwitch(poseStack, stack, config, gameTime);
                    }
                    collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                        for (BakedQuad quad : quads) {
                            ItemShaderBakery.emitQuad(consumer, pose, quad, frames, frame,
                                    tintR, tintG, tintB, intensity16, split16, animClock);
                        }
                    });
                } finally {
                    poseStack.popPose();
                }
            }
            // Active: suppress the vanilla submission for this layer.
            return true;
        }
        return false;
    }

    /** Drops every captured entry and every cached mask/render-type resolution (level unload, reload, debug). */
    public static void clear() {
        CAPTURED.clear();
        EMITTED.clear();
        // The geometry is not cached any more: it comes from the item's own quads, which are rebuilt with
        // the model. What is cached is the per-mask resolution and its render type.
        ItemMaskTexture.invalidate();
        ItemShaderPipelines.invalidateRenderTypes();
    }

    /** Diagnostics: how many render states currently have a captured stack. */
    public static int capturedCount() {
        return CAPTURED.size();
    }

    /** Convenience used by the twitch registry path, kept for API parity with 1.20.1. */
    public static boolean isTwitchEnabled(ItemStack stack, ItemDisplayContext ctx) {
        return ItemRenderRegistry.isTwitchEnabled(stack, ctx);
    }

    /** The corruption layer singleton, for callers that want to poke its defaults. */
    public static IItemShaderLayer corruption() {
        return CorruptionLayer.INSTANCE;
    }
}
