package org.tdddd.epca.impl.client.render.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.tdddd.epca.impl.client.render.ItemShaderRenderTypes;
import org.tdddd.epca.impl.epca;

import java.io.IOException;

public final class EpcaShaders {

    public static ShaderInstance corruptionShader;

    public static Uniform corruptionTimeUniform;
    public static Uniform corruptionIntensityUniform;
    public static Uniform corruptionTintUniform;
    public static Uniform corruptionSplitUniform;

    public static final ItemShaderRenderTypes CORRUPTION_TYPES =
            ItemShaderRenderTypes.create("corruption", () -> corruptionShader, true);

    private EpcaShaders() {
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            new ResourceLocation(epca.MODID, "corruption"),
                            DefaultVertexFormat.BLOCK),
                    shader -> {
                        corruptionShader = shader;
                        corruptionTimeUniform = shader.getUniform("time");
                        corruptionIntensityUniform = shader.getUniform("intensity");
                        corruptionTintUniform = shader.getUniform("tint");
                        corruptionSplitUniform = shader.getUniform("splitStrength");
                        epca.LOGGER.info("[epca-render] 崩坏着色器已加载 (intensity={}, tint={}, splitStrength={})",
                                corruptionIntensityUniform != null,
                                corruptionTintUniform != null,
                                corruptionSplitUniform != null);
                    }
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "EPCA: 加载崩坏着色器失败，请检查 assets/" + epca.MODID + "/shaders/core/corruption.*", e);
        }
    }

    public static void invalidateCaches() {
        org.tdddd.epca.impl.client.render.ItemShaderBakery.invalidate();
    }
}
