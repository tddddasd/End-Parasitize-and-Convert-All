package org.tdddd.epca.impl.overworld.registry.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;

public class AdaptationParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprite;

    public AdaptationParticleProvider(SpriteSet sprite) {
        this.sprite = sprite;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed,
                                   RandomSource random) {
        // 26.1.2: the sprite is passed into the particle constructor; TextureSheetParticle#pickSprite is gone.
        return new AdaptationParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, type, this.sprite.get(random));
    }
}