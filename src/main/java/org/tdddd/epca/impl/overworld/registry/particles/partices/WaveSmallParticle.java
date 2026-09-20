package org.tdddd.epca.impl.overworld.registry.particles.partices;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

// 26.1.2: TextureSheetParticle -> SingleQuadParticle, and the per-vertex `render(...)` hook is gone.
// The fixed 90-degree X rotation of the quad is expressed as a FacingCameraMode (see WaveParticle).
public class WaveSmallParticle extends SingleQuadParticle {
    
    private static final SingleQuadParticle.FacingCameraMode FIXED_X =
            (target, camera, partialTickTime) -> target.rotationX((float) Math.toRadians(90.0F));

    private final SpriteSet sprites;
    private int ageCounter = 0;
    private final int totalLifetime = 21; 
    private int currentFrameIndex = 0; 
    private static final int FRAME_TIME = 3; 
    private static final int[] FRAME_SEQUENCE = {0, 2, 3, 4, 5, 6, 7}; 

    public WaveSmallParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.lifetime = totalLifetime;

        
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        
        this.gravity = 0.0F;

        
        this.quadSize = 2.5F;
        this.alpha = 0.9F;

        
        this.currentFrameIndex = 0;
        this.setSprite(sprites.get(RandomSource.create(FRAME_SEQUENCE[0])));
    }

    @Override
    public void tick() {
        super.tick();
        ageCounter++;

        
        if (!this.removed) {
            
            int targetFrameIndex = Math.min(ageCounter / FRAME_TIME, FRAME_SEQUENCE.length - 1);

            
            if (targetFrameIndex != currentFrameIndex) {
                currentFrameIndex = targetFrameIndex;
                this.setSprite(sprites.get(RandomSource.create(FRAME_SEQUENCE[currentFrameIndex])));
            }

            
            float ageRatio = (float) ageCounter / totalLifetime;
            if (ageRatio < 0.1f) {
                
                this.alpha = 0.9F * (ageRatio / 0.1f);
            } else if (ageRatio > 0.9f) {
                
                this.alpha = 0.9F * ((1.0f - ageRatio) / 0.1f);
            } else {
                this.alpha = 0.9F;
            }
        }
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return FIXED_X;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        // 1.20.1: ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    
    public static class WaveSmallParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public WaveSmallParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd,
                                       RandomSource random) {
            return new WaveSmallParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}