package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.compat.ItemLayerLateRenderQueue;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureRenderer;

/**
 * 光影兼容的延迟回放注入点 —— 对应 RottenRuinsSplendiding 的
 * {@code CosmicAfterLevelMixin}。
 *
 * <h3>为什么是 {@code GameRenderer.renderLevel()}</h3>
 * 只有等光影管线把 GBuffer composite 成最终画面之后，才有机会<b>直写主
 * framebuffer</b> 而不被光影重新着色。因此我们在这个方法里挑两个时间点：
 *
 * <ol>
 *   <li><b>{@code renderHand} 字段被读取之前</b>（也就是第一人称手渲染前）：
 *       此时主场景（实体、地面物品、物品展示框）已经画完、深度已写入，
 *       回放这些条目用 {@code LEQUAL + polygonOffset} 就能贴合轮廓。</li>
 *   <li><b>{@code renderLevel} 结尾（TAIL）</b>：第一人称手也画完了，
 *       回放手持条目用 {@code NO_DEPTH_TEST}（很多光影下手不写主深度缓冲）。</li>
 * </ol>
 *
 * <p>{@code priority = 500} 让它尽量晚于其他 mixin 的注入点，确保我们看到的
 * 是场景渲染完成后的状态。</p>
 */
@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererShaderLayerMixin {

    /**
     * 每帧推进一次结界破损的进度。
     *
     * <p>放在这里而不是 {@code RenderLevelStageEvent.AFTER_SKY}：光影的 shadow pass
     * 会把 {@code LevelRenderer.renderLevel()} 再跑一遍，AFTER_SKY 也就跟着触发两次，
     * 进度会变成双倍速。{@code GameRenderer.render} 每帧严格一次。</p>
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void epca$updateSkyRupture(float partialTick, long nanoTime, boolean renderLevel,
                                       CallbackInfo ci) {
        SkyRuptureRenderer.update();
    }

    /**
     * 阶段 1：在第一人称手渲染之前回放世界空间条目。
     */
    @Inject(method = "renderLevel",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z",
                    ordinal = 0))
    private void epca$replayItemLayersBeforeHand(float partialTick, long finishTimeNano,
                                                 PoseStack poseStack, CallbackInfo ci) {
        // 世界结界破损（天空盒碎裂）：
        // 正常情况它已经在 AFTER_SKY 阶段（天光之后、地形之前）画完了，
        // 这里只在光影激活时补画 —— 光影的 GBuffer 会覆盖早画的层。
        // 它必须画在物品层之前，保证物品层压在裂缝之上。
        SkyRuptureRenderer.renderDeferred();

        if (IrisShaderCompat.isShaderPackActive()) {
            ItemLayerLateRenderQueue.renderNonFirstPerson();
        } else {
            // 光影被中途关掉：清掉残留条目，避免它们在别的画面里冒出来
            ItemLayerLateRenderQueue.clear();
        }
    }

    /**
     * 阶段 2：整个 renderLevel 结束后回放剩余条目（含第一人称手持）。
     */
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void epca$replayItemLayersAfterLevel(float partialTick, long finishTimeNano,
                                                 PoseStack poseStack, CallbackInfo ci) {
        if (IrisShaderCompat.isShaderPackActive()) {
            ItemLayerLateRenderQueue.renderAll();
        } else {
            ItemLayerLateRenderQueue.clear();
        }
    }
}
