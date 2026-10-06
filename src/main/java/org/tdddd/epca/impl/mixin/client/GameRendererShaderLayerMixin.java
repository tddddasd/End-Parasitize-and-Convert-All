package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.compat.ItemLayerLateRenderQueue;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureRenderer;

/**
 *    RottenRuinsSplendiding
 * {@code CosmicAfterLevelMixin}
 *
 * <h3> {@code GameRenderer.renderLevel()}</h3>
 *  GBuffer composite <b>
 * framebuffer</b>
 *
 * <ol>
 *   <li><b>{@code renderHand} </b>
 *
 *        {@code LEQUAL + polygonOffset} </li>
 *   <li><b>{@code renderLevel} TAIL</b>
 *        {@code NO_DEPTH_TEST}</li>
 * </ol>
 *
 * <p>{@code priority = 500}  mixin
 * </p>
 */
@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererShaderLayerMixin {

    /**
     *
     *
     * <p> {@code RenderLevelStageEvent.AFTER_SKY} shadow pass
     *  {@code LevelRenderer.renderLevel()} AFTER_SKY
     * {@code GameRenderer.render} </p>
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void epca$updateSkyRupture(float partialTick, long nanoTime, boolean renderLevel,
                                       CallbackInfo ci) {
        SkyRuptureRenderer.update();
    }

    /**
     *  1
     */
    @Inject(method = "renderLevel",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z",
                    ordinal = 0))
    private void epca$replayItemLayersBeforeHand(float partialTick, long finishTimeNano,
                                                 PoseStack poseStack, CallbackInfo ci) {
        //  AFTER_SKY
        //    GBuffer
        SkyRuptureRenderer.renderDeferred();

        if (IrisShaderCompat.isShaderPackActive()) {
            ItemLayerLateRenderQueue.renderNonFirstPerson();
        } else {
            ItemLayerLateRenderQueue.clear();
        }
    }

    /**
     *  2 renderLevel
     */
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void epca$replayItemLayersAfterLevel(float partialTick, long finishTimeNano,
                                                 PoseStack poseStack, CallbackInfo ci) {
        if (IrisShaderCompat.isShaderPackActive()) {
            ItemLayerLateRenderQueue.renderAll();
        } else {
            ItemLayerLateRenderQueue.clear();
        }
    }
}

