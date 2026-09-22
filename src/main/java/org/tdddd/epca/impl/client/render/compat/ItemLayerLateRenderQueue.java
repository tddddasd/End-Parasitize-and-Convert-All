package org.tdddd.epca.impl.client.render.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.tdddd.epca.impl.client.render.ItemLayerBinding;
import org.tdddd.epca.impl.client.render.ItemShaderRenderHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 光影兼容的延迟渲染队列。
 *
 * <h3>为什么要有队列</h3>
 * 见 {@link IrisShaderCompat} 的说明：光影激活时直接绘制会被吸进 GBuffer 然后丢失效果。
 * 这里的做法与 RottenRuinsSplendiding 的 {@code CosmicItemLateRenderQueue} 一致：
 *
 * <ol>
 *   <li>渲染物品时<b>不</b>立即画层，而是把完整渲染状态快照入队
 *       （PoseStack 矩阵 + 法线矩阵 + ModelView + Projection + 物品栈副本 + 光照）</li>
 *   <li>{@code GameRenderer.renderLevel()} 里两个注入点回放：
 *     <ul>
 *       <li><b>renderHand 之前</b>（{@link #renderNonFirstPerson()}）：
 *           回放世界中的物品（地面 / 物品展示框 / 第三视角），
 *           此时主场景深度已写入，用手持以外的 LEQUAL 变体</li>
 *       <li><b>renderLevel 结尾</b>（{@link #renderAll()}）：
 *           回放第一人称手持（很多光影下手不写主深度缓冲，用 NO_DEPTH_TEST 变体）</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>关键细节</h3>
 * <ul>
 *   <li>回放用 {@code mc.renderBuffers().bufferSource()} <b>原版</b> buffer source，
 *       而不是各个调用方传入的（后者可能已被光影包裹）</li>
 *   <li>逐条目独立 flush —— 不同物品的 uniform 不同，不能合批</li>
 *   <li>矩阵必须在入队时快照，回放时 PoseStack 早已不是当时的状态</li>
 * </ul>
 */
public final class ItemLayerLateRenderQueue {

    private record Entry(ItemStack stack,
                         ItemDisplayContext context,
                         List<ItemLayerBinding> bindings,
                         Matrix4f pose,
                         Matrix3f normal,
                         Matrix4f modelView,
                         Matrix4f projection,
                         int packedLight,
                         int packedOverlay) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private ItemLayerLateRenderQueue() {
    }

    /** 是否已经入队。 */
    public static boolean isEmpty() {
        return ENTRIES.isEmpty();
    }

    /** 清空队列（世界切换 / 出错时兜底）。 */
    public static void clear() {
        ENTRIES.clear();
    }

    /**
     * 快照一次延迟渲染。必须在渲染线程、且当前 ModelView/Projection 正确时调用。
     */
    public static void enqueue(ItemStack stack, ItemDisplayContext context, List<ItemLayerBinding> bindings,
                               PoseStack poseStack, int packedLight, int packedOverlay) {
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        ENTRIES.add(new Entry(
                stack.copy(),
                context,
                List.copyOf(bindings),
                new Matrix4f(pose.pose()),
                new Matrix3f(pose.normal()),
                new Matrix4f(RenderSystem.getModelViewMatrix()),
                new Matrix4f(RenderSystem.getProjectionMatrix()),
                packedLight,
                packedOverlay
        ));
    }

    /** 阶段 1：在第一人称手渲染之前回放世界空间条目。 */
    public static void renderNonFirstPerson() {
        replay(false);
    }

    /** 阶段 2：在 renderLevel 结束时回放剩余条目（含第一人称手）。 */
    public static void renderAll() {
        replay(true);
    }

    private static void replay(boolean includeFirstPersonHand) {
        if (ENTRIES.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ENTRIES.clear();
            return;
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();

        List<Entry> pending = includeFirstPersonHand ? List.of() : new ArrayList<>();

        try {
            LateRenderState.prepareMainTargetPass();

            for (Entry entry : ENTRIES) {
                if (!includeFirstPersonHand && isFirstPersonHand(entry.context())) {
                    pending.add(entry);
                    continue;
                }
                drawEntry(entry, buffers);
            }
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.DISTANCE_TO_ORIGIN);
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
            LateRenderState.finishMainTargetPass();

            ENTRIES.clear();
            if (!pending.isEmpty()) {
                ENTRIES.addAll(pending);
            }
        }
    }

    private static void drawEntry(Entry entry, MultiBufferSource.BufferSource buffers) {
        // 还原入队时的矩阵状态
        RenderSystem.getModelViewStack().last().pose().set(entry.modelView());
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(entry.projection()), VertexSorting.DISTANCE_TO_ORIGIN);

        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(entry.pose());
        poseStack.last().normal().set(entry.normal());

        for (ItemLayerBinding binding : entry.bindings()) {
            ItemShaderRenderHelper.drawBinding(binding, entry.stack(), entry.context(), poseStack, buffers,
                    entry.packedLight(), entry.packedOverlay(), true);

            // 逐条目、逐层独立 flush
            buffers.endBatch(binding.layer().renderTypes().afterLevel());
            buffers.endBatch(binding.layer().renderTypes().handAfterLevel());
        }
    }

    private static boolean isFirstPersonHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }
}
