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
 *
 *
 * <h3></h3>
 *  {@link IrisShaderCompat}  GBuffer
 *  RottenRuinsSplendiding  {@code CosmicItemLateRenderQueue}
 *
 * <ol>
 *   <li><b></b>
 *       PoseStack  +  + ModelView + Projection +  + </li>
 *   <li>{@code GameRenderer.renderLevel()}
 *     <ul>
 *       <li><b>renderHand </b>{@link #renderNonFirstPerson()}
 *            /  /
 *            LEQUAL </li>
 *       <li><b>renderLevel </b>{@link #renderAll()}
 *            NO_DEPTH_TEST </li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3></h3>
 * <ul>
 *   <li> {@code mc.renderBuffers().bufferSource()} <b></b> buffer source
 *       </li>
 *   <li> flush   uniform </li>
 *   <li> PoseStack </li>
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

    /**  */
    public static boolean isEmpty() {
        return ENTRIES.isEmpty();
    }

    /**  /  */
    public static void clear() {
        ENTRIES.clear();
    }

    /**
     *  ModelView/Projection
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

    /**  1 */
    public static void renderNonFirstPerson() {
        replay(false);
    }

    /**  2 renderLevel  */
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
        RenderSystem.getModelViewStack().last().pose().set(entry.modelView());
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(entry.projection()), VertexSorting.DISTANCE_TO_ORIGIN);

        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(entry.pose());
        poseStack.last().normal().set(entry.normal());

        for (ItemLayerBinding binding : entry.bindings()) {
            ItemShaderRenderHelper.drawBinding(binding, entry.stack(), entry.context(), poseStack, buffers,
                    entry.packedLight(), entry.packedOverlay(), true);

            //  flush
            buffers.endBatch(binding.layer().renderTypes().afterLevel());
            buffers.endBatch(binding.layer().renderTypes().handAfterLevel());
        }
    }

    private static boolean isFirstPersonHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }
}

