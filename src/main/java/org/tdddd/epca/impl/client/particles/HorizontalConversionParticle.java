package org.tdddd.epca.impl.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.particles.ConversionParticleOptions;

/** Same 10-frame conversion burst, but the quad always lies flat on the XZ plane. */
public class HorizontalConversionParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected HorizontalConversionParticle(ClientLevel level, double x, double y, double z,
                                           double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.lifetime = 14;
        this.quadSize = 2.2F;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.xd = dx; this.yd = dy; this.zd = dz;
        this.setColor(0.247F, 0.353F, 0.165F);
        this.setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        super.tick();
        int frame = Mth.clamp((int) (this.age * 0.75F), 0, 9);
        this.setSprite(sprites.get(frame, 10));
        // Opaque: no alpha fade-out (same treatment as the camera-facing conversion particle).
        this.alpha = 1.0F;
    }

    @Override public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x);
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y) + 0.01F;
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z);
        float s = this.quadSize;
        int light = this.getLightColor(partialTick);
        buffer.vertex(x - s, y, z - s).uv(this.getU1(), this.getV1()).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(x - s, y, z + s).uv(this.getU1(), this.getV0()).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(x + s, y, z + s).uv(this.getU0(), this.getV0()).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(x + s, y, z - s).uv(this.getU0(), this.getV1()).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
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
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public HorizontalConversionParticle createParticle(ConversionParticleOptions options, ClientLevel level,
                                                                      double x, double y, double z, double dx, double dy, double dz) {
            return new HorizontalConversionParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}