package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.tdddd.epca.impl.epca;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「残影」（末影人 / 头颅 / 末影螨共用，见
 * {@link org.tdddd.epca.impl.client.entity.layer.EndermanAfterimageLayer}）专用的
 * RenderPipeline + RenderType。
 *
 * <h2>为什么需要自定义 pipeline</h2>
 * <p>1.20.1 的根因是 {@code RenderType.entityTranslucent(...)} 的 writeMask 为
 * {@code COLOR_DEPTH_WRITE}：残影会写深度，于是<b>挡在后面的实体 / 粒子在深度测试里被剔除</b>，
 * 看起来像"残影把后面的实体遮住了"。</p>
 *
 * <p>26.1.2 的等价物是 {@link RenderPipelines#ENTITY_TRANSLUCENT}，而它用的是
 * {@link DepthStencilState#DEFAULT}（{@code depthTest=LEQUAL, writeDepth=true}），
 * 与 1.20.1 的 {@code COLOR_DEPTH_WRITE} 是同一个毛病。26 的渲染已经完全
 * RenderPipeline 化（{@code RenderType} 只是 pipeline + 贴图 + 若干 flag 的集合，
 * 深度写入由 pipeline 的 {@code DepthStencilState} 决定），
 * 也没有任何现成的 "entity + 半透明 + 不写深度" pipeline
 * （{@code ENTITY_TRANSLUCENT_EMISSIVE} 虽然不写深度，但它是 EMISSIVE/全亮语义，
 * 会丢掉光照和 overlay，不能复用）。所以这里复制 {@code ENTITY_TRANSLUCENT} 的构成，
 * 只把 {@code DepthStencilState} 换成 {@code writeDepth = false}。</p>
 *
 * <p>与 1.20.1 的 {@code AfterimageRenderType} 逐项对应：</p>
 * <ul>
 *   <li>{@code RENDERTYPE_ENTITY_TRANSLUCENT_SHADER} -&gt; {@code ENTITY_SNIPPET}（core/entity 着色器、
 *       {@code DefaultVertexFormat.ENTITY}、QUADS）；</li>
 *   <li>{@code TRANSLUCENT_TRANSPARENCY} -&gt; {@code ColorTargetState(BlendFunction.TRANSLUCENT)}；</li>
 *   <li>{@code LEQUAL_DEPTH_TEST} -&gt; {@code CompareOp.LESS_THAN_OR_EQUAL}；</li>
 *   <li>{@code NO_CULL} -&gt; {@code withCull(false)}；</li>
 *   <li>{@code LIGHTMAP} / {@code OVERLAY} -&gt; {@code useLightmap()} / {@code useOverlay()}；</li>
 *   <li><b>{@code setWriteMaskState(COLOR_WRITE)} -&gt; {@code writeDepth = false}</b>（关键）；</li>
 *   <li>{@code sortOnUpload = true} -&gt; {@code sortOnUpload()}。</li>
 * </ul>
 */
public final class AfterimagePipelines {

    private static final Identifier PIPELINE_ID =
            Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/afterimage");

    /**
     * 与 {@link RenderPipelines#ENTITY_TRANSLUCENT} 等价，但 {@code writeDepth = false}
     * ——这是"能透过残影看到后面实体"的关键。
     */
    public static final RenderPipeline AFTERIMAGE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    // ENTITY_SNIPPET 已带 Sampler0(贴图) / Sampler2(lightmap)，这里补 Sampler1(overlay)
                    .withSampler("Sampler1")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    // 不写深度：残影不会把后面的实体挡住（等价于 1.20.1 的 COLOR_WRITE writeMask）
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build();

    /** RenderType 缓存：按贴图复用，避免每帧重建（对应 1.20.1 的 RENDER_TYPES 缓存）。 */
    private static final Map<Identifier, RenderType> RENDER_TYPES = new ConcurrentHashMap<>();

    private static final String RENDER_TYPE_NAME_PREFIX = "epca_afterimage/";

    private AfterimagePipelines() {
    }

    /** 在 {@code RegisterRenderPipelinesEvent}（mod 总线，客户端）里注册。 */
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(AFTERIMAGE_PIPELINE);
        DepthStencilState depth = AFTERIMAGE_PIPELINE.getDepthStencilState();
        epca.LOGGER.info("[epca-render] afterimage pipeline registered: {} shader={} depth={} writeDepth={} blend={}",
                AFTERIMAGE_PIPELINE.getLocation(),
                AFTERIMAGE_PIPELINE.getVertexShader(),
                depth == null ? null : depth.depthTest(),
                depth != null && depth.writeDepth(),
                AFTERIMAGE_PIPELINE.getColorTargetState().blendFunction().orElse(null));
    }

    /** 残影专用 RenderType（按贴图缓存）。 */
    public static RenderType renderTypeFor(Identifier texture) {
        return RENDER_TYPES.computeIfAbsent(texture, tex -> RenderType.create(
                RENDER_TYPE_NAME_PREFIX + tex,
                RenderSetup.builder(AFTERIMAGE_PIPELINE)
                        .withTexture("Sampler0", tex)
                        .useLightmap()
                        .useOverlay()
                        .sortOnUpload()
                        .setOutline(RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup()));
    }

    public static int renderTypeCount() {
        return RENDER_TYPES.size();
    }
}
