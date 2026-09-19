package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

/**
 * 客户端专用（击杀数 / 实体）。
 *
 * <p>只实现 Jade 的客户端组件接口 {@link IEntityComponentProvider}，负责 {@code appendTooltip} 渲染；
 * 它只在 {@code registerClient(IWailaClientRegistration)} 里被引用，专用服务器不会加载它
 * （{@code IEntityComponentProvider} → {@code ITooltip} 会拖出客户端专有的
 * {@code net.minecraft.client.gui.layouts.LayoutElement}）。
 *
 * <p>UID 与数据端 {@link KillCountEntityDataProvider} 完全一致，用于把服务端 NBT 数据配到渲染上。
 */
public class KillCountEntityComponentProvider implements IEntityComponentProvider {

    public static final KillCountEntityComponentProvider INSTANCE = new KillCountEntityComponentProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!config.get(EPCAJadeIds.KILL_COUNT_INFO)) return;
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.getInt(EPCAJadeIds.NBT_KILL_COUNT).isPresent()) return;
        int count = serverData.getIntOr(EPCAJadeIds.NBT_KILL_COUNT, 0);
        if (count <= 0) return;

        tooltip.add(JadeUI.smallItem(EPCAJadeClientIcons.killCountIcon()));
        tooltip.append(Component.literal(" " + count));
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.KILL_COUNT_INFO;
    }
}
