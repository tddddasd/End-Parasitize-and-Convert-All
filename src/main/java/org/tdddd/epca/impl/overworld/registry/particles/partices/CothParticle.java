package org.tdddd.epca.impl.overworld.registry.particles.partices;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

// 26.1.2: TextureSheetParticle -> SingleQuadParticle.
public class CothParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public CothParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.lifetime = 32; 
        this.setSpriteFromAge(sprites);
        
        this.xd = 0; 
        this.yd = 0; 
        this.zd = 0; 
        
        this.gravity = 0.0F;
        
        this.quadSize = 0.1F; 
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

    
    public static class CothParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public CothParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd,
                                       RandomSource random) {
            return new CothParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}