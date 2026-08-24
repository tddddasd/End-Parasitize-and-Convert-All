package org.tdddd.epca.impl.overworld.registry.particles.partices;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.Random;

public class BiomassBoomSmallParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float brightnessVariation; 

    public BiomassBoomSmallParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.lifetime = 16;
        this.setSpriteFromAge(sprites);

        
        this.gravity = 0.0F;
        
        this.quadSize = 0.3F; 
        
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
    public void tick() {
        super.tick();
        
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; 
    }

    
    public static class BiomassBoomSmallParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BiomassBoomSmallParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new BiomassBoomSmallParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}