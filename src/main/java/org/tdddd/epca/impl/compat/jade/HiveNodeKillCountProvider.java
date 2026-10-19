package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

/** Shows a hive node's stored biomass as "current / 64". */
public class HiveNodeKillCountProvider implements IBlockComponentProvider {

    public static final HiveNodeKillCountProvider INSTANCE = new HiveNodeKillCountProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(EPCAJadeIds.KILL_COUNT_INFO)) {
            return;
        }
        CompoundTag data = accessor.getServerData();
        if (data.getInt(EPCAJadeIds.NBT_KILL_COUNT).isEmpty()) {
            return;
        }
        int count = data.getIntOr(EPCAJadeIds.NBT_KILL_COUNT, 0);
        int max = data.getIntOr("EPCA_KillCountMax", 0);
        if (count <= 0) {
            return;
        }
        tooltip.add(JadeUI.smallItem(EPCAJadeClientIcons.killCountIcon()));
        tooltip.append(Component.literal(" " + count + "/" + max));
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.KILL_COUNT_INFO;
    }
}
