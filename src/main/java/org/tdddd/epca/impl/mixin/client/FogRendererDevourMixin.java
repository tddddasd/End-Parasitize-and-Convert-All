package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.sky.DarknessDevourEffect;

/**
 *
 *
 * <p>{@code FogRenderer.setupFog}  {@code RenderSystem}
 *  {@code VertexBuffer.drawWithShader} {@code LevelRenderer}
 * / TAIL
 * ""
 * </p>
 *
 * <p> {@code priority = 1200}TAIL
 * </p>
 *
 * <p> {@code static} {@code static}</p>
 */
@Mixin(value = FogRenderer.class, priority = 1200)
public abstract class FogRendererDevourMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void epca$applyDevourFog(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                            boolean thickFog, float partialTick, CallbackInfo ci) {
        DarknessDevourEffect.applyFog(renderDistance);
    }
}

