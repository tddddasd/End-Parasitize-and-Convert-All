package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
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
 * <h2>Emitting the extra pass</h2>
 * {@code OrderedSubmitNodeCollector#submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)}
 * hands back a pose plus a raw {@code VertexConsumer}, and the hand-built {@link BakedQuad}s are
 * written with {@code VertexConsumer#putBakedQuad(pose, quad, QuadInstance)}. No custom
 * {@code VertexConsumer} is involved; that method writes POSITION, COLOR, UV0, UV1, UV2 and NORMAL,
 * which is exactly the format {@link ItemShaderPipelines} declares.
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
    public static void emitForLayer(Object renderState, PoseStack poseStack,
                                    SubmitNodeCollector collector,
                                    int packedLight, int packedOverlay) {
        Captured captured = CAPTURED.get(renderState);
        if (captured == null) {
            return;
        }
        ItemStack stack = captured.stack();
        ItemDisplayContext ctx = captured.context();
        if (stack.isEmpty() || ctx == null) {
            return;
        }

        List<ItemLayerBinding> bindings = ItemRenderRegistry.resolve(stack, ctx);
        if (bindings.isEmpty()) {
            return;
        }

        RenderType renderType = ItemShaderPipelines.corruptionRenderType();
        if (renderType == null) {
            // Pipeline not registered yet (resource reload / very early frame).
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
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
                continue;
            }
            // One overlay per render state per submit, whatever the item's layer count is.
            if (!EMITTED.add(renderState)) {
                return;
            }

            Identifier maskId = layer.maskTexture(stack, config);
            if (maskId == null) {
                maskId = ItemRenderRegistry.defaultMaskFor(stack);
            }
            TextureAtlas atlas = mc.getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS);
            TextureAtlasSprite sprite = atlas.getSprite(maskId);
            List<BakedQuad> quads = ItemShaderBakery.quads(sprite, renderType);

            QuadInstance instance = new QuadInstance();
            // COLOR carries the tint; putBakedQuad multiplies it by the quad's own baked colour,
            // which is white for the quads built here.
            instance.setColor(net.minecraft.util.ARGB.color(
                    255,
                    channel(payload.tintRed()),
                    channel(payload.tintGreen()),
                    channel(payload.tintBlue())));
            // UV1 carries the full 32-bit tick clock, split into two 16-bit halves by putBakedQuad.
            instance.setOverlayCoords(payload.timeTicks());
            // UV2 carries intensity and split strength as 16-bit fixed point.
            instance.setLightCoords((payload.packedSplitStrength() << 16) | (payload.packedIntensity() & 0xFFFF));

            // Geometry-level decay: the jitter goes on the pose, so the overlay moves with the same
            // magnitudes and frequency as the 1.20.1 twitch.
            poseStack.pushPose();
            try {
                if (layer.usesTwitchTransform(stack, config)) {
                    layer.applyTwitch(poseStack, stack, config, gameTime);
                }
                collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                    for (BakedQuad quad : quads) {
                        consumer.putBakedQuad(pose, quad, instance);
                    }
                });
            } finally {
                poseStack.popPose();
            }
            return;
        }
    }

    /** 0..1 float -&gt; 0..255 byte. */
    private static int channel(float value) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, value)) * 255.0f);
    }

    /** Drops every captured entry (level unload / debugging). */
    public static void clear() {
        CAPTURED.clear();
        EMITTED.clear();
        ItemShaderBakery.invalidate();
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
