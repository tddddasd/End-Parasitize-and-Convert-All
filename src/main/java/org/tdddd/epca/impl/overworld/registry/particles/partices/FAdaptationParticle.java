package org.tdddd.epca.impl.overworld.registry.particles.partices;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

// 26.1.2: TextureSheetParticle -> SingleQuadParticle.
public class FAdaptationParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public FAdaptationParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.lifetime = 24; 
        this.setSpriteFromAge(sprites);
        
        this.xd = 0; 
        this.yd = 0; 
        this.zd = 0; 
        
        this.gravity = 0.0F;
        
        this.quadSize = 0.15F; 
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    
    public static class FAdaptationParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FAdaptationParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd,
                                       RandomSource random) {
            return new FAdaptationParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}