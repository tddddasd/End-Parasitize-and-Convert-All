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
import org.tdddd.epca.impl.client.render.layer.CorruptionPulse;
import org.tdddd.epca.impl.client.render.twitch.ITwitchItem;

/**
 * 几何层面的“崩坏”：给物品叠加鬼畜抖动，对应 RottenRuinsSplendiding 的
 * {@code MixinItemRendererTwitch}。
 *
 * <p>注入在 {@code ItemRenderer.render()} 的 HEAD，也就是所有变换之前 ——
 * 基础贴图、崩坏层、光影延迟回放录下的矩阵都会带上同一份抖动，因此
 * 几何与颜色两层崩坏完全同步。</p>
 *
 * <h3>触发条件</h3>
 * <ul>
 *   <li>物品实现 {@link ITwitchItem}，或</li>
 *   <li>某个已注册层把 {@code ItemLayerConfig.twitch(true)} 打开了</li>
 * </ul>
 * GUI 里永不抖动（背包里抖会没法用）。
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererTwitchMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void epca$applyCorruptionTwitch(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                            PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, int packedOverlay, BakedModel model,
                                            CallbackInfo ci) {
        if (stack.isEmpty() || context == ItemDisplayContext.GUI) {
            return;
        }

        boolean twitch = false;
        float strength = 1.0f;

        if (stack.getItem() instanceof ITwitchItem twitchItem) {
            if (!twitchItem.twitchShouldRender(context)) {
                return;
            }
            twitch = true;
        }

        for (ItemLayerBinding binding : ItemRenderRegistry.resolveAll(stack)) {
            if (!binding.config().shouldRender(stack, context)) {
                continue;
            }
            if (binding.layer().usesTwitchTransform(stack, binding.config())) {
                twitch = true;
                strength = Math.max(strength, binding.config().strength());
                break;
            }
        }

        if (!twitch) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        CorruptionPulse.applyTwitch(poseStack, stack, mc.level.getGameTime(), strength);
    }
}
