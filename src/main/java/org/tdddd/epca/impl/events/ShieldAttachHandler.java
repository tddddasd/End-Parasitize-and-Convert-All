package org.tdddd.epca.impl.events;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.capability.IShieldCapability;
import org.tdddd.epca.impl.overworld.registry.capability.ShieldCapability;
import org.tdddd.epca.impl.epca;

public class ShieldAttachHandler {
    private static final ResourceLocation SHIELD_CAP_ID = new ResourceLocation(epca.MODID, "shield");

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            ShieldCapability shieldCap = new ShieldCapability();
            LazyOptional<IShieldCapability> capInstance = LazyOptional.of(() -> shieldCap);
            ICapabilityProvider provider = new ICapabilityProvider() {
                @Override
                public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                    return ShieldCapabilityHandler.SHIELD_CAP.orEmpty(cap, capInstance);
                }
            };
            event.addCapability(SHIELD_CAP_ID, provider);
            event.addListener(() -> capInstance.invalidate());
        }
    }
}