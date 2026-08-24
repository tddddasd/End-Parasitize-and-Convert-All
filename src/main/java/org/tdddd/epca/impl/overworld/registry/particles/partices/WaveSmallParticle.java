package org.tdddd.epca.impl.overworld.registry.particles.partices;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class WaveSmallParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private int ageCounter = 0;
    private final int totalLifetime = 21; 
    private int currentFrameIndex = 0; 
    private static final int FRAME_TIME = 3; 
    private static final int[] FRAME_SEQUENCE = {0, 2, 3, 4, 5, 6, 7}; 

    public WaveSmallParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
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
    public void render(VertexConsumer buffer, net.minecraft.client.Camera camera, float partialTick) {
        
        var cameraPos = camera.getPosition();
        float x = (float)(net.minecraft.util.Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x());
        float y = (float)(net.minecraft.util.Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y());
        float z = (float)(net.minecraft.util.Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z());

        
        Quaternionf quaternion = new Quaternionf();

        
        float angle = (float)Math.toRadians(90.0f);
        quaternion.rotationX(angle);

        Vector3f[] vector3fs = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        float size = this.getQuadSize(partialTick);

        for(int i = 0; i < 4; ++i) {
            Vector3f vector3f = vector3fs[i];
            vector3f.rotate(quaternion);
            vector3f.mul(size);
            vector3f.add(x, y, z);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTick);

        
        buffer.vertex(vector3fs[0].x(), vector3fs[0].y(), vector3fs[0].z())
                .uv(u1, v1)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(light)
                .endVertex();
        buffer.vertex(vector3fs[1].x(), vector3fs[1].y(), vector3fs[1].z())
                .uv(u1, v0)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(light)
                .endVertex();
        buffer.vertex(vector3fs[2].x(), vector3fs[2].y(), vector3fs[2].z())
                .uv(u0, v0)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(light)
                .endVertex();
        buffer.vertex(vector3fs[3].x(), vector3fs[3].y(), vector3fs[3].z())
                .uv(u0, v1)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(light)
                .endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    
    public static class WaveSmallParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public WaveSmallParticleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new WaveSmallParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}