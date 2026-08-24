package org.tdddd.epca.impl.overworld.registry.capability;

public interface ILifetimeCapability {
    void setRemainingTicks(int ticks);
    int getRemainingTicks();
    void tick(); 
}