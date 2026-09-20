package org.tdddd.epca.impl.overworld.registry.capability;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;


public class ShieldCapability implements IShieldCapability, ValueIOSerializable {
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

    @Override
    public void serialize(ValueOutput output) {
        output.putFloat("shield", shield);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.shield = input.getFloatOr("shield", 0.0F);
    }
}
