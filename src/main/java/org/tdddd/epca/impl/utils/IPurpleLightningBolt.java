package org.tdddd.epca.impl.utils;

/**
 * 「这道闪电是否要画成虫染紫」的鸭子接口（duck interface）。
 *
 * <p>26.1.2 的实体渲染被拆成抽取（{@code LightningBoltRenderer#extractRenderState}，还能看到
 * {@code LightningBolt}）与提交（{@code #submit}，只看得到 {@code LightningBoltRenderState}）
 * 两趟，所以「脚下的方块是不是 {@code InfestedBlockInterface}」这个判定必须搭在 render state 上从
 * 抽取趟传到提交趟。
 *
 * <p><b>为什么是普通接口，而不是直接引用另一个 mixin 类。</b>
 * 之前那版把 flag 声明在 {@code LightningBoltRenderStateMixin} 上，再由
 * {@code LightningBoltRendererMixin} 把 {@code state} 强转成那个 mixin 类型：
 * 只要某个 mixin 的方法体里出现另一个 mixin 类的类型引用，Mixin 的
 * {@code MixinTargetContext#remapClassName} 就会认为它需要解析成「真实类型」
 * （{@code ClassInfo.isMixin(name) && !isLoadable()} → {@code findRealType}），
 * 而 {@code findCorrespondingType} 只会沿着<b>当前目标类的继承体系</b>去找
 * {@code LightningBoltRenderState}——它不是 {@code LightningBoltRenderer} 的父类，
 * 于是抛 {@code InvalidMixinException: Resolution error: unable to find corresponding type for
 * ...LightningBoltRenderStateMixin in hierarchy of ...LightningBoltRenderer}，客户端直接崩。
 * （证据见本次交付说明：反编译 sponge-mixin 0.17.3 的
 * {@code org/spongepowered/asm/mixin/transformer/MixinTargetContext}，
 * 报错字符串 {@code "Resolution error: unable to find corresponding type for "} 位于
 * {@code findRealType}，唯一调用点是 {@code remapClassName} 里对 mixin 名字的分支。）
 *
 * <p>本接口是模组自己的<b>普通类</b>（没在 mixin 配置里注册，{@code ClassInfo.isMixin} 为 false），
 * Mixin 遇到它只会原样保留，不再去解析继承体系，因此跨 mixin 共享状态是安全的。
 * 与项目里既有的 {@link IBeaconMixin}（由 {@code BeaconBlockEntityMixin} 实现、
 * 由 {@code BeaconMenuMixin} 使用）是同一套写法。
 */
public interface IPurpleLightningBolt {

    /** 该 render state 对应的闪电是否站在虫染方块上。 */
    boolean epca$isPurpleBolt();

    /**
     * 写入判定结果。{@code LightningBoltRenderer#extractRenderState} 每帧都会无条件写一次，
     * 所以 flag 不会跨帧残留（render state 由渲染器复用）。
     */
    void epca$setPurpleBolt(boolean purple);
}
