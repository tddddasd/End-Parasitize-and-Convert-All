package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.tdddd.epca.impl.utils.IPurpleLightningBolt;

/**
 * 26.1.2 渲染管线的 render state 侧：给 {@link LightningBoltRenderState} 挂一个「虫染紫」标记位。
 *
 * <p>1.20.1 的渲染器可以在 {@code render(...)} 里同时看到 {@code LightningBolt} 与绘制过程，
 * 所以一个 {@code ThreadLocal} 就够了。26.1.2 把抽取与提交分开，且<b>所有实体先抽取完再统一提交</b>，
 * {@code ThreadLocal} 会被后一个实体的抽取覆盖，因此标记必须搭在 state 上。
 *
 * <p>标记通过普通接口 {@link IPurpleLightningBolt} 暴露，绝不在另一个 mixin 里出现本类的类型引用
 * ——那正是之前 {@code InvalidMixinException: unable to find corresponding type for
 * LightningBoltRenderStateMixin} 的原因（详见 {@link IPurpleLightningBolt} 的类注释）。
 *
 * <p>目标类已用 {@code javap} 核对（{@code minecraft-patched-26.1.2.76.jar}）：
 * <pre>
 * public class net.minecraft.client.renderer.entity.state.LightningBoltRenderState
 *         extends net.minecraft.client.renderer.entity.state.EntityRenderState {
 *   public long seed;
 *   public net.minecraft.client.renderer.entity.state.LightningBoltRenderState();
 * }
 * </pre>
 */
@Mixin(LightningBoltRenderState.class)
public class LightningBoltRenderStateMixin implements IPurpleLightningBolt {

    @Unique
    private boolean epca$purpleBolt;

    @Override
    public boolean epca$isPurpleBolt() {
        return this.epca$purpleBolt;
    }

    @Override
    public void epca$setPurpleBolt(boolean purple) {
        this.epca$purpleBolt = purple;
    }
}
