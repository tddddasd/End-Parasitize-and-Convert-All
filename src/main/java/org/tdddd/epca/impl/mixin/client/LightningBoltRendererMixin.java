package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.PurpleLightningGeometry;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.utils.IPurpleLightningBolt;

/**
 * 26.1.2 版的紫色闪电（虫染闪电）。
 *
 * <p><b>1.20.1 原版行为。</b> {@code LightningBoltRenderer#render(...)} 开头判断闪电脚下的方块是否是
 * {@link InfestedBlockInterface}，把结果放进 {@code ThreadLocal<Boolean> IS_PURPLE}；
 * 随后 {@code @Redirect} 私有静态方法 {@code quad(...)}，在 {@code IS_PURPLE} 为真时把它的
 * RGB 参数从原版常量 {@code (0.45F, 0.45F, 0.5F)} 改成 {@code (0.8F, 0.2F, 1.0F)}。
 * alpha 是 {@code quad} 内部的 {@code 0.3F}，从未改动；几何体完全不变。
 *
 * <p><b>为什么旧的注入点不存在了。</b> 26.1.2 没有 {@code render} 方法：
 * <ul>
 *   <li>{@code extractRenderState(LightningBolt, LightningBoltRenderState, float)} 只记录
 *       {@code LightningBolt.seed}；</li>
 *   <li>{@code submit(LightningBoltRenderState, PoseStack, SubmitNodeCollector, CameraRenderState)}
 *       算出 8 段偏移，然后 {@code submitCustomGeometry(...)} 里挂一个 lambda，真正的
 *       {@code quad} 调用在合成方法 {@code lambda$submit$0} 内。{@code submit} 的方法体里只有一条
 *       {@code invokedynamic}，没有 {@code quad} 调用点，所以 {@code method = "submit"} +
 *       {@code @Redirect quad} 找不到注入点。</li>
 * </ul>
 *
 * <p><b>本实现的注入点（两处）。</b>
 * <ol>
 *   <li>{@code extractRenderState} 的 HEAD：这里还拿得到 {@code LightningBolt}，虫染判定与 1.20.1
 *       逐字一致，结果写进 render state（{@link IPurpleLightningBolt}，由
 *       {@link LightningBoltRenderStateMixin} 实现）。<b>不用 {@code ThreadLocal}</b>：
 *       26.1.2 是「先抽取全部实体、再统一提交」，{@code ThreadLocal} 会被下一只实体的抽取覆盖。</li>
 *   <li>{@code submit} 里那次 {@code submitCustomGeometry(poseStack, RenderTypes.lightning(), lambda)}
 *       的 {@code @Redirect}：非紫色时<b>原样调用传进来的原版 {@code CustomGeometryRenderer}</b>
 *       （就是 {@code lambda$submit_0} 绑定的实例，逐字节等于原版）；紫色时换成
 *       {@link PurpleLightningGeometry}，它逐行照抄原版 lambda + {@code quad}，只把颜色参数化。</li>
 * </ol>
 *
 * <p><b>为什么不再崩。</b> 上一版把 {@code state} 强转成 {@code LightningBoltRenderStateMixin}，
 * 只要 mixin 的方法体里出现另一个 mixin 类的类型引用，Mixin 的
 * {@code MixinTargetContext#remapClassName} 就会走 {@code ClassInfo.isMixin(name) &&
 * !isLoadable()} 分支去 {@code findRealType}，而 {@code ClassInfo.findCorrespondingType} 只沿
 * {@code LightningBoltRenderer} 的继承体系找 {@code LightningBoltRenderState}（找不到）→
 * {@code InvalidMixinException: Resolution error: unable to find corresponding type ...}。
 * 现在跨 mixin 共享的是普通接口 {@link IPurpleLightningBolt}（非 mixin，Mixin 原样保留），
 * 本文件里没有任何对其他 mixin 类的类型引用。
 *
 * <p>目标方法与描述符均用 {@code javap} 核对过（{@code minecraft-patched-26.1.2.76.jar}）：
 * <pre>
 * public void submit(net.minecraft.client.renderer.entity.state.LightningBoltRenderState,
 *                    com.mojang.blaze3d.vertex.PoseStack,
 *                    net.minecraft.client.renderer.SubmitNodeCollector,
 *                    net.minecraft.client.renderer.state.level.CameraRenderState);
 *   descriptor: (Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V
 * public void extractRenderState(net.minecraft.world.entity.LightningBolt,
 *                    net.minecraft.client.renderer.entity.state.LightningBoltRenderState, float);
 *   descriptor: (Lnet/minecraft/world/entity/LightningBolt;Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;F)V
 * public abstract void net.minecraft.client.renderer.OrderedSubmitNodeCollector.submitCustomGeometry(
 *                    com.mojang.blaze3d.vertex.PoseStack,
 *                    net.minecraft.client.renderer.rendertype.RenderType,
 *                    net.minecraft.client.renderer.SubmitNodeCollector$CustomGeometryRenderer);
 *   descriptor: (Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V
 * </pre>
 * 两个方法名都是重载（{@code extractRenderState} / {@code submit} 各有 {@code Entity*} 版本），
 * 因此注解里一律写完整描述符，避免注入到错误的重载上。
 * 注意 {@code submitCustomGeometry} 声明在父接口 {@code OrderedSubmitNodeCollector} 上，
 * 但 {@code submit} 的字节码里 {@code invokeinterface} 的 owner 就是
 * {@code net/minecraft/client/renderer/SubmitNodeCollector}（{@code // InterfaceMethod
 * net/minecraft/client/renderer/SubmitNodeCollector.submitCustomGeometry:(...)V}），
 * {@code @At.target} 必须按字节码里的 owner 写。
 */
@Mixin(LightningBoltRenderer.class)
public class LightningBoltRendererMixin {

    /**
     * 抽取趟：判定 + 写标记。
     * 描述符消歧（同名重载 {@code extractRenderState(Entity, EntityRenderState, float)}）。
     */
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LightningBolt;Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;F)V",
            at = @At("HEAD")
    )
    private void epca$markPurpleBolt(LightningBolt entity, LightningBoltRenderState state, float partialTick,
                                     CallbackInfo ci) {
        boolean purple = false;
        if (entity != null && state != null) {
            Level level = entity.level();
            if (level != null) {
                // 1.20.1 原版判定：脚下（blockPosition().below()）是 InfestedBlockInterface。
                BlockPos below = entity.blockPosition().below();
                purple = level.getBlockState(below).getBlock() instanceof InfestedBlockInterface;
            }
        }
        ((IPurpleLightningBolt) (Object) state).epca$setPurpleBolt(purple);
    }

    /**
     * 提交趟：换掉 {@code submitCustomGeometry} 的几何体回调。
     *
     * <p>处理器参数顺序 = 被重定向调用的（接收者 + 参数）…，再接上目标方法 {@code submit} 的全部参数
     * （按声明顺序，全部列出是 {@code @Redirect} 捕获外层参数唯一无歧义的写法）：
     * {@code state, poseStack, collector, camera}。
     */
    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"
            )
    )
    private void epca$submitBolt(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
                                 SubmitNodeCollector.CustomGeometryRenderer vanillaGeometry,
                                 LightningBoltRenderState state, PoseStack submitPoseStack,
                                 SubmitNodeCollector submitCollector, CameraRenderState camera) {
        if (!((IPurpleLightningBolt) (Object) state).epca$isPurpleBolt()) {
            // 非虫染闪电：把原版回调原样交回去，几何体与颜色逐字节等于原版。
            collector.submitCustomGeometry(poseStack, renderType, vanillaGeometry);
            return;
        }
        // 虫染闪电：同一套几何体，RGB 换成 (0.8, 0.2, 1.0)，alpha 仍是 0.3。
        collector.submitCustomGeometry(poseStack, renderType, new PurpleLightningGeometry(state.seed));
    }
}
