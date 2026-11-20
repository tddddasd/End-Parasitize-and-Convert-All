package org.tdddd.epca.impl.client.organ.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;

import java.util.List;

/**
 * 「内器官模型」渲染层（RenderLayer，非 mixin 主体）。
 *
 * <p>布局（模型单位，1 单位 = 1/16 方块；头部立方体 x/z 为 -4..4、y 为 -8..0，躯干 x 为 -4..4、
 * y 为 0..12、z 为 -2..2）：</p>
 * <ul>
 *   <li>第一层皮肤的正面窗口由 {@code ModelPartCubeOrganFaceMixin} 挖掉（不渲染）；</li>
 *   <li>本层在原来皮肤的位置重画四条「皮肤边框」（贴图 UV 取自玩家皮肤，视觉上与原皮肤一致）；</li>
 *   <li>窗口向内凹 {@link #RECESS_DEPTH} 像素：四个侧壁 + 底面用原版石头贴图铺底纹、
 *       再用顶点色乘成深红偏暗的组织质感，物品模型贴在底面上。</li>
 * </ul>
 *
 * <p>第二层皮肤（帽子 / 外套，半径更大，z=-4.5 / -2.25）在更外侧，仍正常显示；
 * 深红窗口整体位于它内部，因此不会被"盖住"也不会挡住它。</p>
 */
@OnlyIn(Dist.CLIENT)
public class OrganModelRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /**
     * 凹壁 / 底面的底纹贴图：直接使用原版石头贴图（不新增任何贴图文件），
     * 再用顶点色把它乘成深红偏暗的「内部组织」观感。
     */
    private static final ResourceLocation RECESS_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/stone.png");

    /**
     * 深红调色（乘在石头贴图上）：0.42 / 0.03 / 0.05 的纯色原先是「纯色面」，
     * 现在作为顶点色对石头贴图做乘法调色，灰石头就变成暗红组织质感。
     */
    private static final float RED_R = 0.42F;
    private static final float RED_G = 0.03F;
    private static final float RED_B = 0.05F;
    private static final float RED_A = 1.0F;

    /** UV 密度：1 个模型单位（1/16 方块）= 1 个石头贴图纹素，贴图不会被拉伸变糊。 */
    private static final float UV_PER_UNIT = 1.0F / 16.0F;

    /** 窗口向内凹陷的深度：3 像素 = 3 个模型单位。 */
    private static final float RECESS_DEPTH = 3.0F;

    // ---------------------------------------------------------------- 头部几何
    private static final float HEAD_FACE_Z = -4.0F;
    private static final float HEAD_FLOOR_Z = HEAD_FACE_Z + RECESS_DEPTH;
    private static final float HEAD_ITEM_Z = HEAD_FLOOR_Z - 0.06F;
    /** 头部正面外框（立方体正面）。 */
    private static final float HEAD_FACE_MIN_X = -4.0F;
    private static final float HEAD_FACE_MAX_X = 4.0F;
    private static final float HEAD_FACE_MIN_Y = -8.0F;
    private static final float HEAD_FACE_MAX_Y = 0.0F;
    /** 头部内器官窗口。 */
    private static final float HEAD_MIN_X = -3.5F;
    private static final float HEAD_MAX_X = 3.5F;
    private static final float HEAD_MIN_Y = -6.6F;
    private static final float HEAD_MAX_Y = -0.6F;
    /** 皮肤贴图里头部正面的 UV 原点（64×64 皮肤，头部正面占 8..16）。 */
    private static final float HEAD_UV_U = 8.0F;
    private static final float HEAD_UV_V = 8.0F;
    private static final float HEAD_ITEM_SCALE = 0.17F;

    // ---------------------------------------------------------------- 躯干几何
    private static final float TORSO_FACE_Z = -2.0F;
    private static final float TORSO_FLOOR_Z = TORSO_FACE_Z + RECESS_DEPTH;
    private static final float TORSO_ITEM_Z = TORSO_FLOOR_Z - 0.06F;
    private static final float TORSO_FACE_MIN_X = -4.0F;
    private static final float TORSO_FACE_MAX_X = 4.0F;
    private static final float TORSO_FACE_MIN_Y = 0.0F;
    private static final float TORSO_FACE_MAX_Y = 12.0F;
    private static final float TORSO_MIN_X = -3.4F;
    private static final float TORSO_MAX_X = 3.4F;
    private static final float TORSO_MIN_Y = 2.6F;
    private static final float TORSO_MAX_Y = 8.4F;
    private static final float TORSO_UV_U = 20.0F;
    private static final float TORSO_UV_V = 20.0F;
    private static final float TORSO_ITEM_SCALE = 0.085F;

    private static final float UV_DENOMINATOR = 64.0F;

    public OrganModelRenderLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player == null || player.isInvisible()) return;

        OrganModelClientData.Display display = OrganModelClientData.get(player.getUUID());
        if (!display.visible()) return;

        PlayerModel<AbstractClientPlayer> model = getParentModel();

        if (OrganModelClientData.showHead(player)) {
            renderHead(poseStack, buffer, packedLight, player, model, display.headInner());
        }
        if (OrganModelClientData.showTorso(player)) {
            renderTorso(poseStack, buffer, packedLight, player, model, display.torsoInner());
        }
        renderLimbExtensions(poseStack, buffer, packedLight, player, model, display);
    }

    // ---------------------------------------------------------------- 头部

    private void renderHead(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
                            List<ItemStack> stacks) {
        poseStack.pushPose();
        // 跟随头部旋转（看向 / 潜行时的头部摆动）。
        model.head.translateAndRotate(poseStack);

        drawSkinFrame(poseStack, buffer, packedLight, player,
                HEAD_UV_U, HEAD_UV_V,
                HEAD_FACE_MIN_X, HEAD_FACE_MAX_X, HEAD_FACE_MIN_Y, HEAD_FACE_MAX_Y,
                HEAD_MIN_X, HEAD_MAX_X, HEAD_MIN_Y, HEAD_MAX_Y,
                HEAD_FACE_Z);

        drawRecess(poseStack, buffer, packedLight,
                HEAD_MIN_X, HEAD_MAX_X, HEAD_MIN_Y, HEAD_MAX_Y,
                HEAD_FACE_Z, HEAD_FLOOR_Z);

        int columns = OrganSlotGroup.HEAD_INNER_COLUMNS;
        int rows = OrganSlotGroup.HEAD_INNER_ROWS;
        float cellWidth = (HEAD_MAX_X - HEAD_MIN_X) / columns;
        float cellHeight = (HEAD_MAX_Y - HEAD_MIN_Y) / rows;
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (stack == null || stack.isEmpty()) continue;
            int column = index % columns;
            int row = index / columns;
            float x = HEAD_MIN_X + (column + 0.5F) * cellWidth;
            float y = HEAD_MIN_Y + (row + 0.5F) * cellHeight;
            renderItem(poseStack, buffer, packedLight, player, stack,
                    x, y, HEAD_ITEM_Z, HEAD_ITEM_SCALE, 0.0F, 0.0F);
        }

        poseStack.popPose();
    }

    // ---------------------------------------------------------------- 躯干

    private void renderTorso(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                             AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
                             List<ItemStack> stacks) {
        poseStack.pushPose();
        // 跟随身体（潜行 / 游泳时的躯干旋转）。
        model.body.translateAndRotate(poseStack);

        drawSkinFrame(poseStack, buffer, packedLight, player,
                TORSO_UV_U, TORSO_UV_V,
                TORSO_FACE_MIN_X, TORSO_FACE_MAX_X, TORSO_FACE_MIN_Y, TORSO_FACE_MAX_Y,
                TORSO_MIN_X, TORSO_MAX_X, TORSO_MIN_Y, TORSO_MAX_Y,
                TORSO_FACE_Z);

        drawRecess(poseStack, buffer, packedLight,
                TORSO_MIN_X, TORSO_MAX_X, TORSO_MIN_Y, TORSO_MAX_Y,
                TORSO_FACE_Z, TORSO_FLOOR_Z);

        int columns = OrganSlotGroup.TORSO_INNER_COLUMNS;
        int rows = OrganSlotGroup.TORSO_INNER_ROWS;
        float cellWidth = (TORSO_MAX_X - TORSO_MIN_X) / columns;
        float cellHeight = (TORSO_MAX_Y - TORSO_MIN_Y) / rows;
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (stack == null || stack.isEmpty()) continue;
            int column = index % columns;
            int row = index / columns;
            float x = TORSO_MIN_X + (column + 0.5F) * cellWidth;
            float y = TORSO_MIN_Y + (row + 0.5F) * cellHeight;
            renderItem(poseStack, buffer, packedLight, player, stack,
                    x, y, TORSO_ITEM_Z, TORSO_ITEM_SCALE, 0.0F, 0.0F);
        }

        poseStack.popPose();
    }

    // ---------------------------------------------------------------- 四肢扩展点

    /**
     * 四肢内器官的"外部显示"预留接口：通过 {@link OrganLimbModelExtension} 查询额外部件，
     * 默认没有任何 Provider → 不绘制任何东西，四肢外观保持原版。
     */
    private void renderLimbExtensions(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
                                      OrganModelClientData.Display display) {
        for (OrganSlotGroup group : OrganSlotGroup.LIMB_INNER_GROUPS) {
            List<ItemStack> stacks = display.limb(group);
            List<OrganLimbModelExtension.ExtraPart> parts =
                    OrganLimbModelExtension.extraParts(player, group, stacks);
            if (parts.isEmpty()) continue;
            ModelPart part = limbPart(model, group);
            if (part == null) continue;

            poseStack.pushPose();
            part.translateAndRotate(poseStack);
            for (OrganLimbModelExtension.ExtraPart extra : parts) {
                if (extra == null) continue;
                renderItem(poseStack, buffer, packedLight, player, extra.stack(),
                        extra.x(), extra.y(), extra.z(), extra.scale(),
                        extra.yawDegrees(), extra.pitchDegrees());
            }
            poseStack.popPose();
        }
    }

    private static ModelPart limbPart(PlayerModel<?> model, OrganSlotGroup group) {
        if (group == OrganSlotGroup.LEFT_ARM) return model.leftArm;
        if (group == OrganSlotGroup.RIGHT_ARM) return model.rightArm;
        if (group == OrganSlotGroup.LEFT_LEG) return model.leftLeg;
        if (group == OrganSlotGroup.RIGHT_LEG) return model.rightLeg;
        return null;
    }

    // ---------------------------------------------------------------- 皮肤边框

    /**
     * 在被挖掉的第一层皮肤正面位置，重画四条「皮肤边框」（上/下/左/右），
     * 用玩家皮肤贴图的对应 UV，视觉上与原版皮肤一致，只留下中间的窗口。
     */
    private static void drawSkinFrame(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      AbstractClientPlayer player,
                                      float uvU, float uvV,
                                      float faceMinX, float faceMaxX,
                                      float faceMinY, float faceMaxY,
                                      float winMinX, float winMaxX,
                                      float winMinY, float winMaxY,
                                      float faceZ) {
        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));
        // 稍微往外一点点，避免与立方体侧壁的前沿共面。
        float z = faceZ - 0.02F;

        // 上边框 / 下边框
        skinQuad(consumer, poseStack, packedLight, z,
                faceMinX, faceMaxX, faceMinY, winMinY,
                uvU, uvV, faceMinX, faceMinY);
        skinQuad(consumer, poseStack, packedLight, z,
                faceMinX, faceMaxX, winMaxY, faceMaxY,
                uvU, uvV, faceMinX, faceMinY);
        // 左边框 / 右边框
        skinQuad(consumer, poseStack, packedLight, z,
                faceMinX, winMinX, winMinY, winMaxY,
                uvU, uvV, faceMinX, faceMinY);
        skinQuad(consumer, poseStack, packedLight, z,
                winMaxX, faceMaxX, winMinY, winMaxY,
                uvU, uvV, faceMinX, faceMinY);
    }

    private static void skinQuad(VertexConsumer consumer, PoseStack poseStack, int packedLight, float z,
                                 float x0, float x1, float y0, float y1,
                                 float uvU, float uvV, float faceMinX, float faceMinY) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Vector3f normal = new Vector3f(0.0F, 0.0F, -1.0F);
        normal.mul(normalMatrix);

        float u0 = (uvU + (x0 - faceMinX)) / UV_DENOMINATOR;
        float u1 = (uvU + (x1 - faceMinX)) / UV_DENOMINATOR;
        float v0 = (uvV + (y0 - faceMinY)) / UV_DENOMINATOR;
        float v1 = (uvV + (y1 - faceMinY)) / UV_DENOMINATOR;

        skinVertex(consumer, matrix, normal, packedLight, x0, y0, z, u0, v0);
        skinVertex(consumer, matrix, normal, packedLight, x0, y1, z, u0, v1);
        skinVertex(consumer, matrix, normal, packedLight, x1, y1, z, u1, v1);
        skinVertex(consumer, matrix, normal, packedLight, x1, y0, z, u1, v0);
    }

    private static void skinVertex(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                                   int packedLight, float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x / 16.0F, y / 16.0F, z / 16.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    // ---------------------------------------------------------------- 凹陷内壁

    /**
     * 凹陷：窗口四周四个侧壁 + 深处的底面，全部用<b>原版石头贴图</b>铺底纹，
     * 再用顶点色 {@code (0.42, 0.03, 0.05)} 乘成深红偏暗的组织质感。
     *
     * <p>RenderType 用 {@link RenderType#entitySolid(ResourceLocation)}：带贴图、可被顶点色调制、
     * 走 lightmap（用当前 {@code packedLight}），配合 {@code OverlayTexture.NO_OVERLAY}。
     * UV 按「1 模型单位 = 1 纹素」映射（{@link #UV_PER_UNIT}），各面之间共用同一套缩放，
     * 贴图不会被拉伸；每个四边形正反两种绕序各画一次，避免被剔除掉。</p>
     */
    private static void drawRecess(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                   float minX, float maxX, float minY, float maxY,
                                   float zFace, float zFloor) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entitySolid(RECESS_TEXTURE));

        float uSpan = (maxX - minX) * UV_PER_UNIT;
        float vSpan = (maxY - minY) * UV_PER_UNIT;
        float depthSpan = (zFloor - zFace) * UV_PER_UNIT;

        // 上壁（y = minY）：u 沿 x，v 沿凹陷深度；与底面的 y=minY 边接缝对得上
        texturedQuad(consumer, poseStack, packedLight,
                minX, minY, zFace, 0.0F, 0.0F,
                maxX, minY, zFace, uSpan, 0.0F,
                maxX, minY, zFloor, uSpan, depthSpan,
                minX, minY, zFloor, 0.0F, depthSpan);
        // 下壁（y = maxY）
        texturedQuad(consumer, poseStack, packedLight,
                minX, maxY, zFace, 0.0F, vSpan,
                maxX, maxY, zFace, uSpan, vSpan,
                maxX, maxY, zFloor, uSpan, vSpan + depthSpan,
                minX, maxY, zFloor, 0.0F, vSpan + depthSpan);
        // 左壁（x = minX）：u 沿凹陷深度，v 沿 y
        texturedQuad(consumer, poseStack, packedLight,
                minX, minY, zFace, 0.0F, 0.0F,
                minX, maxY, zFace, 0.0F, vSpan,
                minX, maxY, zFloor, depthSpan, vSpan,
                minX, minY, zFloor, depthSpan, 0.0F);
        // 右壁（x = maxX）
        texturedQuad(consumer, poseStack, packedLight,
                maxX, minY, zFace, 0.0F, 0.0F,
                maxX, maxY, zFace, 0.0F, vSpan,
                maxX, maxY, zFloor, depthSpan, vSpan,
                maxX, minY, zFloor, depthSpan, 0.0F);
        // 底面（z = zFloor）：u 沿 x，v 沿 y
        texturedQuad(consumer, poseStack, packedLight,
                minX, minY, zFloor, 0.0F, 0.0F,
                maxX, minY, zFloor, uSpan, 0.0F,
                maxX, maxY, zFloor, uSpan, vSpan,
                minX, maxY, zFloor, 0.0F, vSpan);
    }

    /**
     * 画一个带石头贴图 + 深红顶点色的四边形（正反两种绕序各一次，避免受剔除状态影响）。
     */
    private static void texturedQuad(VertexConsumer consumer, PoseStack poseStack, int packedLight,
                                     float x1, float y1, float z1, float u1, float v1,
                                     float x2, float y2, float z2, float u2, float v2,
                                     float x3, float y3, float z3, float u3, float v3,
                                     float x4, float y4, float z4, float u4, float v4) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Vector3f normal = new Vector3f(0.0F, 0.0F, -1.0F);
        normal.mul(normalMatrix);

        texturedVertex(consumer, matrix, normal, packedLight, x1, y1, z1, u1, v1);
        texturedVertex(consumer, matrix, normal, packedLight, x2, y2, z2, u2, v2);
        texturedVertex(consumer, matrix, normal, packedLight, x3, y3, z3, u3, v3);
        texturedVertex(consumer, matrix, normal, packedLight, x4, y4, z4, u4, v4);

        texturedVertex(consumer, matrix, normal, packedLight, x4, y4, z4, u4, v4);
        texturedVertex(consumer, matrix, normal, packedLight, x3, y3, z3, u3, v3);
        texturedVertex(consumer, matrix, normal, packedLight, x2, y2, z2, u2, v2);
        texturedVertex(consumer, matrix, normal, packedLight, x1, y1, z1, u1, v1);
    }

    private static void texturedVertex(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                                       int packedLight,
                                       float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x / 16.0F, y / 16.0F, z / 16.0F)
                .color(RED_R, RED_G, RED_B, RED_A)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    // ---------------------------------------------------------------- 物品

    /**
     * 在当前位置渲染一个物品模型。
     *
     * <p>坐标是所在 ModelPart 的本地单位（1/16 方块），这里统一换算成方块后 translate。
     * 实体渲染时 PoseStack 已经被 scale(-1, -1, 1) 翻转，这里再乘一次 (-1, -1, 1)
     * （等价于绕 Z 轴 180°，行列式为 +1，不会把模型翻成反面），物品就不会上下颠倒、左右镜像。</p>
     */
    private static void renderItem(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                   AbstractClientPlayer player, ItemStack stack,
                                   float x, float y, float z, float scale,
                                   float yawDegrees, float pitchDegrees) {
        if (stack == null || stack.isEmpty() || scale <= 0.0F) return;

        poseStack.pushPose();
        poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        poseStack.scale(scale, scale, scale);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F + yawDegrees));
        if (pitchDegrees != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(pitchDegrees));
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                player.level(), player.getId());
        poseStack.popPose();
    }
}
