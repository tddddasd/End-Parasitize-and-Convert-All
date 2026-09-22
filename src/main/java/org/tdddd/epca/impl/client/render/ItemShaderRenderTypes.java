package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.tdddd.epca.impl.epca;

import java.util.function.Supplier;

/**
 * 一个物品 shader 层所需的 <b>三套 RenderType 变体</b>，是从
 * RottenRuinsSplendiding 的 {@code CosmicRenderType} 中抽取出来的通用结构。
 *
 * <h3>为什么必须是三种变体</h3>
 * <table>
 *   <tr><th>变体</th><th>深度测试</th><th>输出目标</th><th>写入掩码</th><th>用途</th></tr>
 *   <tr><td>{@link #immediate()}</td><td>EQUAL</td><td>默认</td><td>默认</td>
 *       <td>无光影 / GUI：只画在基础物品已写入深度的像素上，精确贴合轮廓</td></tr>
 *   <tr><td>{@link #afterLevel()}</td><td>LEQUAL + polygonOffset</td><td>MAIN_TARGET</td><td>COLOR_WRITE</td>
 *       <td>光影激活时的延迟回放（非第一人称）</td></tr>
 *   <tr><td>{@link #handAfterLevel()}</td><td>NO_DEPTH_TEST</td><td>MAIN_TARGET</td><td>COLOR_WRITE</td>
 *       <td>光影激活时的延迟回放（第一人称手持）</td></tr>
 * </table>
 *
 * <ul>
 *   <li><b>即时用 EQUAL</b>：基础物品的深度已经写入，用 {@code =} 精确匹配轮廓。</li>
 *   <li><b>延迟非手持用 LEQUAL</b>：回放时基础物品的深度是更早的 pass 写的，
 *       在部分光影管线里深度值有微小差异，必须放宽到 {@code <=}。</li>
 *   <li><b>延迟手持用 NO_DEPTH_TEST</b>：很多光影下第一人称手不写主深度缓冲。</li>
 *   <li><b>MAIN_TARGET + COLOR_WRITE</b>：强制写主 framebuffer 绕过光影 GBuffer，
 *       且不污染深度，避免影响光影的 composite pass。</li>
 *   <li><b>polygonOffset</b>：避免与物品原表面 z-fighting。</li>
 * </ul>
 *
 * <p>本类是纯静态工厂 + 数据持有者。它继承 {@link RenderType}（即
 * {@link RenderStateShard} 的子类）是为了取得 {@code protected} 的
 * {@code EQUAL_DEPTH_TEST / MAIN_TARGET / COLOR_WRITE ...} 等状态常量——
 * 这与参考项目的 {@code CosmicRenderType extends RenderType} 做法一致。</p>
 *
 * <p>新增一个 shader 层时，只需调用一次
 * {@link #create(String, Supplier, boolean)} 拿到三套 RenderType 即可，
 * 不必再手写 RenderType 代码。</p>
 */
public abstract class ItemShaderRenderTypes extends RenderType {

    /** 推送几何体一点点靠近相机，避免回放时与原表面 z-fighting。 */
    private static final RenderStateShard.LayeringStateShard LAYER_DEPTH_BIAS =
            new RenderStateShard.LayeringStateShard(
                    "epca_item_layer_depth_bias",
                    () -> {
                        RenderSystem.polygonOffset(-1.0F, -32.0F);
                        RenderSystem.enablePolygonOffset();
                    },
                    () -> {
                        RenderSystem.polygonOffset(0.0F, 0.0F);
                        RenderSystem.disablePolygonOffset();
                    }
            );

    private static final int BUFFER_SIZE = 2097152;

    private final RenderType immediate;
    private final RenderType afterLevel;
    private final RenderType handAfterLevel;

    private ItemShaderRenderTypes(String name, Supplier<ShaderInstance> shader, boolean affectsCrumbling) {
        // 这个父类实例本身不会被使用，只是为了让内部类能访问 RenderStateShard 的 protected 常量。
        super(epca.MODID + ":item_layer/" + name + "_holder",
                DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, BUFFER_SIZE,
                false, false, () -> {
                }, () -> {
                });

        RenderStateShard.ShaderStateShard shaderState = new RenderStateShard.ShaderStateShard(shader);

        this.immediate = RenderType.create(
                epca.MODID + ":item_layer/" + name,
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                affectsCrumbling,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(EQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .createCompositeState(true)
        );

        this.afterLevel = RenderType.create(
                epca.MODID + ":item_layer/" + name + "_after_level",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .setLayeringState(LAYER_DEPTH_BIAS)
                        .setOutputState(MAIN_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );

        this.handAfterLevel = RenderType.create(
                epca.MODID + ":item_layer/" + name + "_hand_after_level",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .setOutputState(MAIN_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
    }

    /**
     * 为某个 shader 层创建三套 RenderType 变体。
     *
     * @param name            层名（需唯一，用于 RenderType 名称，例如 {@code "corruption"}）
     * @param shader          shader 实例供给器；允许返回 {@code null}（尚未加载完成时）
     * @param affectsCrumbling 即时变体是否参与方块破坏进度批处理；对物品层一般传 {@code true}
     */
    public static ItemShaderRenderTypes create(String name, Supplier<ShaderInstance> shader, boolean affectsCrumbling) {
        return new ItemShaderRenderTypes(name, shader, affectsCrumbling) {
        };
    }

    /** 即时渲染使用的 RenderType（EQUAL 深度）。 */
    public RenderType immediate() {
        return immediate;
    }

    /** 延迟回放使用的 RenderType（LEQUAL 深度 + 主 framebuffer 输出）。 */
    public RenderType afterLevel() {
        return afterLevel;
    }

    /** 第一人称延迟回放使用的 RenderType（NO_DEPTH_TEST + 主 framebuffer 输出）。 */
    public RenderType handAfterLevel() {
        return handAfterLevel;
    }

    /**
     * 延迟回放时按渲染上下文选择变体。
     */
    public RenderType lateFor(ItemDisplayContext context) {
        return isFirstPersonHand(context) ? handAfterLevel : afterLevel;
    }

    /**
     * 是否属于第一人称手持上下文。
     */
    public static boolean isFirstPersonHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    /** 便捷方法：把 {@code name} 转成 {@code epca:...} 的 ResourceLocation。 */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(epca.MODID, path);
    }
}
