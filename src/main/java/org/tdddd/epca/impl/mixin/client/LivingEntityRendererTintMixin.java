package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.ClientEntityTint;


@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererTintMixin {

    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void epca$applyModelTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        Integer tint = state.getRenderData(ClientEntityTint.MODEL_TINT);
        if (tint == null) {
            return;
        }
        cir.setReturnValue(ARGB.multiply(cir.getReturnValueI(), tint));
    }
}
