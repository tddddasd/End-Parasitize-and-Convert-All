package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;

/**
 * The temporary scene texture the slash's transparent border refracts.
 *
 * <h2>What "refraction" means here</h2>
 * <p>The band around the slash is transparent, but it must not simply show the scene: it has to bend it
 * ("会折射透明部分后面的景象"). Faking that with a glow or a colour shift is explicitly not what was asked
 * for, so the band really samples the frame: once per frame the finished main colour buffer is copied into
 * a private texture, and the band's fragment stage samples that copy with an offset computed from the
 * geometry. Because the copy is taken <i>before</i> the band is drawn, there is no feedback loop - the
 * band reads the frame as it was a moment ago, which is what a refraction of the background looks like.</p>
 *
 * <h2>How the copy works on 26.1.2</h2>
 * <p>1.20.1 used {@code glCopyTexSubImage2D}, which does not exist here any more: 26.1.2 has no
 * {@code GlStateManager} texture calls and no raw GL texture ids, only the {@code GpuDevice} /
 * {@code GpuTexture} abstraction. The route that does exist is
 * {@code CommandEncoder#copyTextureToTexture}, which takes exactly the two {@link GpuTexture}s the copy
 * needs: the main render target's colour texture as the source and this class's own texture as the
 * destination. Because the shader has to be able to <i>bind</i> that texture by name,
 * {@link AbstractTexture} wraps it and {@code TextureManager#register} publishes it under
 * {@link #TEXTURE_ID}, which is what the band's render setup names.</p>
 *
 * <h2>Limits, stated honestly</h2>
 * <ul>
 *   <li>The copy is taken from the <b>main</b> render target. With Iris and a shader pack active, that
 *       target at the {@code AfterTranslucentParticles} stage holds the composite result rather than the
 *       final image, so the band refracts a slightly different (usually pre-tonemap) version of the
 *       scene. It is still the real frame, never a fake, but it is not guaranteed to be pixel-identical
 *       to what is on screen.</li>
 *   <li>Only one copy is made per frame, at the first moment a slash is drawn in that frame. A second
 *       slash in the same frame samples the same copy, which is invisible in practice because both are
 *       drawn within one frame.</li>
 *   <li>If the texture cannot be created or the copy fails, {@link #isReady()} stays false and the
 *       renderer draws the glowing line <b>without</b> the refracting band instead of failing the frame.
 *       That is the documented fallback.</li>
 * </ul>
 */
public final class ArayaSceneCopy {

    /** Texture id the copy is registered under; the band's render setup binds it by this name. */
    public static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath(epca.MODID, "araya_scene_copy");

    /** Wrapper that publishes the copy to the texture manager so a sampler can bind it. */
    private static final class SceneCopyTexture extends AbstractTexture {
        private SceneCopyTexture(GpuTexture texture, GpuTextureView view, GpuSampler sampler) {
            this.texture = texture;
            this.textureView = view;
            this.sampler = sampler;
        }
    }

    /** The private texture; {@code null} until the first successful capture. */
    private static SceneCopyTexture wrapper;

    /** The GPU texture the frame is copied into, or {@code null}. */
    private static GpuTexture texture;

    /** Size of {@link #texture}; it has to match the framebuffer or the copy is rejected. */
    private static int width;
    private static int height;

    /** True while {@link #texture} is a usable GPU texture. */
    private static boolean ready;

    /** The frame token the copy was last refreshed at. */
    private static long lastCaptureToken = Long.MIN_VALUE;

    private ArayaSceneCopy() {
    }

    /** True when the band can actually sample a frame copy. */
    public static boolean isReady() {
        return ready;
    }

    /** Width of the copy in pixels, or 0. */
    public static int width() {
        return width;
    }

    /** Height of the copy in pixels, or 0. */
    public static int height() {
        return height;
    }

    /**
     * Copies the finished frame into {@link #texture}, at most once per distinct {@code frameToken}.
     *
     * @param frameToken monotonically increasing token that changes once per rendered frame
     */
    public static void captureFrame(long frameToken) {
        if (frameToken == lastCaptureToken) {
            return;
        }
        lastCaptureToken = frameToken;

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.getMainRenderTarget();
        if (target == null || target.width <= 0 || target.height <= 0) {
            return;
        }
        if (!ensureTexture(target.width, target.height)) {
            return;
        }
        GpuTexture source = target.getColorTexture();
        if (source == null) {
            return;
        }
        try {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(source, texture, 0, 0, 0, 0,
                    0, width, height);
        } catch (Throwable throwable) {
            ready = false;
            epca.LOGGER.warn("[epca-araya] scene copy failed, the refraction band is disabled", throwable);
        }
    }

    /** Forces a fresh copy on the next rendered frame; used when the window or the level changed. */
    public static void invalidate() {
        lastCaptureToken = Long.MIN_VALUE;
    }

    /**
     * Creates the private texture at the framebuffer size, or recreates it when the window changed.
     *
     * @return true when {@link #texture} is a usable GPU texture of the requested size
     */
    private static boolean ensureTexture(int requestedWidth, int requestedHeight) {
        if (ready && width == requestedWidth && height == requestedHeight) {
            return true;
        }
        try {
            Minecraft minecraft = Minecraft.getInstance();
            GpuTexture created = RenderSystem.getDevice().createTexture("epca_araya_scene_copy",
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, TextureFormat.RGBA8,
                    requestedWidth, requestedHeight, 1, 1);
            GpuTextureView view = RenderSystem.getDevice().createTextureView(created);
            // Clamped edges and linear filtering: the band magnifies the copy a little, and clamping
            // stops the border from wrapping the opposite screen edge into the band.
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            SceneCopyTexture created2 = new SceneCopyTexture(created, view, sampler);
            minecraft.getTextureManager().register(TEXTURE_ID, created2);

            SceneCopyTexture previous = wrapper;
            wrapper = created2;
            texture = created;
            width = requestedWidth;
            height = requestedHeight;
            ready = true;
            if (previous != null) {
                previous.close();
            }
            lastCaptureToken = Long.MIN_VALUE;
            return true;
        } catch (Throwable throwable) {
            ready = false;
            texture = null;
            width = 0;
            height = 0;
            epca.LOGGER.warn("[epca-araya] could not create the scene copy texture, the refraction band "
                    + "is disabled", throwable);
            return false;
        }
    }
}
