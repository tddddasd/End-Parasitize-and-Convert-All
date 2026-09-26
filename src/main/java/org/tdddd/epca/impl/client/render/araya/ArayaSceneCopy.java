package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;

/**
 * The temporary scene texture the slash's transparent border refracts.
 *
 * <h2>What "refraction" means here</h2>
 * <p>The band around the slash is transparent, but it must not simply show the scene: it has to bend it
 * ("会折射透明部分后面的景象"). Faking that with a glow or a colour shift is explicitly not what was
 * asked for, so the band really samples the frame: once per frame the finished main colour buffer is
 * copied into a private texture, and the band's fragment stage samples that copy with a small offset
 * computed from the geometry. Because the copy is taken <i>before</i> the band is drawn, there is no
 * feedback loop - the band reads the frame as it was a moment ago, which is what a refraction of the
 * background looks like.</p>
 *
 * <h2>Why the copy is a {@code glCopyTexSubImage2D}</h2>
 * <p>It is the only route on 1.20.1 that touches neither the render target nor the resource pack.
 * {@code RenderTarget} exposes no copy-to-texture entry point here, and a texture a core shader can
 * sample has to be a real GL texture id; {@link DynamicTexture} provides exactly that (it allocates the
 * id and keeps it alive) while this class keeps it in step with the framebuffer size and registers it
 * under {@link #TEXTURE_ID}, which is the name the band's render type binds.</p>
 *
 * <h2>Limits, stated honestly</h2>
 * <ul>
 *   <li>The copy is taken from the <b>main</b> render target. With Embeddium/Iris and a shader pack
 *       active, that target at the {@code AFTER_PARTICLES} stage holds the deferred/composite result
 *       rather than the final image, so the band refracts a slightly different (usually pre-tonemap)
 *       version of the scene. It is still the real frame, never a fake, but it is not guaranteed to be
 *       pixel-identical to what is on screen.</li>
 *   <li>Only one copy is made per frame, at the first moment a slash is drawn in that frame (the
 *       caller passes a frame token). A second slash in the same frame samples the same copy, which is
 *       invisible in practice because both are drawn within one frame.</li>
 *   <li>If the GL texture cannot be created (driver failure, no context), {@link #isReady()} stays
 *       false and the renderer draws the glowing line <b>without</b> the refracting band instead of
 *       failing the frame. That is the documented fallback.</li>
 * </ul>
 */
public final class ArayaSceneCopy {

    /** Texture id the copy is registered under; the band's render type binds it by this name. */
    public static final ResourceLocation TEXTURE_ID =
            new ResourceLocation(epca.MODID, "araya_scene_copy");

    // OpenGL enums, spelled out because the 1.20.1 {@code GlStateManager} keeps them private and the
    // LWJGL GL11 class is not on the javac-check classpath. The values are fixed by the GL spec.
    private static final int GL_TEXTURE_2D = 3553;
    private static final int GL_TEXTURE_MIN_FILTER = 10241;
    private static final int GL_TEXTURE_MAG_FILTER = 10240;
    private static final int GL_TEXTURE_WRAP_S = 10242;
    private static final int GL_TEXTURE_WRAP_T = 10243;
    private static final int GL_LINEAR = 9729;
    private static final int GL_CLAMP_TO_EDGE = 33071;

    /** The private texture; {@code null} until {@link #captureFrame} succeeded once. */
    private static DynamicTexture texture;

    /** Raw GL texture id of {@link #texture}, or 0 before the first successful capture. */
    private static int textureId;

    /** Size of {@link #texture}; it has to match the framebuffer or the copy is rejected by GL. */
    private static int width;
    private static int height;

    /** True while {@link #texture} is a usable GL texture. */
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
     * <p>The token is supplied by the renderer (one increment per render pass), so a frame that draws
     * several slashes pays for one copy and a frame that draws none pays for none.</p>
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

        // The main target is the copy source. bindWrite is called even though the level stage normally
        // leaves it bound: glCopyTexSubImage2D reads whatever is bound for reading, and this is the one
        // place that makes it explicit. Binding it for writing again restores exactly the state the
        // stage expects.
        target.bindWrite(false);
        GlStateManager._bindTexture(textureId);
        try {
            GlStateManager._glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
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
     * @return true when {@link #textureId} is a usable GL texture of the requested size
     */
    private static boolean ensureTexture(int requestedWidth, int requestedHeight) {
        if (ready && width == requestedWidth && height == requestedHeight) {
            return true;
        }
        try {
            Minecraft minecraft = Minecraft.getInstance();
            DynamicTexture created = new DynamicTexture(requestedWidth, requestedHeight, true);
            // Registers the texture under TEXTURE_ID so the band's render type can bind it by name. The
            // no-mipmap constructor argument keeps the copy a plain 2D texture, which is all the band
            // needs and stops the driver from wanting a mip chain the copy never fills.
            minecraft.getTextureManager().register(TEXTURE_ID, created);
            int id = created.getId();
            if (id <= 0) {
                created.close();
                return false;
            }
            GlStateManager._bindTexture(id);
            // Linear filtering and clamped edges: the band magnifies the copy a little, and clamping
            // stops the border from wrapping the opposite screen edge into the band.
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            DynamicTexture previous = texture;
            texture = created;
            textureId = id;
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
            textureId = 0;
            width = 0;
            height = 0;
            epca.LOGGER.warn("[epca-araya] could not create the scene copy texture, the refraction band "
                    + "is disabled", throwable);
            return false;
        }
    }
}
