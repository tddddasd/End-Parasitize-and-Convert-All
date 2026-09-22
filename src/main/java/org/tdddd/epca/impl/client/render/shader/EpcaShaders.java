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
 * EPCA 物品渲染 shader 的注册与 uniform 句柄。
 *
 * <h3>shader 文件</h3>
 * <pre>
 * assets/epca/shaders/core/
 *   corruption.json   ← 程序配置（blend / attributes / samplers / uniforms）
 *   corruption.vsh
 *   corruption.fsh
 * </pre>
 *
 * <h3>崩坏 shader uniform</h3>
 * <table>
 *   <tr><th>uniform</th><th>类型</th><th>用途</th></tr>
 *   <tr><td>{@code time}</td><td>float</td><td>动画时间（游戏刻）</td></tr>
 *   <tr><td>{@code intensity}</td><td>float</td><td>崩坏强度 0–1</td></tr>
 *   <tr><td>{@code tint}</td><td>vec3</td><td>崩坏染色</td></tr>
 *   <tr><td>{@code splitStrength}</td><td>float</td><td>RGB 色散倍率</td></tr>
 * </table>
 *
 * <p>注意：GLSL 编译器会优化掉未使用的 uniform，那时 {@code getUniform()} 返回
 * {@code null}，因此所有设置处都必须做空判断。</p>
 */
public final class EpcaShaders {

    public static ShaderInstance corruptionShader;

    public static Uniform corruptionTimeUniform;
    public static Uniform corruptionIntensityUniform;
    public static Uniform corruptionTintUniform;
    public static Uniform corruptionSplitUniform;

    /** 崩坏层的三套 RenderType（即时 / 延迟非手持 / 延迟手持）。 */
    public static final ItemShaderRenderTypes CORRUPTION_TYPES =
            ItemShaderRenderTypes.create("corruption", () -> corruptionShader, true);

    private EpcaShaders() {
    }

    /**
     * 注册全部 shader。由 {@link org.tdddd.epca.impl.client.render.EpcaRenderClient}
     * 挂到 mod 事件总线上的 {@link RegisterShadersEvent} 调用。
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

    /** 资源重载时清掉烘焙缓存，避免持有旧图集的 sprite。 */
    public static void invalidateCaches() {
        org.tdddd.epca.impl.client.render.ItemShaderBakery.invalidate();
    }
}
