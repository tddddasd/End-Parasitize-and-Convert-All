package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

/**
 * 客户端专用（击杀数 / 方块）。
 *
 * <p>只实现 {@link IBlockComponentProvider}，负责 {@code appendTooltip} 渲染；只在
 * {@code registerClient(...)} 里被引用。数据端见 {@link KillCountBlockDataProvider}（同一个 UID）。
 */
public class KillCountBlockComponentProvider implements IBlockComponentProvider {

    public static final KillCountBlockComponentProvider INSTANCE = new KillCountBlockComponentProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
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
