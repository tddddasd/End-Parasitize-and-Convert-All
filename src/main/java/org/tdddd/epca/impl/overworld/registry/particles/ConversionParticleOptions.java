package org.tdddd.epca.impl.overworld.registry.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.tdddd.epca.impl.overworld.registry.ModParticles;

/** Color + family variant for the 10-frame conversion burst. */
public record ConversionParticleOptions(int color, int variant) implements ParticleOptions {
    public static final MapCodec<ConversionParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(Codec.INT.fieldOf("color").forGetter(ConversionParticleOptions::color),
                            Codec.INT.optionalFieldOf("variant", 0).forGetter(ConversionParticleOptions::variant))
                    .apply(i, ConversionParticleOptions::new));

    @Override public ParticleType<ConversionParticleOptions> getType() { return ModParticles.byVariant(variant); }
    @Override public void writeToNetwork(FriendlyByteBuf buf) { buf.writeInt(color); buf.writeByte(variant); }
    @Override public String writeToString() { return "epca:conversion[" + Integer.toHexString(color) + "," + variant + "]"; }
}
