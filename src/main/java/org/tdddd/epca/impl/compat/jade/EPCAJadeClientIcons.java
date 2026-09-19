package org.tdddd.epca.impl.compat.jade;

import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.ModItems;

/**
 * 仅客户端可达的图标缓存（只被客户端的 tooltip provider 引用）。
 *
 * <p>26.1.2：Jade 会在专用服务器上实例化插件类，所以不能在静态初始化器里构建
 * {@code ItemStack}（那会碰到物品注册表）。这里保持懒加载；而且整个类只挂在
 * {@code registerClient(IWailaClientRegistration)} 这条路径上，服务端根本不会加载它。
 */
public final class EPCAJadeClientIcons {

    private static ItemStack killCountIcon;

    private EPCAJadeClientIcons() {
    }

    /** 懒加载的击杀数图标，附带 Jade 的小尺寸物品渲染。 */
    public static ItemStack killCountIcon() {
        if (killCountIcon == null) {
            killCountIcon = new ItemStack(ModItems.BIOMASS_COUNT_ICON.get());
        }
        return killCountIcon;
    }
}
