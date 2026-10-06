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
 *  RottenRuinsSplendiding
 * {@code MixinItemRendererTwitch}
 *
 * <p> {@code ItemRenderer.render()}  HEAD
 *
 * </p>
 *
 * <h3></h3>
 * <ul>
 *   <li> {@link ITwitchItem}</li>
 *   <li> {@code ItemLayerConfig.twitch(true)} </li>
 * </ul>
 * GUI
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

