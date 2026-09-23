package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.render.sky.DarknessDevourEffect;

/**
 * The grounding of the "darkness devour" effect: the fog closes in from the world edge until the
 * terrain itself is eaten.
 *
 * <h2>1.20.1 -&gt; 26.1.2: a different method and a different mutation model</h2>
 * The 1.20.1 twin injected at the TAIL of {@code FogRenderer.setupFog(Camera, FogMode, float, boolean,
 * float)} and pushed the new values into the global {@code RenderSystem} state
 * ({@code setShaderFogStart/End/Color}); vanilla then read them back when it built the fog uniforms.
 *
 * <p>Neither the {@code FogMode} overload nor any {@code RenderSystem} fog setter survived into
 * 26.1.2. Verified with {@code javap -s -p} on the 26.1.2 patched jar, the only setup entry point is
 * now</p>
 * <pre>
 *   public net.minecraft.client.renderer.fog.FogData setupFog(
 *       net.minecraft.client.Camera, int, net.minecraft.client.DeltaTracker, float,
 *       net.minecraft.client.multiplayer.ClientLevel);
 * </pre>
 * <p>and it <b>allocates a fresh {@link FogData} on every call</b> (bytecode offset 33:
 * {@code invokespecial FogData."&lt;init&gt;":()V}), fills it from the {@code FogEnvironment} list and
 * the render distance, then lets NeoForge post-process it through {@code ClientHooks.onSetupFog}. The
 * caller ({@code GameRenderer}) hands the returned object to
 * {@code FogRenderer#updateBuffer(FogData)}, which is what uploads the {@code Fog} std140 block that
 * {@code assets/minecraft/shaders/include/fog.glsl} declares.</p>
 *
 * <p>Because the value object is fresh and is returned to the caller, the correct hook is
 * {@code @At("RETURN")} with a {@code CallbackInfoReturnable<FogData>}: mutating the returned object
 * is exactly equivalent to 1.20.1's global write, and it is strictly safer - it cannot affect the
 * nether, underwater or potion fog, because those are only skipped when the effect is not running.</p>
 *
 * <p>The method is an instance method (1.20.1's was {@code private static}), so the handler is an
 * instance method too.</p>
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererDevourMixin {

    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;F"
            + "Lnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
            at = @At("RETURN"))
    private void epca$applyDevourFog(Camera camera, int renderDistance, DeltaTracker deltaTracker,
                                     float partialTick, ClientLevel level,
                                     CallbackInfoReturnable<FogData> cir) {
        FogData fog = cir.getReturnValue();
        if (fog != null) {
            DarknessDevourEffect.applyFog(fog, (float) renderDistance);
        }
    }
}
