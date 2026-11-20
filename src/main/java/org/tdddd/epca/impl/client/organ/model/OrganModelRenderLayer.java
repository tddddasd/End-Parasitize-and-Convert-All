package org.tdddd.epca.impl.client.organ.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;

import java.util.List;
import java.util.UUID;

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
 * <p><b>26 API 说明</b>：实体渲染改成 SubmitNodeCollector 延迟提交，所以自定义顶点走
 * {@code submitCustomGeometry}、物品走 {@link ItemStackRenderState#submit}；
 * {@code RenderType.entitySolid} 在 26 里搬到了 {@code net.minecraft.client.renderer.rendertype.RenderTypes}。</p>
 */
@OnlyIn(Dist.CLIENT)
public class OrganModelRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    /**
     * 凹壁 / 底面的底纹贴图：直接使用原版石头贴图（不新增任何贴图文件），
     * 再用顶点色把它乘成深红偏暗的「内部组织」观感。
     */
    private static final Identifier RECESS_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/block/stone.png");

    /**
     * 深红调色（乘在石头贴图上）：灰石头就变成暗红组织质感。
     */
    private static final int RED_TINT = 0xFF6B080D;

    /** UV 密度：1 个模型单位（1/16 方块）= 1 个石头贴图纹素。 */
    private static final float UV_PER_UNIT = 1.0F / 16.0F;

    /** 窗口向内凹陷的深度：3 像素 = 3 个模型单位。 */
    private static final float RECESS_DEPTH = 3.0F;

    // ---------------------------------------------------------------- 头部几何
    private static final float HEAD_FACE_Z = -4.0F;
    private static final float HEAD_FLOOR_Z = HEAD_FACE_Z + RECESS_DEPTH;
    private static final float HEAD_ITEM_Z = HEAD_FLOOR_Z - 0.06F;
    private static final float HEAD_FACE_MIN_X = -4.0F;
    private static final float HEAD_FACE_MAX_X = 4.0F;
    private static final float HEAD_FACE_MIN_Y = -8.0F;
    private static final float HEAD_FACE_MAX_Y = 0.0F;
    private static final float HEAD_MIN_X = -3.5F;
    private static final float HEAD_MAX_X = 3.5F;
    private static final float HEAD_MIN_Y = -6.6F;
    private static final float HEAD_MAX_Y = -0.6F;
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
            RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state == null || state.isInvisible) return;

        UUID playerId = OrganFaceMask.playerId(state);
        if (playerId == null) return;
        Player player = resolvePlayer(state);

        OrganModelClientData.Display display = OrganModelClientData.get(playerId);
        if (!display.visible()) return;

        PlayerModel model = getParentModel();
        Identifier skin = state.skin == null ? null : state.skin.body().texturePath();

        if (OrganModelClientData.showHead(playerId)) {
            submitHead(poseStack, collector, lightCoords, player, model, skin, display.headInner());
        }
        if (OrganModelClientData.showTorso(playerId)) {
            submitTorso(poseStack, collector, lightCoords, player, model, skin, display.torsoInner());
        }
        submitLimbExtensions(poseStack, collector, lightCoords, player, model, display, playerId);
    }

    private static Player resolvePlayer(AvatarRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        Entity entity = minecraft.level.getEntity(state.id);
        return entity instanceof Player player ? player : null;
    }

    // ---------------------------------------------------------------- 头部

    private void submitHead(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                            Player player, PlayerModel model, Identifier skin,
                            List<ItemStack> stacks) {
        poseStack.pushPose();
        // 跟随头部旋转（看向 / 潜行时的头部摆动）。
        model.head.translateAndRotate(poseStack);

        if (skin != null) {
            drawSkinFrame(poseStack, collector, lightCoords, skin,
                    HEAD_UV_U, HEAD_UV_V,
                    HEAD_FACE_MIN_X, HEAD_FACE_MAX_X, HEAD_FACE_MIN_Y, HEAD_FACE_MAX_Y,
                    HEAD_MIN_X, HEAD_MAX_X, HEAD_MIN_Y, HEAD_MAX_Y,
                    HEAD_FACE_Z);
        }
        drawRecess(poseStack, collector, lightCoords,
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
            submitItem(poseStack, collector, lightCoords, player, stack,
                    x, y, HEAD_ITEM_Z, HEAD_ITEM_SCALE, 0.0F, 0.0F);
        }

        poseStack.popPose();
    }

    // ---------------------------------------------------------------- 躯干

    private void submitTorso(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                             Player player, PlayerModel model, Identifier skin,
                             List<ItemStack> stacks) {
        poseStack.pushPose();
        // 跟随身体（潜行 / 游泳时的躯干旋转）。
        model.body.translateAndRotate(poseStack);

        if (skin != null) {
            drawSkinFrame(poseStack, collector, lightCoords, skin,
                    TORSO_UV_U, TORSO_UV_V,
                    TORSO_FACE_MIN_X, TORSO_FACE_MAX_X, TORSO_FACE_MIN_Y, TORSO_FACE_MAX_Y,
                    TORSO_MIN_X, TORSO_MAX_X, TORSO_MIN_Y, TORSO_MAX_Y,
                    TORSO_FACE_Z);
        }
        drawRecess(poseStack, collector, lightCoords,
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
            submitItem(poseStack, collector, lightCoords, player, stack,
                    x, y, TORSO_ITEM_Z, TORSO_ITEM_SCALE, 0.0F, 0.0F);
        }

        poseStack.popPose();
    }

    // ---------------------------------------------------------------- 四肢扩展点

    /**
     * 四肢内器官的"外部显示"预留接口：通过 {@link OrganLimbModelExtension} 查询额外部件，
     * 默认没有任何 Provider → 不绘制任何东西，四肢外观保持原版。
     */
    private void submitLimbExtensions(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                      Player player, PlayerModel model,
                                      OrganModelClientData.Display display, UUID playerId) {
        for (OrganSlotGroup group : OrganSlotGroup.LIMB_INNER_GROUPS) {
            List<ItemStack> stacks = display.limb(group);
            List<OrganLimbModelExtension.ExtraPart> parts =
                    OrganLimbModelExtension.extraParts(playerId, group, stacks);
            if (parts.isEmpty()) continue;
            ModelPart part = limbPart(model, group);
            if (part == null) continue;

            poseStack.pushPose();
            part.translateAndRotate(poseStack);
            for (OrganLimbModelExtension.ExtraPart extra : parts) {
                if (extra == null) continue;
                submitItem(poseStack, collector, lightCoords, player, extra.stack(),
                        extra.x(), extra.y(), extra.z(), extra.scale(),
                        extra.yawDegrees(), extra.pitchDegrees());
            }
            poseStack.popPose();
        }
    }

    private static ModelPart limbPart(PlayerModel model, OrganSlotGroup group) {
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
    private static void drawSkinFrame(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                      Identifier skin, float uvU, float uvV,
                                      float faceMinX, float faceMaxX,
                                      float faceMinY, float faceMaxY,
                                      float winMinX, float winMaxX,
                                      float winMinY, float winMaxY,
                                      float faceZ) {
        RenderType renderType = RenderTypes.entityCutout(skin);
        // 稍微往外一点点，避免与立方体侧壁的前沿共面。
        float z = faceZ - 0.02F;

        collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            // 上边框 / 下边框
            skinQuad(consumer, pose, lightCoords, 0xFFFFFFFF, z,
                    faceMinX, faceMaxX, faceMinY, winMinY,
                    uvU, uvV, faceMinX, faceMinY);
            skinQuad(consumer, pose, lightCoords, 0xFFFFFFFF, z,
                    faceMinX, faceMaxX, winMaxY, faceMaxY,
                    uvU, uvV, faceMinX, faceMinY);
            // 左边框 / 右边框
            skinQuad(consumer, pose, lightCoords, 0xFFFFFFFF, z,
                    faceMinX, winMinX, winMinY, winMaxY,
                    uvU, uvV, faceMinX, faceMinY);
            skinQuad(consumer, pose, lightCoords, 0xFFFFFFFF, z,
                    winMaxX, faceMaxX, winMinY, winMaxY,
                    uvU, uvV, faceMinX, faceMinY);
        });
    }

    private static void skinQuad(VertexConsumer consumer, PoseStack.Pose pose, int lightCoords,
                                 int color, float z,
                                 float x0, float x1, float y0, float y1,
                                 float uvU, float uvV, float faceMinX, float faceMinY) {
        float u0 = (uvU + (x0 - faceMinX)) / UV_DENOMINATOR;
        float u1 = (uvU + (x1 - faceMinX)) / UV_DENOMINATOR;
        float v0 = (uvV + (y0 - faceMinY)) / UV_DENOMINATOR;
        float v1 = (uvV + (y1 - faceMinY)) / UV_DENOMINATOR;

        skinVertex(consumer, pose, lightCoords, color, x0, y0, z, u0, v0);
        skinVertex(consumer, pose, lightCoords, color, x0, y1, z, u0, v1);
        skinVertex(consumer, pose, lightCoords, color, x1, y1, z, u1, v1);
        skinVertex(consumer, pose, lightCoords, color, x1, y0, z, u1, v0);

        // 反绕序再画一次，避免受 RenderType 的剔除状态影响。
        skinVertex(consumer, pose, lightCoords, color, x1, y0, z, u1, v0);
        skinVertex(consumer, pose, lightCoords, color, x1, y1, z, u1, v1);
        skinVertex(consumer, pose, lightCoords, color, x0, y1, z, u0, v1);
        skinVertex(consumer, pose, lightCoords, color, x0, y0, z, u0, v0);
    }

    private static void skinVertex(VertexConsumer consumer, PoseStack.Pose pose, int lightCoords, int color,
                                   float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x / 16.0F, y / 16.0F, z / 16.0F)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    // ---------------------------------------------------------------- 凹陷内壁

    /**
     * 凹陷：窗口四周四个侧壁 + 深处的底面，全部用<b>原版石头贴图</b>铺底纹，
     * 再用顶点色 {@link #RED_TINT} 乘成深红偏暗的组织质感。
     */
    private static void drawRecess(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                   float minX, float maxX, float minY, float maxY,
                                   float zFace, float zFloor) {
        RenderType renderType = RenderTypes.entitySolid(RECESS_TEXTURE);

        float uSpan = (maxX - minX) * UV_PER_UNIT;
        float vSpan = (maxY - minY) * UV_PER_UNIT;
        float depthSpan = (zFloor - zFace) * UV_PER_UNIT;

        collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            // 上壁（y = minY）
            texturedQuad(consumer, pose, lightCoords, minX, minY, zFace, 0.0F, 0.0F,
                    maxX, minY, zFace, uSpan, 0.0F,
                    maxX, minY, zFloor, uSpan, depthSpan,
                    minX, minY, zFloor, 0.0F, depthSpan);
            // 下壁（y = maxY）
            texturedQuad(consumer, pose, lightCoords, minX, maxY, zFace, 0.0F, vSpan,
                    maxX, maxY, zFace, uSpan, vSpan,
                    maxX, maxY, zFloor, uSpan, vSpan + depthSpan,
                    minX, maxY, zFloor, 0.0F, vSpan + depthSpan);
            // 左壁（x = minX）
            texturedQuad(consumer, pose, lightCoords, minX, minY, zFace, 0.0F, 0.0F,
                    minX, maxY, zFace, 0.0F, vSpan,
                    minX, maxY, zFloor, depthSpan, vSpan,
                    minX, minY, zFloor, depthSpan, 0.0F);
            // 右壁（x = maxX）
            texturedQuad(consumer, pose, lightCoords, maxX, minY, zFace, 0.0F, 0.0F,
                    maxX, maxY, zFace, 0.0F, vSpan,
                    maxX, maxY, zFloor, depthSpan, vSpan,
                    maxX, minY, zFloor, depthSpan, 0.0F);
            // 底面（z = zFloor）
            texturedQuad(consumer, pose, lightCoords, minX, minY, zFloor, 0.0F, 0.0F,
                    maxX, minY, zFloor, uSpan, 0.0F,
                    maxX, maxY, zFloor, uSpan, vSpan,
                    minX, maxY, zFloor, 0.0F, vSpan);
        });
    }

    /**
     * 画一个带石头贴图 + 深红顶点色的四边形（正反两种绕序各一次，避免受剔除状态影响）。
     */
    private static void texturedQuad(VertexConsumer consumer, PoseStack.Pose pose, int lightCoords,
                                     float x1, float y1, float z1, float u1, float v1,
                                     float x2, float y2, float z2, float u2, float v2,
                                     float x3, float y3, float z3, float u3, float v3,
                                     float x4, float y4, float z4, float u4, float v4) {
        texturedVertex(consumer, pose, lightCoords, x1, y1, z1, u1, v1);
        texturedVertex(consumer, pose, lightCoords, x2, y2, z2, u2, v2);
        texturedVertex(consumer, pose, lightCoords, x3, y3, z3, u3, v3);
        texturedVertex(consumer, pose, lightCoords, x4, y4, z4, u4, v4);

        texturedVertex(consumer, pose, lightCoords, x4, y4, z4, u4, v4);
        texturedVertex(consumer, pose, lightCoords, x3, y3, z3, u3, v3);
        texturedVertex(consumer, pose, lightCoords, x2, y2, z2, u2, v2);
        texturedVertex(consumer, pose, lightCoords, x1, y1, z1, u1, v1);
    }

    private static void texturedVertex(VertexConsumer consumer, PoseStack.Pose pose, int lightCoords,
                                       float x, float y, float z, float u, float v) {
        Vector3f normal = pose.transformNormal(0.0F, 0.0F, -1.0F, new Vector3f());
        consumer.addVertex(pose, x / 16.0F, y / 16.0F, z / 16.0F)
                .setColor(RED_TINT)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(normal.x(), normal.y(), normal.z());    }

    // ---------------------------------------------------------------- 物品

    /**
     * 在当前位置渲染一个物品模型。
     *
     * <p>坐标是所在 ModelPart 的本地单位（1/16 方块），这里统一换算成方块后 translate。
     * 实体渲染时 PoseStack 已经被 scale(-1, -1, 1) 翻转，这里再乘一次 (-1, -1, 1)
     * （等价于绕 Z 轴 180°，行列式为 +1），物品就不会上下颠倒、左右镜像。</p>
     */
    private static void submitItem(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                   Player player, ItemStack stack,
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

        ItemStackRenderState itemState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver()
                .updateForLiving(itemState, stack, ItemDisplayContext.FIXED, player);
        itemState.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
