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

public final class ArayaSceneCopy {

    public static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath(epca.MODID, "araya_scene_copy");

    private static final class SceneCopyTexture extends AbstractTexture {
        private SceneCopyTexture(GpuTexture texture, GpuTextureView view, GpuSampler sampler) {
            this.texture = texture;
            this.textureView = view;
            this.sampler = sampler;
        }
    }

    private static SceneCopyTexture wrapper;

    private static GpuTexture texture;

    private static int width;
    private static int height;

    private static boolean ready;

    private static long lastCaptureToken = Long.MIN_VALUE;

    private ArayaSceneCopy() {
    }

    public static boolean isReady() {
        return ready;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

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

    public static void invalidate() {
        lastCaptureToken = Long.MIN_VALUE;
    }

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
