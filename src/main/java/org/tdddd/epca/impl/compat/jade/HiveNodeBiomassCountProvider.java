package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

public class HiveNodeBiomassCountProvider implements IBlockComponentProvider {

    public static final HiveNodeBiomassCountProvider INSTANCE = new HiveNodeBiomassCountProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(EPCAJadeIds.BIOMASS_COUNT_INFO)) {
            return;
        }
        CompoundTag data = accessor.getServerData();
        if (data.getInt(EPCAJadeIds.NBT_BIOMASS_COUNT).isEmpty()) {
            return;
        }
        int count = data.getIntOr(EPCAJadeIds.NBT_BIOMASS_COUNT, 0);
        int max = data.getIntOr("EPCA_BiomassCountMax", 0);
        if (count <= 0) {
            return;
        }
        tooltip.add(JadeUI.smallItem(EPCAJadeClientIcons.biomassCountIcon()));
        tooltip.append(Component.literal(" " + count + "/" + max));
    }

    /**
     * 方块组件的 id 与召唤柱的同名组件区分开（否则同一注册表里两个实例同 id，Jade 会警告）；
     * 配置开关仍然读 {@link EPCAJadeIds#BIOMASS_COUNT_INFO}，玩家侧只有一个开关。
     */
    @Override
    public Identifier getUid() {
        return EPCAJadeIds.HIVE_NODE_BIOMASS_COUNT;
    }
}
