package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.ItemLayerBinding;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.client.render.ItemShaderRenderHelper;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.compat.ItemLayerLateRenderQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品 shader 层的渲染注入点 —— 对应 RottenRuinsSplendiding 的
 * {@code MixinItemRendererCosmic}。
 *
 * <h3>注入位置</h3>
 * {@code ItemRenderer.render()} 里<b>最后一个</b> {@code popPose()} 之前，
 * 也就是“原版已经把物品模型画完”的那一刻。此时：
 * <ul>
 *   <li>基础贴图已经写入深度缓冲 → 我们的层可以用 EQUAL 深度测试精确贴合轮廓</li>
 *   <li>还没有 popPose → 当前 PoseStack 正是物品的变换，直接复用即可</li>
 * </ul>
 *
 * <p><b>为什么必须写 {@code ordinal = 1}：</b>
 * 1.20.1 的 {@code ItemRenderer.render} 里有<b>两处</b> {@code popPose()} ——
 * offset 385 处那一处只在“带附魔光效的指南针/时钟”（{@code hasAnimatedTexture && hasFoil}）
 * 的循环里 push/pop 配对使用，offset 475 处才是所有物品共用的真正出口。
 * Mixin 的 {@code @At(INVOKE)} 默认会在<b>每个</b>匹配点上注入，那样这类物品
 * 每帧会重复画 N+1 次崩坏层、并往延迟队列里塞重复条目。
 * 这里显式取第 2 个匹配（也就是共用出口），保证任何物品都只画一次。</p>
 *
 * <h3>两条路径</h3>
 * <ul>
 *   <li><b>无光影 / GUI</b> → 立即绘制（EQUAL 深度）</li>
 *   <li><b>光影激活</b> → 快照入队，等 {@code GameRenderer.renderLevel()} 结束后
 *       回放到主 framebuffer（见 {@link ItemLayerLateRenderQueue}）</li>
 * </ul>
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererShaderLayerMixin {

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE))
    private void epca$renderItemShaderLayers(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                             PoseStack poseStack, MultiBufferSource buffer,
                                             int packedLight, int packedOverlay, BakedModel model,
                                             CallbackInfo ci) {
        if (stack.isEmpty()) {
            return;
        }

        List<ItemLayerBinding> bindings = ItemRenderRegistry.resolve(stack, context);
        if (bindings.isEmpty()) {
            return;
        }

        // 自定义渲染器（如部分 GeckoLib 物品）没有走标准 quad 路径写深度，
        // EQUAL 深度测试必然画不出来 —— 直接跳过，省掉无意义的 draw。
        if (model.isCustomRenderer()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        // 先把基础物品 flush 出去，保证深度已经在 GPU 深度缓冲里
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }

        // 光影激活 → 延迟到世界渲染结束后回放（绕过光影 GBuffer）
        if (IrisShaderCompat.shouldDeferItemLayer(context)) {
            List<ItemLayerBinding> deferrable = new ArrayList<>(bindings.size());
            for (ItemLayerBinding binding : bindings) {
                if (binding.layer().supportsDeferredReplay()) {
                    deferrable.add(binding);
                }
            }
            if (!deferrable.isEmpty()) {
                ItemLayerLateRenderQueue.enqueue(stack, context, deferrable, poseStack, packedLight, packedOverlay);
            }
            return;
        }

        // 即时绘制
        for (ItemLayerBinding binding : bindings) {
            ItemShaderRenderHelper.drawBinding(binding, stack, context, poseStack, buffer,
                    packedLight, packedOverlay, false);
        }
    }
}
