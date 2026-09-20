package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

import java.util.UUID;


@Cancelable
public class ParasiteFollowEvent extends LivingEvent {
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