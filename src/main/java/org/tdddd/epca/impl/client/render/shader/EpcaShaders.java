package org.tdddd.epca.impl.client.render.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.tdddd.epca.impl.client.render.ItemShaderRenderTypes;
import org.tdddd.epca.impl.epca;

import java.io.IOException;

/**
 * EPCA  shader  uniform
 *
 * <h3>shader </h3>
 * <pre>
 * assets/epca/shaders/core/
 *   corruption.json    blend / attributes / samplers / uniforms
 *   corruption.vsh
 *   corruption.fsh
 * </pre>
 *
 * <h3> shader uniform</h3>
 * <table>
 *   <tr><th>uniform</th><th></th><th></th></tr>
 *   <tr><td>{@code time}</td><td>float</td><td></td></tr>
 *   <tr><td>{@code intensity}</td><td>float</td><td> 01</td></tr>
 *   <tr><td>{@code tint}</td><td>vec3</td><td></td></tr>
 *   <tr><td>{@code splitStrength}</td><td>float</td><td>RGB </td></tr>
 * </table>
 *
 * <p>GLSL  uniform {@code getUniform()}
 * {@code null}</p>
 */
public final class EpcaShaders {

    public static ShaderInstance corruptionShader;

    public static Uniform corruptionTimeUniform;
    public static Uniform corruptionIntensityUniform;
    public static Uniform corruptionTintUniform;
    public static Uniform corruptionSplitUniform;

    /**  RenderType /  /  */
    public static final ItemShaderRenderTypes CORRUPTION_TYPES =
            ItemShaderRenderTypes.create("corruption", () -> corruptionShader, true);

    private EpcaShaders() {
    }

    /**
     *  shader {@link org.tdddd.epca.impl.client.render.EpcaRenderClient}
     *  mod  {@link RegisterShadersEvent}
     */
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

    /**  sprite */
    public static void invalidateCaches() {
        org.tdddd.epca.impl.client.render.ItemShaderBakery.invalidate();
    }
}

