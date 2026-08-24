package org.tdddd.epca.impl.overworld.registry.capability;

public interface IShieldCapability {
    float getShield();
    void setShield(float shield);
    void addShield(float amount);
    void consumeShield(float amount);
}