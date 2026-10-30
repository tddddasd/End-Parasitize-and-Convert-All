package org.tdddd.epca.impl.overworld.registry.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ConversionParticleType extends ParticleType<ConversionParticleOptions> {
    public ConversionParticleType() { super(false); }
    @Override public MapCodec<ConversionParticleOptions> codec() { return ConversionParticleOptions.CODEC; }
    @Override public StreamCodec<? super RegistryFriendlyByteBuf, ConversionParticleOptions> streamCodec() { return ConversionParticleOptions.STREAM_CODEC; }
}