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

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.BIOMASS_COUNT_INFO;
    }
}
