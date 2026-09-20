package org.tdddd.epca.impl.overworld.registry.capability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;


@EventBusSubscriber(modid = epca.MODID)
public class LifetimeCapability implements ILifetimeCapability, net.neoforged.neoforge.common.util.ValueIOSerializable {

    
    public static final net.neoforged.neoforge.registries.DeferredHolder<
            net.neoforged.neoforge.attachment.AttachmentType<?>,
            net.neoforged.neoforge.attachment.AttachmentType<LifetimeCapability>> LIFETIME =
            EpcaAttachments.LIFETIME;

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
            if (remainingTicks == 0 && entity != null && !entity.level().isClientSide()) {
                entity.discard(); 
            }
        }
    }

    

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("RemainingTicks", this.remainingTicks);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.remainingTicks = input.getIntOr("RemainingTicks", -1);
    }

    
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) {
            
            
            LifetimeCapability cap = living.getExistingDataOrNull(EpcaAttachments.LIFETIME);
            if (cap != null) {
                cap.tick();
            }
        }
    }
}
