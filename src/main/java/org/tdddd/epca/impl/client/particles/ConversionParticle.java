package org.tdddd.epca.impl.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.tdddd.epca.impl.overworld.registry.particles.ConversionParticleOptions;

public class ConversionParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final boolean tint;

    protected ConversionParticle(ClientLevel level, double x, double y, double z,
                                 double dx, double dy, double dz, int color, boolean tint,
                                 SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz, sprites.get(0, 10));
        this.sprites = sprites;
        this.tint = tint;
        this.lifetime = 14;
        this.quadSize = 2.2F;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.xd = dx; this.yd = dy; this.zd = dz;
        if (tint) this.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
        else this.setColor(1.0F, 1.0F, 1.0F);
    }

    @Override public void tick() {
        super.tick();
        int frame = Mth.clamp((int) (this.age * 0.75F), 0, 9);
        this.setSprite(sprites.get(frame, 10));

        this.setAlpha(1.0F);
    }

    @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }

    public static class Provider implements ParticleProvider<ConversionParticleOptions> {
        private final SpriteSet sprites;
        private final boolean tint;
        public Provider(SpriteSet sprites, boolean tint) { this.sprites = sprites; this.tint = tint; }
        @Override public ConversionParticle createParticle(ConversionParticleOptions options, ClientLevel level,
                                                            double x, double y, double z, double dx, double dy, double dz,
                                                            RandomSource random) {
            return new ConversionParticle(level, x, y, z, dx, dy, dz, options.color(), tint, sprites);
        }
    }
}
