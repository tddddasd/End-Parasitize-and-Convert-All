package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
 *
 * <p><b>26 API 差异</b>：{@code ModelPart.Cube#compile} 变成
 * {@code (PoseStack.Pose, VertexConsumer, int lightCoords, int overlayCoords, int color)}，
 * 原来 4 个 float 的 RGBA 合并成一个 {@code int color}；{@code Polygon} / {@code Vertex} 变成
 * <b>public record</b>、{@code Cube#polygons} 也已经是 public 字段，所以**不再需要 accesstransformer**；
 * 顶点改用 {@code VertexConsumer.addVertex(x,y,z,color,u,v,overlay,light,nx,ny,nz)} 的链式写法。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ModelPart.Cube.class)
public abstract class ModelPartCubeOrganFaceMixin {

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true, require = 0)
    private void epca$compileWithoutFrontFace(PoseStack.Pose pose, VertexConsumer consumer,
                                              int lightCoords, int overlayCoords, int color,
                                              CallbackInfo ci) {
        if (!OrganFaceMask.shouldHideFront()) {
            return;
        }

        ModelPart.Cube cube = (ModelPart.Cube) (Object) this;
        float frontZ = OrganFaceMask.frontPlaneZ();
        Vector3f scratch = new Vector3f();

        for (ModelPart.Polygon polygon : cube.polygons) {
            if (polygon == null) continue;
            if (epca$isFrontFace(polygon, frontZ)) {
                continue;
            }
            Vector3f normal = pose.transformNormal(polygon.normal(), scratch);
            float nx = normal.x();
            float ny = normal.y();
            float nz = normal.z();
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                consumer.addVertex(pose, vertex.worldX(), vertex.worldY(), vertex.worldZ())
                        .setColor(color)
                        .setUv(vertex.u(), vertex.v())
                        .setOverlay(overlayCoords)
                        .setLight(lightCoords)
                        .setNormal(nx, ny, nz);
            }
        }
        ci.cancel();
    }

    private static boolean epca$isFrontFace(ModelPart.Polygon polygon, float frontZ) {
        if (polygon.normal() == null || polygon.normal().z() > -0.5F) {
            return false;
        }
        for (ModelPart.Vertex vertex : polygon.vertices()) {
            if (Math.abs(vertex.z() - frontZ) > 0.01F) {
                return false;
            }
        }
        return true;
    }
}
