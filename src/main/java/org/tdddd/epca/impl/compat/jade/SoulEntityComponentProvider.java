package org.tdddd.epca.impl.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

public class SoulEntityComponentProvider implements IEntityComponentProvider {

    public static final SoulEntityComponentProvider INSTANCE = new SoulEntityComponentProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("EpcaSoulMaxValue")) return;
        double soul = data.getDoubleOr("EpcaSoulValue", 0.0);
        double max = data.getDoubleOr("EpcaSoulMaxValue", 0.0);
        if (max <= 0.0) return;
        float ratio = (float) Math.max(0.0, Math.min(1.0, soul / max));
        tooltip.add(java.util.List.of(new SoulIconElement(),
                JadeUI.text(Component.literal(String.format("   %.1f/%.1f", soul, max)).withStyle(ChatFormatting.AQUA))));
        tooltip.add(new SoulBarElement(ratio));
    }

    @Override
    public Identifier getUid() {
        return SoulEntityDataProvider.UID;
    }
}