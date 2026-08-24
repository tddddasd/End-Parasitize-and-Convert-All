package org.tdddd.epca.impl.overworld.registry.particles.partices;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class BleedingParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    public BleedingParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.lifetime = 7;
        this.setSpriteFromAge(sprites);
        this.speedUpWhenYMotionIsBlocked = true;
        
        this.gravity = 0.6F;
        
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
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    
    public static class BleedingParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BleedingParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new BleedingParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}