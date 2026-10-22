package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;


public class BiomassCountEntityComponentProvider implements IEntityComponentProvider {

    public static final BiomassCountEntityComponentProvider INSTANCE = new BiomassCountEntityComponentProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!config.get(EPCAJadeIds.BIOMASS_COUNT_INFO)) return;
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.getInt(EPCAJadeIds.NBT_BIOMASS_COUNT).isPresent()) return;
        int count = serverData.getIntOr(EPCAJadeIds.NBT_BIOMASS_COUNT, 0);
        if (count <= 0) return;

        tooltip.add(JadeUI.smallItem(EPCAJadeClientIcons.biomassCountIcon()));
        tooltip.append(Component.literal(" " + count));
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.BIOMASS_COUNT_INFO;
    }
}
