package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.sky.DarknessDevourEffect;

/**
 * 黑暗吞噬（从世界边缘吃到玩家）的注入点。
 *
 * <p>{@code FogRenderer.setupFog} 是唯一往 {@code RenderSystem} 写雾参数的地方，
 * 而 {@code VertexBuffer.drawWithShader}（区块）与 {@code LevelRenderer}
 * 会把那些静态值推进方块/实体着色器。所以在这里的 TAIL 覆盖一次，
 * 本帧后续所有方块渲染就都变成"被黑暗吃掉"的样子 —— 效果是逐方块计算的，
 * 不是屏幕滤镜。</p>
 *
 * <p>用 {@code priority = 1200}：TAIL 注入按优先级顺序追加，
 * 优先级高的后应用，因此我们的值会压过其他模组对雾的改动。</p>
 *
 * <p>目标方法是 {@code static}，所以处理器也必须是 {@code static}。</p>
 */
@Mixin(value = FogRenderer.class, priority = 1200)
public abstract class FogRendererDevourMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void epca$applyDevourFog(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                            boolean thickFog, float partialTick, CallbackInfo ci) {
        DarknessDevourEffect.applyFog(renderDistance);
    }
}
