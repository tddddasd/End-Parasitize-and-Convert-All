package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.entity.heart.SoulProtectionHeartRenderer;

/**
 * Submits the soul protection flame for every living entity that carries {@code epca:soul_protection}.
 *
 * <h2>Why this hook</h2>
 * <p>26.1.2 rendering is extract-then-submit, and the mod's living entity hook is
 * {@link LivingEntityRendererTintMixin}, a mixin on {@link LivingEntityRenderer}. This is the same
 * renderer path: {@code submit} is the only place that runs once per rendered living entity, for
 * vanilla mobs and for players alike, because {@code EntityRenderDispatcher#submit} always calls
 * {@code renderer.submit(...)}.</p>
 *
 * <p>The injection point is {@code TAIL}, i.e. just before the final return, which is after the
 * renderer's own {@code pushPose}/{@code popPose} pair and after the call to
 * {@code EntityRenderer#submit} (whose name tag path is balanced as well). The top of the pose stack
 * is therefore the camera-relative entity transform that {@code EntityRenderDispatcher#submit}
 * pushed, which is exactly the basis the gas cloud billboards are anchored to. Verified with
 * {@code javap -s -p} on the 26.1.2 patched jar, which reports for that method:
 * {@code (Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V}
 * - the descriptor below is that string verbatim, because {@code LivingEntityRenderer} also carries
 * a synthesised bridge with the erased {@code EntityRenderState} parameter.</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererHeartMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL"))
    private void epca$submitSoulProtectionHeart(LivingEntityRenderState state, PoseStack poseStack,
                                                SubmitNodeCollector collector, CameraRenderState camera,
                                                CallbackInfo ci) {
        // A copy, because the submission path resets the top pose to this basis.
        SoulProtectionHeartRenderer.submit(state, poseStack, poseStack.last().copy(), collector);
    }
}
