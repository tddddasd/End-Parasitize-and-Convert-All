package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 一个可挂到物品上的 shader 渲染层。
 *
 * <h3>这是“统一渲染”体系的扩展点</h3>
 * 实现本接口 + 注册 shader 文件，就能得到一套完整的（含光影兼容的）
 * 物品附加渲染层，无需再碰 Mixin。参考项目
 * RottenRuinsSplendiding 里 cosmic / corruption 各自手写一套 RenderType，
 * 这里把它们抽成了这个统一接口。
 *
 * <h3>新增一个层的最小步骤</h3>
 * <ol>
 *   <li>写 shader：{@code assets/epca/shaders/core/<name>.json|vsh|fsh}</li>
 *   <li>在 {@link org.tdddd.epca.impl.client.render.shader.EpcaShaders} 里注册
 *       {@link ShaderInstance} 并把 uniform 引用存下来，
 *       然后用 {@link ItemShaderRenderTypes#create} 建三套 RenderType；
 *       （或者直接在 {@link ItemShaderLayers} 里 {@code register(new MyLayer())}）</li>
 *   <li>实现本接口，在 {@link #prepare} 里设置 uniform</li>
 *   <li>用 {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry#attach} 把层挂到指定物品上</li>
 * </ol>
 *
 * <h3>渲染契约</h3>
 * 调用方（Mixin）保证：基础物品模型 <b>已经画完并 flush</b>，深度已写入。
 * 因此本层的几何体可以直接使用 EQUAL 深度测试贴合物品轮廓。
 * 光影激活时改由延迟队列在 {@code GameRenderer.renderLevel()} 之后回放，
 * 实现细节见 {@link org.tdddd.epca.impl.client.render.compat.ItemLayerLateRenderQueue}。
 */
public interface IItemShaderLayer {

    /** 层名，需唯一（例如 {@code "corruption"}）。 */
    String name();

    /** 默认 id：{@code epca:item_layer/<name>}。 */
    default ResourceLocation id() {
        return new ResourceLocation("epca", "item_layer/" + name());
    }

    /**
     * 当前 shader 实例。shader 尚未加载完成时必须返回 {@code null}
     * （调用方会跳过绘制，不会崩）。
     */
    ShaderInstance shader();

    /** 该层使用的三套 RenderType 变体。 */
    ItemShaderRenderTypes renderTypes();

    /**
     * 是否支持“光影激活时延迟回放”。
     * 返回 {@code false} 的层在光影下会被直接跳过（而不是画进 GBuffer 里丢失效果）。
     */
    default boolean supportsDeferredReplay() {
        return true;
    }

    /**
     * 该物品应当使用哪张遮罩贴图。
     *
     * <p>缺省约定：用物品自己的贴图
     * （{@code assets/<namespace>/textures/item/<path>.png}）——
     * 这正好是 1:1 的物品轮廓，崩坏层因此天然贴合物品。</p>
     *
     * @param config 该绑定的配置，可用 {@link ItemLayerConfig#maskOverride()} 覆盖
     */
    ResourceLocation maskTexture(ItemStack stack, ItemLayerConfig config);

    /**
     * 设置 uniform 并决定这次是否真的要绘制。
     *
     * @param stack      被渲染的物品
     * @param config     该绑定的配置（{@link ItemLayerConfig#strength()} 是通用强度倍率）
     * @param ctx        渲染上下文
     * @param lateRender {@code true} 表示这是光影延迟回放（矩阵/深度状态与即时路径不同）
     * @return {@code false} 表示本次跳过绘制
     */
    boolean prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx, boolean lateRender);

    /**
     * 是否需要给它叠加“鬼畜抖动”的 PoseStack 变换（几何层面的崩坏）。
     * 缺省不启用；崩坏层通过 {@link ItemLayerConfig#twitch()} 开启。
     */
    default boolean usesTwitchTransform(ItemStack stack, ItemLayerConfig config) {
        return config.twitch();
    }

    /**
     * 应用鬼畜抖动变换。仅当 {@link #usesTwitchTransform} 为真时被调用。
     */
    default void applyTwitch(PoseStack poseStack, ItemStack stack, ItemLayerConfig config, long gameTime) {
    }
}
