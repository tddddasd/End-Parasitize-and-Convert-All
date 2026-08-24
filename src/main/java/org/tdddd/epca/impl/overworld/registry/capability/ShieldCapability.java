package org.tdddd.epca.impl.overworld.registry.capability;

import net.minecraft.nbt.CompoundTag;

public class ShieldCapability implements IShieldCapability {
    private float shield = 0.0f;

    @Override
    public float getShield() {
        return shield;
    }

    @Override
    public void setShield(float shield) {
        this.shield = Math.max(0, shield);
    }

    @Override
    public void addShield(float amount) {
        this.shield += amount;
        if (this.shield < 0) this.shield = 0;
    }

    @Override
    public void consumeShield(float amount) {
        this.shield = Math.max(0, this.shield - amount);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("shield", shield);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.shield = tag.getFloat("shield");
    }
}