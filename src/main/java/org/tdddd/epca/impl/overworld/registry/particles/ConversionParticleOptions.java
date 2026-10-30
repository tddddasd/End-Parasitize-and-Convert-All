package org.tdddd.epca.impl.overworld.registry.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.tdddd.epca.impl.overworld.registry.ModParticles;

public record ConversionParticleOptions(int color, int variant) implements ParticleOptions {
    public static final MapCodec<ConversionParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(Codec.INT.fieldOf("color").forGetter(ConversionParticleOptions::color),
                            Codec.INT.optionalFieldOf("variant", 0).forGetter(ConversionParticleOptions::variant))
                    .apply(i, ConversionParticleOptions::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConversionParticleOptions> STREAM_CODEC =
            StreamCodec.of((buf, o) -> { buf.writeInt(o.color); buf.writeByte(o.variant); },
                    buf -> new ConversionParticleOptions(buf.readInt(), buf.readUnsignedByte()));
    @Override public ParticleType<ConversionParticleOptions> getType() { return ModParticles.byVariant(variant); }
}