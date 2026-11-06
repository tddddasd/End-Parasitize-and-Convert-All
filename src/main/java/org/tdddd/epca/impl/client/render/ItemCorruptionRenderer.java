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

public final class ItemCorruptionRenderer {

    private static final Map<Object, Captured> CAPTURED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final Set<Object> EMITTED =
            Collections.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));

    private ItemCorruptionRenderer() {
    }

    public record Captured(ItemStack stack, ItemDisplayContext context) {
    }

    public static void capture(Object renderState, ItemStack stack, ItemDisplayContext context) {
        if (renderState == null || stack == null || stack.isEmpty()) {
            return;
        }
        CAPTURED.put(renderState, new Captured(stack, context));
    }

    public static void beginSubmit(Object renderState) {
        EMITTED.remove(renderState);
    }

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
                continue;
            }

            if (itemQuads == null || itemQuads.isEmpty()) {
                return false;
            }

            Identifier maskId = layer.maskTexture(stack, config);
            if (maskId == null) {
                maskId = ItemRenderRegistry.defaultMaskFor(stack);
            }

            ItemMaskTexture.MaskInfo mask = ItemMaskTexture.resolve(maskId);
            if (mask.unusable()) {

                continue;
            }
            RenderType renderType = ItemShaderPipelines.renderTypeFor(mask.texturePath());
            if (renderType == null) {
                return false;
            }

            int frames = mask.frames();
            int frame = ItemMaskTexture.currentFrame(mask, gameTime);
            final List<BakedQuad> quads = itemQuads;

            float tintR = payload.tintRed();
            float tintG = payload.tintGreen();
            float tintB = payload.tintBlue();
            int intensity16 = payload.packedIntensity();
            int split16 = payload.packedSplitStrength();

            float animClock = payload.timeTicks();

            if (EMITTED.add(renderState)) {

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

            return true;
        }
        return false;
    }

    public static void clear() {
        CAPTURED.clear();
        EMITTED.clear();

        ItemMaskTexture.invalidate();
        ItemShaderPipelines.invalidateRenderTypes();
    }

    public static int capturedCount() {
        return CAPTURED.size();
    }

    public static boolean isTwitchEnabled(ItemStack stack, ItemDisplayContext ctx) {
        return ItemRenderRegistry.isTwitchEnabled(stack, ctx);
    }

    public static IItemShaderLayer corruption() {
        return CorruptionLayer.INSTANCE;
    }
}
