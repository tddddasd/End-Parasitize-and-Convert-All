package org.tdddd.epca.impl.client.blackhole;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;

public final class BlackHoleSceneCopy {

    public static final ResourceLocation TEXTURE_ID =
            new ResourceLocation(epca.MODID, "blackhole_scene_copy");

    private static final int GL_TEXTURE_2D = 3553;
    private static final int GL_TEXTURE_MIN_FILTER = 10241;
    private static final int GL_TEXTURE_MAG_FILTER = 10240;
    private static final int GL_TEXTURE_WRAP_S = 10242;
    private static final int GL_TEXTURE_WRAP_T = 10243;
    private static final int GL_LINEAR = 9729;
    private static final int GL_CLAMP_TO_EDGE = 33071;

    private static DynamicTexture texture;
    private static int textureId;
    private static int width;
    private static int height;
    private static boolean ready;
    private static long lastCaptureToken = Long.MIN_VALUE;
    private static boolean recreatedThisFrame;

    private BlackHoleSceneCopy() { }

    public static boolean wasRecreated() {
        return recreatedThisFrame;
    }

    public static void captureFrame(long frameToken) {
        recreatedThisFrame = false;
        if (frameToken == lastCaptureToken) return;
        lastCaptureToken = frameToken;

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.getMainRenderTarget();
        if (target == null || target.width <= 0 || target.height <= 0) return;
        if (!ensureTexture(target.width, target.height)) return;

        target.bindWrite(false);
        GlStateManager._bindTexture(textureId);
        try {
            GlStateManager._glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        } catch (Throwable throwable) {
            ready = false;
            epca.LOGGER.warn("[blackhole] scene copy failed, the lens is disabled", throwable);
        }
    }

    private static boolean ensureTexture(int requestedWidth, int requestedHeight) {
        if (ready && width == requestedWidth && height == requestedHeight) return true;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            DynamicTexture created = new DynamicTexture(requestedWidth, requestedHeight, true);
            minecraft.getTextureManager().register(TEXTURE_ID, created);
            int id = created.getId();
            if (id <= 0) { created.close(); return false; }
            GlStateManager._bindTexture(id);
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
            if (previous != null) previous.close();
            recreatedThisFrame = true;
            lastCaptureToken = Long.MIN_VALUE;
            return true;
        } catch (Throwable throwable) {
            ready = false;
            textureId = 0;
            width = 0;
            height = 0;
            epca.LOGGER.warn("[blackhole] could not create the scene copy texture", throwable);
            return false;
        }
    }
}