package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.organ.model.OrganFaceMask;

/**
 * 挖掉第一层皮肤正面那一块四边形（其余五个面照原样输出），
 * 让渲染层可以把深红底面放到模型内部 3 像素处。
 *
 * <p>判定条件：该四边形是"正面"（法线朝向 -Z）且四个顶点都落在当前部件的正面平面上
 * （头部 z=-4，躯干 z=-2）。只有 {@link OrganFaceMask#shouldHideFront()} 为真时才生效，
 * 否则一整段逻辑不介入，完全走原版渲染。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ModelPart.Cube.class)
public abstract class ModelPartCubeOrganFaceMixin {

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void epca$compileWithoutFrontFace(PoseStack.Pose pose, VertexConsumer consumer,
                                              int light, int overlay,
                                              float red, float green, float blue, float alpha,
                                              CallbackInfo ci) {
        if (!OrganFaceMask.shouldHideFront()) {
            return;
        }

        ModelPart.Cube cube = (ModelPart.Cube) (Object) this;
        float frontZ = OrganFaceMask.frontPlaneZ();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        for (ModelPart.Polygon polygon : cube.polygons) {
            if (epca$isFrontFace(polygon, frontZ)) {
                continue;
            }
            Vector3f normal = new Vector3f(polygon.normal);
            normal.mul(normalMatrix);
            float nx = normal.x();
            float ny = normal.y();
            float nz = normal.z();
            for (ModelPart.Vertex vertex : polygon.vertices) {
                consumer.vertex(matrix,
                                vertex.pos.x() / 16.0F,
                                vertex.pos.y() / 16.0F,
                                vertex.pos.z() / 16.0F)
                        .color(red, green, blue, alpha)
                        .uv(vertex.u, vertex.v)
                        .overlayCoords(overlay)
                        .uv2(light)
                        .normal(nx, ny, nz)
                        .endVertex();
            }
        }
        ci.cancel();
    }

    private static boolean epca$isFrontFace(ModelPart.Polygon polygon, float frontZ) {
        if (polygon.normal.z() > -0.5F) {
            return false;
        }
        for (ModelPart.Vertex vertex : polygon.vertices) {
            if (Math.abs(vertex.pos.z() - frontZ) > 0.01F) {
                return false;
            }
        }
        return true;
    }
}
