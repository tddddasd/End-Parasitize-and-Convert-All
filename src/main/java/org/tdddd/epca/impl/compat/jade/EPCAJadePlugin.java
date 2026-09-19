package org.tdddd.epca.impl.compat.jade;

import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.BeckonCore;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade 插件入口。
 *
 * <p>26.1.2 关键约束：{@code @WailaPlugin} 只带 mod id、没有 dist 过滤，Jade 会在
 * <strong>专用服务器</strong>上实例化本类并调用
 * {@code register(IWailaCommonRegistration)}（客户端那一次还受
 * {@code CommonProxy.isPhysicallyClient()} 保护）。所以：
 *
 * <ul>
 *   <li>{@link #register(IWailaCommonRegistration)} 只允许引用实现了
 *       {@code snownee.jade.api.IServerDataProvider} 的<strong>服务端数据</strong>类
 *       （{@link KillCountEntityDataProvider}、{@link KillCountBlockDataProvider}、
 *       {@link DamageAdaptationEntityDataProvider}），它们的方法签名、字段、import 里都没有
 *       {@code net.minecraft.client.*} 类型。</li>
 *   <li>{@link #registerClient(IWailaClientRegistration)} 才引用实现了
 *       {@code IEntityComponentProvider} / {@code IBlockComponentProvider} 的
 *       <strong>客户端渲染</strong>类；那些类的 {@code appendTooltip(ITooltip, ...)} 会通过
 *       {@code ITooltip} 拖入客户端专有的 {@code net.minecraft.client.gui.layouts.LayoutElement}，
 *       在专用服务器上一旦被解析就是致命错误。</li>
 *   <li>本类不含任何静态初始化器（没有静态字段）；UID/配置键/NBT 键都在中立的
 *       {@link EPCAJadeIds}，懒加载图标在仅客户端可达的 {@link EPCAJadeClientIcons}。</li>
 * </ul>
 */
@WailaPlugin
public class EPCAJadePlugin implements IWailaPlugin {

    /** 服务端注册路径：只能碰到 server-safe 的类。 */
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(KillCountEntityDataProvider.INSTANCE, LivingEntity.class);

        registration.registerEntityDataProvider(DamageAdaptationEntityDataProvider.INSTANCE, LivingEntity.class);

        registration.registerBlockDataProvider(KillCountBlockDataProvider.INSTANCE, BeckonCoreBlockEntity.class);
    }

    /** 客户端注册路径：只有物理客户端才会执行到这里，可以自由引用客户端类型。 */
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(DamageAdaptationEntityComponentProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityComponent(KillCountEntityComponentProvider.INSTANCE, LivingEntity.class);
        registration.registerBlockComponent(KillCountBlockComponentProvider.INSTANCE, BeckonCore.class);
        // Config keys may already be registered by yawningapi
        try {
            registration.addConfig(EPCAJadeIds.KILL_COUNT_INFO, true);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            registration.addConfig(EPCAJadeIds.DAMAGE_ADAPTATION_INFO, true);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
