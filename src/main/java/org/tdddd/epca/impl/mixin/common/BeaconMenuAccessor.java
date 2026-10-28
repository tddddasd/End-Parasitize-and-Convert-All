package org.tdddd.epca.impl.mixin.common;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconMenu.class)
public interface BeaconMenuAccessor {
    @Accessor("beacon")
    Container epca$getBeacon();

    @Accessor("access")
    ContainerLevelAccess epca$getAccess();
}