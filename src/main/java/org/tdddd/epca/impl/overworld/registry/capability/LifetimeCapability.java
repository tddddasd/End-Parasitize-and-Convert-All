package org.tdddd.epca.impl.overworld.registry.capability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class LifetimeCapability implements ILifetimeCapability {
    public static final Capability<ILifetimeCapability> LIFETIME = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation ID = new ResourceLocation(epca.MODID, "lifetime");

    private int remainingTicks = -1; 
    private LivingEntity entity;     

    public LifetimeCapability(LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public void setRemainingTicks(int ticks) {
        this.remainingTicks = ticks;
    }

    @Override
    public int getRemainingTicks() {
        return remainingTicks;
    }

    @Override
    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0 && entity != null && !entity.level().isClientSide) {
                entity.discard(); 
            }
        }
    }

    
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity living) {
            LifetimeCapability provider = new LifetimeCapability(living);
            event.addCapability(ID, new ICapabilitySerializable<CompoundTag>() {
                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                    return LIFETIME.orEmpty(cap, LazyOptional.of(() -> provider));
                }

                @Override
                public CompoundTag serializeNBT() {
                    CompoundTag tag = new CompoundTag();
                    tag.putInt("RemainingTicks", provider.remainingTicks);
                    return tag;
                }

                @Override
                public void deserializeNBT(CompoundTag tag) {
                    provider.remainingTicks = tag.getInt("RemainingTicks");
                }
            });
        }
    }

    
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        event.getEntity().getCapability(LIFETIME).ifPresent(ILifetimeCapability::tick);
    }
}