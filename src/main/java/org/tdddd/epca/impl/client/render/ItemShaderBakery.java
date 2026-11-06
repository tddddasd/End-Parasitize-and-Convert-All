package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3fc;

public final class ItemShaderBakery {

    public static final int VERTEX_STRIDE = 5;

    public static final int VERTICES_PER_QUAD = 4;

    public static final float OVERLAY_INFLATE = 1.001f;

    private ItemShaderBakery() {
    }

    public static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                int frames, int frame,
                                float tintR, float tintG, float tintB,
                                int intensity16, int split16, float animClock) {
        if (quad == null) {
            return;
        }

        float uMin = 0.0f;
        float vMin = 0.0f;
        float uSpan = 1.0f;
        float vSpan = 1.0f;
        BakedQuad.MaterialInfo material = quad.materialInfo();
        TextureAtlasSprite sprite = material == null ? null : material.sprite();
        if (sprite != null) {
            float su = sprite.getU0();
            float sv = sprite.getV0();
            float du = sprite.getU1() - su;
            float dv = sprite.getV1() - sv;
            if (du > 1.0e-6f && dv > 1.0e-6f) {
                uMin = su;
                vMin = sv;
                uSpan = du;
                vSpan = dv;
            }
        }

        int safeFrames = Math.max(1, frames);
        float vScale = 1.0f / safeFrames;
        float vOffset = Math.max(0, frame) * vScale;

        float cx = 0.0f;
        float cy = 0.0f;
        float cz = 0.0f;
        for (int i = 0; i < VERTICES_PER_QUAD; i++) {
            Vector3fc p = quad.position(i);
            cx += p.x();
            cy += p.y();
            cz += p.z();
        }
        cx /= VERTICES_PER_QUAD;
        cy /= VERTICES_PER_QUAD;
        cz /= VERTICES_PER_QUAD;

        for (int i = 0; i < VERTICES_PER_QUAD; i++) {
            Vector3fc p = quad.position(i);
            long packed = quad.packedUV(i);

            float u = (UVPair.unpackU(packed) - uMin) / uSpan;
            float v = (UVPair.unpackV(packed) - vMin) / vSpan;

            float x = cx + (p.x() - cx) * OVERLAY_INFLATE;
            float y = cy + (p.y() - cy) * OVERLAY_INFLATE;
            float z = cz + (p.z() - cz) * OVERLAY_INFLATE;

            consumer.addVertex(pose, x, y, z)
                    .setColor(tintR, tintG, tintB, 1.0f)
                    .setUv(u, vOffset + v * vScale)
                    .setUv1(intensity16, split16)
                    .setLineWidth(animClock);
        }
    }
}
