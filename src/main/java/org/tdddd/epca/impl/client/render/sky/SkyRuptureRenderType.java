package org.tdddd.epca.impl.client.render.sky;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.tdddd.epca.impl.epca;

/**
 * 世界结界破损（天空盒碎裂）用的全屏 RenderType。
 *
 * <h3>它为什么看起来是"贴在天空上"而不是"盖在屏幕上"</h3>
 * 顶点着色器把四边形放在 NDC 的最远平面上（{@code z = 1}），
 * 深度测试用 <b>{@code LEQUAL}</b>，于是：
 * <ul>
 *   <li>深度缓冲仍是清空值（{@code 1.0}）的像素 —— 也就是"天空" —— 通过测试</li>
 *   <li>地形、实体、云已经写了更近的深度 —— 全部不通过，自动把裂缝挡掉</li>
 * </ul>
 * 结果是裂缝只出现在真正能看到天的地方：站在山谷里抬头，只有露出来的那片天是裂的。
 * 再配合着色器里按<b>世界方向</b>算图案，转动视角时裂缝会像天空本身一样平移。
 *
 * <h3>配方</h3>
 * <table>
 *   <tr><th>状态</th><th>设置</th><th>原因</th></tr>
 *   <tr><td>深度测试</td><td>{@code LEQUAL_DEPTH_TEST}</td>
 *       <td>用它来"只画天空像素"（见上）；同时这是<b>光影下唯一可靠的天空遮罩</b>，
 *           因为 Oculus/Iris 的 composite 之后主 framebuffer 的深度仍然保存着世界深度</td></tr>
 *   <tr><td>深度写入</td><td>{@code COLOR_WRITE}</td>
 *       <td>只写颜色，不污染深度（否则会影响第一人称手与光影 composite 的深度判定）</td></tr>
 *   <tr><td>输出目标</td><td>{@code MAIN_TARGET}</td>
 *       <td>直写主 framebuffer：<b>光影激活时绕过 GBuffer</b></td></tr>
 *   <tr><td>背面剔除</td><td>{@code NO_CULL}</td><td>全屏四边形不关心绕序</td></tr>
 *   <tr><td>混合</td><td>{@code TRANSLUCENT_TRANSPARENCY}</td>
 *       <td>普通 alpha 混合：结界完好的地方让原版天空透出来</td></tr>
 *   <tr><td>纹理</td><td>方块图集（含 12 张星点 sprite）</td>
 *       <td>星场直接采样图集里的 {@code epca:shader/cosmic_0..11} —— 与参考项目
 *           RottenRuinsSplendiding 的宇宙渲染同一套星点粒子。双线性、不开 mipmap</td></tr>
 *   <tr><td>顶点格式</td><td>{@code POSITION}</td><td>顶点即 NDC，不需要 UV/矩阵</td></tr>
 * </table>
 *
 * <p>本类是纯静态持有者，继承 {@link RenderType} 只是为了拿到
 * {@code protected} 的 {@code LEQUAL_DEPTH_TEST / MAIN_TARGET / COLOR_WRITE ...}
 * 等状态常量（与 ItemShaderRenderTypes 同一手法）。</p>
 */
public abstract class SkyRuptureRenderType extends RenderType {

    /** 全屏结界破损叠层（靠深度缓冲遮罩到天空）。 */
    public static final RenderType SKY_RUPTURE = RenderType.create(
            epca.MODID + ":sky_rupture",
            DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> SkyRuptureShaders.skyRuptureShader))
                    // 方块图集 + 双线性过滤：12 张星点 sprite 就在这张图集里
                    // （与 BLOCK_SHEET 同一张图，只是关掉 mipmap、打开线性过滤，星星更柔）
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, true, false))
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    /** 本类不会被实例化，这个构造器只是为了让编译通过（父类没有无参构造器）。 */
    private SkyRuptureRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                 boolean affectsCrumbling, boolean sortOnUpload,
                                 Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
