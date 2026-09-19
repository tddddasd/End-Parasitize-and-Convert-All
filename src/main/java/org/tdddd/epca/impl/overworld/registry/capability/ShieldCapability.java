package org.tdddd.epca.impl.overworld.registry.capability;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * 灵魂护盾数值。
 *
 * <p><b>26.1.2 改动</b>：原 {@code serializeNBT/deserializeNBT(CompoundTag)} 改为
 * {@link ValueIOSerializable} 的 {@code serialize(ValueOutput)/deserialize(ValueInput)}
 * （Forge 的 {@code INBTSerializable} 与 {@code CompoundTag} 存取在 26.1.2 已删除）。
 * <b>存档字段名与结构不变</b>：{@code shield}（float）。
 */
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
