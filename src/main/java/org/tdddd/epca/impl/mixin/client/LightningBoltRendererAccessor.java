package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LightningBoltRenderer.class)
public interface LightningBoltRendererAccessor {

    @Invoker("quad")
    static void epca$quad(Matrix4f matrix, VertexConsumer consumer,
                          float x1, float z1, int segment, float x2, float z2,
                          float r, float g, float b, float offset1, float offset2,
                          boolean flag1, boolean flag2, boolean flag3, boolean flag4) {
        throw new AssertionError();
    }
}