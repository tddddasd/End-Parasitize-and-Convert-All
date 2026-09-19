package org.tdddd.epca.impl.events;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;

/**
 * 1.20.1 版在 {@code AttachCapabilitiesEvent<Entity>} 里给每个 {@code LivingEntity}
 * 挂一个 {@code ShieldCapability.Provider}（{@code ICapabilityProvider} + {@code LazyOptional}）。
 *
 * <p><b>26.1.2 改动</b>：数据附件**不需要逐实体附加**，也没有“provider”这个概念——
 * 附件在第一次 {@code getData} 时按默认值惰性创建。因此本类不再监听任何事件，
 * 只保留原能力 id（{@code epca:shield}）作为常量，附件本体见
 * {@link ShieldCapabilityHandler#SHIELD_CAP}。
 * 该类仍由 {@code epca} 构造器注册到游戏总线：注册一个没有任何 {@code @SubscribeEvent}
 * 方法的对象是无副作用的 no-op，保持 1.20.1 的接线形状不变。
 */
public class ShieldAttachHandler {
    /** 原 {@code AttachCapabilitiesEvent#addCapability} 用的 id，1.20.1 里是 private。 */
    public static final Identifier SHIELD_CAP_ID = Identifier.fromNamespaceAndPath(epca.MODID, "shield");
}
