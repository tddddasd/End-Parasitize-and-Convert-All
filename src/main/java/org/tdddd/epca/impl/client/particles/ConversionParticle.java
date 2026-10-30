package org.tdddd.epca.impl.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.tdddd.epca.impl.overworld.registry.particles.ConversionParticleOptions;

/** 10-frame, 15 FPS conversion burst. 15 FPS = 1.333 ticks per frame. */
public class ConversionParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected ConversionParticle(ClientLevel level, double x, double y, double z,
                                 double dx, double dy, double dz, int color, boolean tint, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.lifetime = 14;
        this.quadSize = 2.2F;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.xd = dx; this.yd = dy; this.zd = dz;
        if (tint) this.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
        else this.setColor(1.0F, 1.0F, 1.0F);
        this.setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        super.tick();
        int frame = Mth.clamp((int) (this.age * 0.75F), 0, 9);
        this.setSprite(sprites.get(frame, 10));
        // Opaque: no alpha fade-out - the burst keeps the sprite's own (fully opaque) pixels.
        this.alpha = 1.0F;
    }

    /**
     * Fully opaque burst: the vertex alpha stays at 1.0 for the particle's whole life instead of
     * fading out. The translucent sheet is kept on purpose - the convert_* sheet is a cut-out
     * (its transparent texels carry black RGB and there is no partial alpha), so PARTICLE_SHEET_OPAQUE
     * would draw those texels as a black box around the burst.
     */
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class Provider implements ParticleProvider<ConversionParticleOptions> {
        private final SpriteSet sprites;
        private final boolean tint;
        public Provider(SpriteSet sprites, boolean tint) { this.sprites = sprites; this.tint = tint; }
        @Override public ConversionParticle createParticle(ConversionParticleOptions options, ClientLevel level,
                                                            double x, double y, double z, double dx, double dy, double dz) {
            return new ConversionParticle(level, x, y, z, dx, dy, dz, options.color(), tint, sprites);
        }
    }
}