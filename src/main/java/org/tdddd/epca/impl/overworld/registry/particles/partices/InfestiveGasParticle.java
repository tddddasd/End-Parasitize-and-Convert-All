package org.tdddd.epca.impl.overworld.registry.particles.partices;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

import java.util.Random;

public class InfestiveGasParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float initialAlpha; 
    private final float brightnessVariation; 

    
    private static final int FRAME_TIME = 5; 
    private static final int[] FRAME_SEQUENCE = {0, 1, 2, 3, 4, 5, 6, 7, 8}; 
    private int currentFrameIndex = 0; 

    public InfestiveGasParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;

        
        this.lifetime = FRAME_SEQUENCE.length * FRAME_TIME;

        
        this.currentFrameIndex = 0;
        this.setSprite(sprites.get(RandomSource.create(FRAME_SEQUENCE[0])));

        
        this.xd = 0; 
        this.yd = 0; 
        this.zd = 0; 
        
        this.gravity = 0.0F;
        
        this.quadSize = 1.05F; 

        
        this.initialAlpha = 0.9F;
        this.alpha = initialAlpha;

        
        Random random = new Random();
        this.brightnessVariation = 0.8F + random.nextFloat() * 0.3F; 

        
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

        if (!this.removed) {
            
            int targetFrameIndex = Math.min(this.age / FRAME_TIME, FRAME_SEQUENCE.length - 1);

            
            if (targetFrameIndex != currentFrameIndex) {
                currentFrameIndex = targetFrameIndex;
                this.setSprite(sprites.get(RandomSource.create(FRAME_SEQUENCE[currentFrameIndex])));
            }

            
            float ageRatio = (float)this.age / (float)this.lifetime;

            
            this.alpha = initialAlpha * (1.0F - ageRatio);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; 
    }

    
    public static class InfestiveGasParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public InfestiveGasParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new InfestiveGasParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}