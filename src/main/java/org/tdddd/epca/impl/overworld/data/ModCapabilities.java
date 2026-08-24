package org.tdddd.epca.impl.overworld.data;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.items.IItemHandler;

public class ModCapabilities {
    public static final Capability<IItemHandler> ARMOR_HANDLER = CapabilityManager.get(new CapabilityToken<>(){});
}