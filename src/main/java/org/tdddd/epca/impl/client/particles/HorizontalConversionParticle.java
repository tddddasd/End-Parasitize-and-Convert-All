package org.tdddd.epca.impl.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.tdddd.epca.impl.overworld.registry.particles.ConversionParticleOptions;

public class HorizontalConversionParticle extends SingleQuadParticle {

    private static final float QUAD_SIZE = 4.4F;

    private static final int FRAME_COUNT = 10;

    private static final float TINT_R = 0.247F;
    private static final float TINT_G = 0.353F;
    private static final float TINT_B = 0.165F;

    private final SpriteSet sprites;

    protected HorizontalConversionParticle(ClientLevel level, double x, double y, double z,
                                           double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz, sprites.get(0, FRAME_COUNT));
        this.sprites = sprites;
        this.lifetime = 14;
        this.quadSize = QUAD_SIZE;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.setColor(TINT_R, TINT_G, TINT_B);
    }

    @Override public FacingCameraMode getFacingCameraMode() {
        return (target, camera, partialTickTime) -> target.rotationX((float) (Math.PI / 2.0));
    }

    @Override public void tick() {
        super.tick();
        int frame = Mth.clamp((int) (this.age * 0.75F), 0, FRAME_COUNT - 1);
        this.setSprite(sprites.get(frame, FRAME_COUNT));

        this.setAlpha(1.0F);
    }

    @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }

    public static class Provider implements ParticleProvider<ConversionParticleOptions> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public HorizontalConversionParticle createParticle(ConversionParticleOptions options, ClientLevel level,
                                                                      double x, double y, double z,
                                                                      double dx, double dy, double dz,
                                                                      RandomSource random) {
            return new HorizontalConversionParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
