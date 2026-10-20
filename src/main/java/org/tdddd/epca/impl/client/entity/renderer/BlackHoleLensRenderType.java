package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

/**
 * The lens ring around the black hole.
 *
 * <p>The ring has to sample what is already on screen, so its texture is the main framebuffer's colour
 * attachment rather than a resource texture. A RenderType binds its own texture state at flush time,
 * which would replace that attachment, so the ring is submitted directly with the custom core shader
 * {@code epca:blackhole_lens} bound and the scene texture in slot 0.</p>
 */
public final class BlackHoleLensRenderType {

    private static ShaderInstance shader;

    private BlackHoleLensRenderType() {
    }

    /** Called from {@code RegisterShadersEvent}. */
    public static void registerShader(ShaderInstance instance) {
        shader = instance;
    }

    public static boolean isReady() {
        return shader != null;
    }

    /**
     * Draws a camera facing ring of {@code radius} blocks with the scene behind it swirled around the
     * cube silhouette.
     */
    public static void draw(PoseStack poseStack, float radius, float spin) {
        if (shader == null) {
            return;
        }
        // The scene has to be a real texture a core shader can sample; ArayaSceneCopy already keeps a
        // private DynamicTexture in step with the framebuffer for exactly this purpose.
        org.tdddd.epca.impl.client.render.araya.ArayaSceneCopy.captureFrame((long) (spin * 1000.0F));
        if (!org.tdddd.epca.impl.client.render.araya.ArayaSceneCopy.isReady()) {
            return;
        }
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, org.tdddd.epca.impl.client.render.araya.ArayaSceneCopy.TEXTURE_ID);
        shader.safeGetUniform("LensSpin").set(spin);

        Matrix4f pose = poseStack.last().pose();
        float r = radius * 1.15F;
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        // a quad in the XY plane at the entity origin, uv 0..1 so the shader sees [-1,1]
        buffer.vertex(pose, -r, -r, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(pose, r, -r, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(pose, r, r, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(pose, -r, r, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }
}
