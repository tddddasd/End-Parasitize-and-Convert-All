package org.tdddd.epca.impl.overworld.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;

public class CarryConfig {
    public static final Codec<CarryConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.list(ResourceLocation.CODEC).fieldOf("carryable").forGetter(CarryConfig::getCarryable)
            ).apply(instance, CarryConfig::new)
    );

    private final List<ResourceLocation> carryable;

    public CarryConfig(List<ResourceLocation> carryable) {
        this.carryable = carryable != null ? carryable : Collections.emptyList();
    }

    public List<ResourceLocation> getCarryable() {
        return carryable;
    }
}