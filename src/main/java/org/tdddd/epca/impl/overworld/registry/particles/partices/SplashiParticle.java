package org.tdddd.epca.impl.overworld.registry.particles.partices;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import org.tdddd.epca.impl.epca;

// 26.1.2: TextureSheetParticle -> SingleQuadParticle.
public class SplashiParticle extends SingleQuadParticle {
    private final SpriteSet spriteSet; 

    protected SplashiParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd, spriteSet.first());
        this.spriteSet = spriteSet; 
        this.friction = 0.9F;
        this.gravity = 0.3F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.quadSize = 0.18F;
        this.lifetime = 40; 
        this.setSpriteFromAge(spriteSet); 
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.spriteSet); 
    }

    
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd,
                                       RandomSource random) {
            try {
                if (spriteSet == null) {
                    epca.LOGGER.error("SpriteSet is null in SplashiParticle provider!");
                    return null;
                }
                return new SplashiParticle(level, x, y, z, xd, yd, zd, this.spriteSet);
            } catch (Exception e) {
                epca.LOGGER.error("Failed to create Splashi particle", e);
                return null;
            }
        }
    }
}