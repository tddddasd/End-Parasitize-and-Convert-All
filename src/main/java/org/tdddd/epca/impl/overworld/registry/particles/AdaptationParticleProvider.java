package org.tdddd.epca.impl.overworld.registry.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AdaptationParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprite;

    public AdaptationParticleProvider(SpriteSet sprite) {
        this.sprite = sprite;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        AdaptationParticle particle = new AdaptationParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, type);
        particle.pickSprite(this.sprite);
        return particle;
    }
}