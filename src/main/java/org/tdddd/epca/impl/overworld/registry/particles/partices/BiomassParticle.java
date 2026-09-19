package org.tdddd.epca.impl.overworld.registry.particles.partices;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

import java.util.Random;

// 26.1.2: TextureSheetParticle -> SingleQuadParticle.
public class BiomassParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final float brightnessVariation; 

    public BiomassParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.lifetime = 36;
        this.setSpriteFromAge(sprites);
        
        this.gravity = 0.0F;
        
        this.quadSize = 0.2F; 

        
        Random random = new Random();
        this.brightnessVariation = 0.7F + random.nextFloat() * 0.4F; 

        
        this.rCol *= brightnessVariation;
        this.gCol *= brightnessVariation;
        this.bCol *= brightnessVariation;

        
        this.rCol = Math.min(Math.max(this.rCol, 0.0F), 1.0F);
        this.gCol = Math.min(Math.max(this.gCol, 0.0F), 1.0F);
        this.bCol = Math.min(Math.max(this.bCol, 0.0F), 1.0F);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    
    public static class BiomassParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BiomassParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd,
                                       RandomSource random) {
            return new BiomassParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}