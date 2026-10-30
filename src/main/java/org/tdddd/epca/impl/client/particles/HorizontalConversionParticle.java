package org.tdddd.epca.impl.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.tdddd.epca.impl.overworld.registry.particles.ConversionParticleOptions;

/**
 * Same 10-frame conversion burst as {@link ConversionParticle}, but the quad always lies flat on the
 * XZ plane instead of facing the camera - the "horizontal" variant (id 5) that the Symbiosis Lie death
 * burst spawns.
 *
 * <p>26.1.2 note: the 1.20.1 class drew its four flat vertices by hand inside {@code render(...)};
 * 26.1.2 moved quad emission to {@code SingleQuadParticle#extract}, so the flat orientation is
 * expressed through {@link #getFacingCameraMode()} instead (a fixed +90 degree X rotation). As in
 * 1.20.1 the quad's total width is {@code 2 * 2.2}, i.e. twice the camera-facing variant, which is
 * why {@code quadSize} is 4.4 here.</p>
 */
public class HorizontalConversionParticle extends SingleQuadParticle {
    /** Total quad width. 1.20.1 drew +/- quadSize, so the visible width is 2 * 2.2. */
    private static final float QUAD_SIZE = 4.4F;
    /** Frames in the convert_0..convert_9 sheet. */
    private static final int FRAME_COUNT = 10;
    /** 1.20.1 hard-codes the olive green tint for this variant (0x3F5A2A). */
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

    /** Lay the quad flat on the XZ plane: a constant 90 degree rotation about X. */
    @Override public FacingCameraMode getFacingCameraMode() {
        return (target, camera, partialTickTime) -> target.rotationX((float) (Math.PI / 2.0));
    }

    @Override public void tick() {
        super.tick();
        int frame = Mth.clamp((int) (this.age * 0.75F), 0, FRAME_COUNT - 1);
        this.setSprite(sprites.get(frame, FRAME_COUNT));
        // Opaque: no alpha fade-out (same treatment as the camera-facing conversion particle).
        this.setAlpha(1.0F);
    }

    /**
     * Fully opaque burst: the vertex alpha stays at 1.0 for the particle's whole life instead of
     * fading out. The translucent layer is kept on purpose - the convert_* sheet is a cut-out
     * (its transparent texels carry black RGB and there is no partial alpha), so an opaque pipeline
     * would draw those texels as a black box around the burst.
     */
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
