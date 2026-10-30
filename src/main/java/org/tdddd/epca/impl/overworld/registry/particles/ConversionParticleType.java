package org.tdddd.epca.impl.overworld.registry.particles;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public class ConversionParticleType extends ParticleType<ConversionParticleOptions> {
    public ConversionParticleType() {
        super(false, new ParticleOptions.Deserializer<ConversionParticleOptions>() {
            @Override public ConversionParticleOptions fromCommand(ParticleType<ConversionParticleOptions> type, com.mojang.brigadier.StringReader reader) {
                return new ConversionParticleOptions(0x3F5A2A, 0);
            }
            @Override public ConversionParticleOptions fromNetwork(ParticleType<ConversionParticleOptions> type, net.minecraft.network.FriendlyByteBuf buf) {
                return new ConversionParticleOptions(buf.readInt(), buf.readUnsignedByte());
            }
        });
    }
    @Override public Codec<ConversionParticleOptions> codec() { return ConversionParticleOptions.CODEC.codec(); }
}
