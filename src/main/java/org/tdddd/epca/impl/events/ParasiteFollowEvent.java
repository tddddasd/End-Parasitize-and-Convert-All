package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import java.util.UUID;


public class ParasiteFollowEvent extends LivingEvent implements ICancellableEvent {
    private final UUID oldTarget;
    private final UUID newTarget;

    public ParasiteFollowEvent(LivingEntity entity, UUID oldTarget, UUID newTarget) {
        super(entity);
        this.oldTarget = oldTarget;
        this.newTarget = newTarget;
    }

    
    public UUID getOldTarget() {
        return oldTarget;
    }

    
    public UUID getNewTarget() {
        return newTarget;
    }

    
    public boolean isStartFollow() {
        return oldTarget == null && newTarget != null;
    }

    
    public boolean isStopFollow() {
        return oldTarget != null && newTarget == null;
    }

    
    public boolean isSwitchTarget() {
        return oldTarget != null && newTarget != null && !oldTarget.equals(newTarget);
    }
}