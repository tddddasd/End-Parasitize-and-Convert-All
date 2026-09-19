package org.tdddd.epca.impl.overworld.registry.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;

// 26.1.2: TextureSheetParticle -> SingleQuadParticle (see InfestiveGasParticle).
public class AdaptationParticle extends SingleQuadParticle {
    protected AdaptationParticle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed,
                                 SimpleParticleType type, TextureAtlasSprite sprite) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        
        this.quadSize = 0.2f;
        this.lifetime = 40;
        this.gravity = 0;
        this.hasPhysics = false;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        // 1.20.1: ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    // 26.1.2: Particle#shouldCull() no longer exists; the particle group always frustum-culls by
    // position. The old override (always render) has no replacement hook and was dropped.
}