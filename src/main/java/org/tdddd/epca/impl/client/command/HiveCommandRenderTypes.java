package org.tdddd.epca.impl.client.command;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 指挥系统用的两个 RenderType。
 *
 * <h2>0.148n 之后：直接用原版 RenderType，不再自定义 pipeline</h2>
 * <p>旧实现以 {@code RenderPipelines.LINES_SNIPPET} / {@code DEBUG_FILLED_SNIPPET} 为基底
 * 自己造了两条“深度测试为空”的 pipeline，想复刻 1.20.1 的
 * {@code RenderSystem.disableDepthTest()}（透墙可见），再
 * {@code RenderType.create(...)} 包一层。那条路在 26.1.2 里是纯风险：</p>
 * <ul>
 *     <li>自建 pipeline 要经过 {@code RegisterRenderPipelinesEvent} 注册、启动期编译，
 *     一旦编译失败 {@code GlCommandEncoder} 只会 <b>静默</b> {@code return false}
 *     （{@code pipeline.program() == GlProgram.INVALID_PROGRAM}），屏幕上什么都不出现，
 *     而且在正常日志级别下不一定看得到报错；</li>
 *     <li>顶点格式/拓扑要跟 pipeline 严格对齐，改一个字就是“画了但屏幕上没有”；</li>
 *     <li>“透墙”本身并不是需求硬指标，原版被方块遮挡是完全可以接受的行为。</li>
 * </ul>
 *
 * <p>所以现在直接返回原版<b>已经被无数处世界内渲染验证过</b>的两个 RenderType：</p>
 * <ul>
 *     <li>{@link RenderTypes#lines()}：{@code pipeline/lines}，
 *     顶点格式 {@code POSITION_COLOR_NORMAL_LINE_WIDTH}，拓扑 {@code LINES}，
 *     深度测试开启（会被方块遮挡 —— 接受）。线宽由顶点属性 {@code LineWidth} 提供，
 *     渲染器里每个顶点都写 {@code setLineWidth(...)}，不依赖被驱动夹到 1px 的 GL 线宽；</li>
 *     <li>{@link RenderTypes#debugQuads()}：{@code pipeline/debug_quads}，
 *     顶点格式 {@code POSITION_COLOR}，拓扑 {@code QUADS}，{@code withCull(false)}
 *     + {@code TRANSLUCENT} 混合 + 深度测试开启、不写深度。
 *     用来画路线连线（面向镜头的 ribbon 带）和节点填充小方块。</li>
 * </ul>
 *
 * <p>与本模组里已经工作正常的 {@code InfestedRaidBarrierRenderer}
 * （同样在 {@code AfterTranslucentBlocks} 用 {@code RenderTypes.debugQuads()} 画球壳）
 * 完全同一套写法，不再有“自定义 pipeline 是否编译成功”这个未知量。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HiveCommandRenderTypes {

    private HiveCommandRenderTypes() {
    }

    /** 线框：原版 {@code pipeline/lines}（会被方块遮挡，可接受）。 */
    public static RenderType lines() {
        return RenderTypes.lines();
    }

    /** 四边形（ribbon 连线 / 节点填充）：原版 {@code pipeline/debug_quads}。 */
    public static RenderType quads() {
        return RenderTypes.debugQuads();
    }
}
