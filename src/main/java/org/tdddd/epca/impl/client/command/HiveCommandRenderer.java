package org.tdddd.epca.impl.client.command;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderClientCache;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandHighlight;
import org.tdddd.epca.impl.overworld.data.command.HiveRoute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 指挥系统的世界内渲染（客户端，仅领巢者可见）。
 *
 * <h2>画法（严格区分“线”和“面”）</h2>
 * <ul>
 *     <li><b>被选中的寄生体 / 被指定攻击的目标：纯线框。</b>
 *     用 {@code RenderType.LINES}（{@code DefaultVertexFormat.POSITION_COLOR_NORMAL}，
 *     {@code VertexFormat.Mode.LINES}）逐条写顶点，<b>只画 AABB 的 12 条棱边</b>：
 *     底面 4 条 + 顶面 4 条 + 4 条竖棱。没有任何四边形，所以不会出现“实心面/半透明体块”。
 *     AABB 外扩 {@link #EDGE_INFLATE} 格避免与实体表面 z-fighting。</li>
 *     <li><b>路线节点：紫色填充小方块 + 紫色线框描边。</b>填充是需求原文要求的，
 *     用 {@code RenderType.debugQuads()}（{@code POSITION_COLOR} 四边面）画 6 个面；
 *     描边仍走 {@code RenderType.LINES} 的 12 条棱。</li>
 *     <li><b>路线连线：面向摄像机的带状四边形（billboard ribbon），不是 GL 线。</b>
 *     相邻节点中心之间铺一条有宽度的带子（普通路线带宽 0.07 格、编辑中 0.09 格），
 *     侧向量取 {@code normalize(cross(线段方向, 视线方向))}，所以四边形平面永远包含线段且正对镜头，
 *     从任何角度看都是“有宽度的一条带”，不会退化成 1px 细线，也不会从侧面看成一条缝。
 *     首尾相连时补一条闭合段；每段两端各延长半个带宽，折角处相互重叠并埋进节点方块里，不留缝隙。</li>
 *     <li><b>路线库全画：</b>普通路线 = 暗紫（节点 0.2）；自己“正在编辑”的那条
 *     = 亮紫（连线更宽、节点 0.26、填充更实），并且最后画，视觉上盖在其它路线上面。</li>
 * </ul>
 *
 * <h2>为什么路线连线不用 {@code RenderType.LINES} / {@code lineWidth}</h2>
 * <p>很多显卡/驱动在核心 profile 下把 {@code GL_LINES} 的线宽强制夹到 1.0（{@code lineWidth} 调用被忽略），
 * 而 {@code RenderType.LINES} 底层就是 {@code GL_LINES}，于是路线连线只剩 1px、几乎看不见；
 * 在 {@code disableDepthTest}（透墙可见）下再和节点方块叠加，观感更糟。
 * 所以连线改成 {@code RenderType.debugQuads()}（{@code POSITION_COLOR}）里的实几何；
 * 节点填充也用同一个批次，靠顶点写入顺序保证“先画带子、后画节点填充”。</p>
 *
 * <p>状态：{@code disableDepthTest}（透墙可见，符合原设计）、{@code depthMask(false)}、
 * {@code enableBlend}；每批结束都 {@code endBatch} 并在 finally 里恢复
 * depthTest / depthMask / lineWidth，异常也不会污染后续渲染。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class HiveCommandRenderer {

    /** 实体描边的最大可见距离（格）。 */
    private static final double ENTITY_VIEW_DISTANCE = 192.0D;
    /** 路线起点的最大可见距离（格）。 */
    private static final double ROUTE_VIEW_DISTANCE = 256.0D;
    /** 普通路线节点的半边长度。 */
    private static final double NODE_HALF = 0.2D;
    /** 正在编辑的路线节点半边长度（更大一点，好认）。 */
    private static final double NODE_HALF_EDITING = 0.26D;
    /** 线框 AABB 外扩量：略大于碰撞箱，避免和实体表面 z-fighting。 */
    private static final double EDGE_INFLATE = 0.02D;
    /** 普通线宽（仅用于 GL_LINES 的线框：节点描边、实体描边；很多驱动会忽略，别指望它）。 */
    private static final float LINE_WIDTH = 2.0F;
    /**
     * 普通路线连线的半宽（格）。带宽 = 0.07 格：比节点方块（0.2）细得多，但远粗于 1px 的 GL 线。
     * 连线是有实际宽度的几何，不受 {@code lineWidth} 被驱动夹成 1.0 的影响。
     */
    private static final double ROUTE_RIBBON_HALF = 0.035D;
    /** 正在编辑的路线连线半宽（格）：带宽 0.09 格，比普通路线更粗更好认。 */
    private static final double EDITING_RIBBON_HALF = 0.045D;

    private static final float OWN_PARASITE_R = 0.15F;
    private static final float OWN_PARASITE_G = 0.95F;
    private static final float OWN_PARASITE_B = 0.35F;
    private static final float OTHER_PARASITE_R = 0.25F;
    private static final float OTHER_PARASITE_G = 0.55F;
    private static final float OTHER_PARASITE_B = 1.0F;
    private static final float OWN_TARGET_R = 0.95F;
    private static final float OWN_TARGET_G = 0.20F;
    private static final float OWN_TARGET_B = 0.15F;
    private static final float OTHER_TARGET_R = 1.0F;
    private static final float OTHER_TARGET_G = 0.60F;
    private static final float OTHER_TARGET_B = 0.10F;
    /** 普通路线（暗紫）。 */
    private static final float ROUTE_R = 0.62F;
    private static final float ROUTE_G = 0.22F;
    private static final float ROUTE_B = 0.92F;
    /** 正在编辑的路线（亮紫）。 */
    private static final float EDITING_R = 0.85F;
    private static final float EDITING_G = 0.48F;
    private static final float EDITING_B = 1.0F;
    /** 普通路线填充/描边透明度。 */
    private static final float ROUTE_FILL_ALPHA = 0.55F;
    private static final float ROUTE_LINE_ALPHA = 0.95F;
    /** 编辑中路线填充透明度。 */
    private static final float EDITING_FILL_ALPHA = 0.78F;

    private HiveCommandRenderer() {
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        // 身份守卫（两条取并集，任一条成立就画）：
        //   1) 领巢者名单缓存里有自己 —— 登录 / 换维度时服务端会补发（HiveLeaderManager.syncTo）；
        //   2) 收到过指挥同步包 —— 服务端只发给领巢者，不依赖登录时序。
        // 被撤销时两条会同时失效：SyncHiveLeadersPacket 整表替换后本地玩家不在名单里，
        // HiveLeaderClientCache.updateLeaders 会顺手 HiveCommandClientData.clear() 把 receivedSync
        // 复位，于是本会话内立刻停画（不等下一次同步、更不会画到下次登录）。
        if (!HiveLeaderClientCache.isHiveLeader(minecraft.player.getUUID())
                && !HiveCommandClientData.hasReceivedSync()) {
            return;
        }
        List<HiveCommandHighlight> entries = HiveCommandClientData.entries();
        List<HiveRoute> routes = HiveCommandClientData.routes();
        if (entries.isEmpty() && routes.isEmpty()) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();
        UUID self = minecraft.player.getUUID();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        pose.pushPose();
        try {
            pose.translate(-camera.x, -camera.y, -camera.z);
            Matrix4f matrix = pose.last().pose();

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();

            // ---------- 1. 寄生体/目标：只有线，没有面 ----------
            drawEntityHighlights(minecraft, entries, dimension, self, camera, matrix, buffers);

            // ---------- 2. 路线库：先画其它路线，最后画正在编辑的那条 ----------
            String editingId = HiveCommandClientData.editingRouteId(self);
            drawRoutes(routes, editingId, dimension, camera, matrix, buffers, false);
            if (editingId != null) {
                drawRoutes(routes, editingId, dimension, camera, matrix, buffers, true);
            }
        } finally {
            // 就算中途异常也要收尾顶点批并恢复状态，否则会把之后的每一帧渲染带崩。
            // 顺序：先把残留顶点全部刷出去，再恢复 GL 状态（RenderType 的 setupRenderState 会改
            // blend / cull / depth，所以这些状态必须在 endBatch 之后再恢复）。
            buffers.endBatch(RenderType.LINES);
            buffers.endBatch(RenderType.debugQuads());
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(1.0F);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------
    // 实体 / 目标：12 条棱的线框
    // ------------------------------------------------------------------

    private static void drawEntityHighlights(Minecraft minecraft, List<HiveCommandHighlight> entries,
                                             String dimension, UUID self, Vec3 camera, Matrix4f matrix,
                                             MultiBufferSource.BufferSource buffers) {
        Map<Integer, Boolean> targets = new LinkedHashMap<>();
        RenderSystem.lineWidth(LINE_WIDTH);
        VertexConsumer lines = buffers.getBuffer(RenderType.LINES);
        boolean drew = false;

        for (HiveCommandHighlight entry : entries) {
            if (!dimension.equals(entry.dimension())) {
                continue;
            }
            boolean own = self.equals(entry.owner());
            if (entry.entityId() >= 0) {
                Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entry.entityId());
                if (entity != null && camera.distanceToSqr(entity.position())
                        <= ENTITY_VIEW_DISTANCE * ENTITY_VIEW_DISTANCE) {
                    AABB box = entity.getBoundingBox().inflate(EDGE_INFLATE);
                    if (own) {
                        drawWireBox(matrix, lines, box,
                                OWN_PARASITE_R, OWN_PARASITE_G, OWN_PARASITE_B, 0.95F);
                    } else {
                        drawWireBox(matrix, lines, box,
                                OTHER_PARASITE_R, OTHER_PARASITE_G, OTHER_PARASITE_B, 0.95F);
                    }
                    drew = true;
                }
            }
            // 攻击目标先聚合：同一目标被多人标记时，只要有一个是“自己”就画红色
            if (entry.targetId() >= 0) {
                targets.merge(entry.targetId(), own, (left, right) -> left || right);
            }
        }

        for (Map.Entry<Integer, Boolean> entry : targets.entrySet()) {
            Entity target = minecraft.level == null ? null : minecraft.level.getEntity(entry.getKey());
            if (target == null) {
                continue;
            }
            if (camera.distanceToSqr(target.position()) > ENTITY_VIEW_DISTANCE * ENTITY_VIEW_DISTANCE) {
                continue;
            }
            AABB box = target.getBoundingBox().inflate(EDGE_INFLATE);
            if (entry.getValue()) {
                drawWireBox(matrix, lines, box, OWN_TARGET_R, OWN_TARGET_G, OWN_TARGET_B, 0.95F);
            } else {
                drawWireBox(matrix, lines, box, OTHER_TARGET_R, OTHER_TARGET_G, OTHER_TARGET_B, 0.95F);
            }
            drew = true;
        }

        if (drew) {
            buffers.endBatch(RenderType.LINES);
        }
    }

    // ------------------------------------------------------------------
    // 路线
    // ------------------------------------------------------------------

    /**
     * 画路线库：{@code onlyEditing} 为 true 时只画“正在编辑”的那条（用亮紫 + 更宽的连线带）。
     *
     * <p>连线带与节点填充都在 {@code RenderType.debugQuads()} 这一批里，靠<b>顶点写入顺序</b>
     * 实现“先连线、后节点”：同一批次按写入顺序光栅化，节点填充必然盖在连线之上，
     * 不会出现线从节点方块里穿出来、看起来脏的情况。分两批（普通 / 编辑中）是必须的，
     * 这样编辑中的那条整体后画，视觉上压在所有其它路线上面。</p>
     */
    private static void drawRoutes(List<HiveRoute> routes, String editingId, String dimension, Vec3 camera,
                                   Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                                   boolean onlyEditing) {
        VertexConsumer lines = buffers.getBuffer(RenderType.LINES);
        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        float red = onlyEditing ? EDITING_R : ROUTE_R;
        float green = onlyEditing ? EDITING_G : ROUTE_G;
        float blue = onlyEditing ? EDITING_B : ROUTE_B;
        double half = onlyEditing ? NODE_HALF_EDITING : NODE_HALF;
        double ribbonHalf = onlyEditing ? EDITING_RIBBON_HALF : ROUTE_RIBBON_HALF;
        float fillAlpha = onlyEditing ? EDITING_FILL_ALPHA : ROUTE_FILL_ALPHA;
        boolean drew = false;

        for (HiveRoute route : routes) {
            boolean editing = editingId != null && editingId.equals(route.id());
            if (editing != onlyEditing) {
                continue;
            }
            if (!route.dimension().isEmpty() && !route.dimension().equals(dimension)) {
                continue;
            }
            List<BlockPos> points = route.points();
            if (points.isEmpty()) {
                continue;
            }
            if (camera.distanceToSqr(Vec3.atCenterOf(points.get(0)))
                    > ROUTE_VIEW_DISTANCE * ROUTE_VIEW_DISTANCE) {
                continue;
            }
            // 顺序很重要：先写连线带顶点，再写节点填充顶点（同一 debugQuads 批次内按写入顺序绘制）。
            drawRouteLines(matrix, quads, points, route.closed(), camera, ribbonHalf,
                    red, green, blue, ROUTE_LINE_ALPHA);
            drawRouteNodes(matrix, lines, quads, points, camera, half, red, green, blue, fillAlpha);
            drew = true;
        }

        if (drew) {
            buffers.endBatch(RenderType.debugQuads());
            buffers.endBatch(RenderType.LINES);
        }
    }

    /**
     * 画路线连线：相邻节点中心之间铺一条面向摄像机的带状四边形（含闭合段）。
     * 节点少于 2 个时没有线可画。
     */
    private static void drawRouteLines(Matrix4f matrix, VertexConsumer quadBuffer, List<BlockPos> route,
                                       boolean closed, Vec3 camera, double halfWidth,
                                       float red, float green, float blue, float alpha) {
        if (route.size() < 2) {
            return;
        }
        for (int i = 0; i < route.size() - 1; i++) {
            drawRibbonSegment(matrix, quadBuffer, route.get(i), route.get(i + 1), camera, halfWidth,
                    red, green, blue, alpha);
        }
        if (closed && route.size() > 2) {
            drawRibbonSegment(matrix, quadBuffer, route.get(route.size() - 1), route.get(0), camera, halfWidth,
                    red, green, blue, alpha);
        }
    }

    private static void drawRouteNodes(Matrix4f matrix, VertexConsumer lineBuffer, VertexConsumer quadBuffer,
                                       List<BlockPos> route, Vec3 camera, double half,
                                       float red, float green, float blue, float fillAlpha) {
        for (BlockPos pos : route) {
            double cx = pos.getX() + 0.5D;
            double cy = pos.getY() + 0.5D;
            double cz = pos.getZ() + 0.5D;
            if (camera.distanceToSqr(cx, cy, cz) > ROUTE_VIEW_DISTANCE * ROUTE_VIEW_DISTANCE) {
                continue;
            }
            AABB cube = new AABB(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half);
            // 需求原文要求路线节点保留“紫色填充小方块”：填充走 debugQuads，描边走 LINES。
            drawSolidBox(matrix, quadBuffer, cube, red, green, blue, fillAlpha);
            drawWireBox(matrix, lineBuffer, cube, red, green, blue, ROUTE_LINE_ALPHA);
        }
    }

    /**
     * 一段“有宽度的线”：以线段中点为锚，把线段沿“侧向量”上下各拉半宽，构成一个
     * <b>面向摄像机的带状四边形（billboard ribbon）</b>。
     *
     * <p>侧向量 = {@code normalize(cross(线段方向, 线段中点→摄像机))}：它同时垂直于线段方向和视线方向，
     * 所以四边形平面必定包含线段且正对镜头 —— 任意角度看都是完整宽度，不存在“侧看一条缝”的问题，
     * 也不依赖 {@code GL_LINES} 的 {@code lineWidth}。</p>
     *
     * <p>两端各沿线段方向外延一个半宽：折角处相邻两段互相重叠，并埋进节点方块内部，
     * 因此节点与连线之间不会出现缝隙。</p>
     *
     * <p>两面各写一次顶点（顶点顺序相反），这样无论 {@code debugQuads} 是否开启背面剔除，
     * 从线段两侧看都能看见这条带子。</p>
     */
    private static void drawRibbonSegment(Matrix4f matrix, VertexConsumer buffer, BlockPos from, BlockPos to,
                                          Vec3 camera, double halfWidth,
                                          float red, float green, float blue, float alpha) {
        double ax = from.getX() + 0.5D;
        double ay = from.getY() + 0.5D;
        double az = from.getZ() + 0.5D;
        double bx = to.getX() + 0.5D;
        double by = to.getY() + 0.5D;
        double bz = to.getZ() + 0.5D;

        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4D) {
            // 两个节点重合（同一格）：没有方向可铺，跳过，避免除零
            return;
        }
        double ux = dx / length;
        double uy = dy / length;
        double uz = dz / length;

        // 两端外延半个带宽：折角处相互重叠 + 埋进节点方块，消除“节点与线之间的缝”
        ax -= ux * halfWidth;
        ay -= uy * halfWidth;
        az -= uz * halfWidth;
        bx += ux * halfWidth;
        by += uy * halfWidth;
        bz += uz * halfWidth;

        double mx = (ax + bx) * 0.5D;
        double my = (ay + by) * 0.5D;
        double mz = (az + bz) * 0.5D;

        // 侧向量 = cross(线段方向, 线段中点→摄像机) = billboard 的“横向”
        double sx = uy * (camera.z - mz) - uz * (camera.y - my);
        double sy = uz * (camera.x - mx) - ux * (camera.z - mz);
        double sz = ux * (camera.y - my) - uy * (camera.x - mx);
        double sideLength = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sideLength < 1.0E-3D) {
            // 视线几乎与线段重合：线段在屏幕上退化成点，横向任意取一条垂直方向兜底即可（只为不除零）
            if (Math.abs(uy) < 0.9D) {
                // cross(方向, 世界竖直向上) = (-uz, 0, ux)
                sx = -uz;
                sy = 0.0D;
                sz = ux;
            } else {
                // 线段几乎竖直：cross(方向, 世界 X 轴) = (0, uz, -uy)
                sx = 0.0D;
                sy = uz;
                sz = -uy;
            }
            sideLength = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sideLength < 1.0E-6D) {
                return;
            }
        }
        double sideScale = halfWidth / sideLength;
        sx *= sideScale;
        sy *= sideScale;
        sz *= sideScale;

        // 四个角：A-侧、A+侧、B+侧、B-侧；两面各写一次，剔除状态无关
        quad(matrix, buffer, red, green, blue, alpha,
                ax - sx, ay - sy, az - sz,
                ax + sx, ay + sy, az + sz,
                bx + sx, by + sy, bz + sz,
                bx - sx, by - sy, bz - sz);
        quad(matrix, buffer, red, green, blue, alpha,
                bx - sx, by - sy, bz - sz,
                bx + sx, by + sy, bz + sz,
                ax + sx, ay + sy, az + sz,
                ax - sx, ay - sy, az - sz);
    }

    // ------------------------------------------------------------------
    // 顶点写入
    // ------------------------------------------------------------------

    /**
     * 只画立方体的 12 条棱边（不画任何面）：
     * 底面 4 条、顶面 4 条、竖直 4 条。
     */
    private static void drawWireBox(Matrix4f matrix, VertexConsumer buffer, AABB box,
                                    float red, float green, float blue, float alpha) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        // 底面（y1）
        edge(matrix, buffer, x1, y1, z1, x2, y1, z1, red, green, blue, alpha);
        edge(matrix, buffer, x2, y1, z1, x2, y1, z2, red, green, blue, alpha);
        edge(matrix, buffer, x2, y1, z2, x1, y1, z2, red, green, blue, alpha);
        edge(matrix, buffer, x1, y1, z2, x1, y1, z1, red, green, blue, alpha);
        // 顶面（y2）
        edge(matrix, buffer, x1, y2, z1, x2, y2, z1, red, green, blue, alpha);
        edge(matrix, buffer, x2, y2, z1, x2, y2, z2, red, green, blue, alpha);
        edge(matrix, buffer, x2, y2, z2, x1, y2, z2, red, green, blue, alpha);
        edge(matrix, buffer, x1, y2, z2, x1, y2, z1, red, green, blue, alpha);
        // 4 条竖棱
        edge(matrix, buffer, x1, y1, z1, x1, y2, z1, red, green, blue, alpha);
        edge(matrix, buffer, x2, y1, z1, x2, y2, z1, red, green, blue, alpha);
        edge(matrix, buffer, x2, y1, z2, x2, y2, z2, red, green, blue, alpha);
        edge(matrix, buffer, x1, y1, z2, x1, y2, z2, red, green, blue, alpha);
    }

    private static void edge(Matrix4f matrix, VertexConsumer buffer,
                             double ax, double ay, double az, double bx, double by, double bz,
                             float red, float green, float blue, float alpha) {
        lineVertex(matrix, buffer, ax, ay, az, red, green, blue, alpha);
        lineVertex(matrix, buffer, bx, by, bz, red, green, blue, alpha);
    }

    /** RenderType.LINES = POSITION_COLOR_NORMAL：位置、颜色、法线三个元素都要写。 */
    private static void lineVertex(Matrix4f matrix, VertexConsumer buffer, double x, double y, double z,
                                   float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(red, green, blue, alpha)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    /** RenderType.debugQuads = POSITION_COLOR：仅用于路线节点的“填充小方块”，六个面各四个顶点。 */
    private static void drawSolidBox(Matrix4f matrix, VertexConsumer buffer, AABB box,
                                     float red, float green, float blue, float alpha) {
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        quad(matrix, buffer, red, green, blue, alpha, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        quad(matrix, buffer, red, green, blue, alpha, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        quad(matrix, buffer, red, green, blue, alpha, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        quad(matrix, buffer, red, green, blue, alpha, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        quad(matrix, buffer, red, green, blue, alpha, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        quad(matrix, buffer, red, green, blue, alpha, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
    }

    private static void quad(Matrix4f matrix, VertexConsumer buffer, float red, float green, float blue,
                             float alpha,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.vertex(matrix, ax, ay, az).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, bx, by, bz).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, cx, cy, cz).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, dx, dy, dz).color(red, green, blue, alpha).endVertex();
    }

    /** 双精度版本：路线连线带的四个角是 double 运算出来的（世界坐标可能很大），这里统一收口转 float。 */
    private static void quad(Matrix4f matrix, VertexConsumer buffer, float red, float green, float blue,
                             float alpha,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz) {
        quad(matrix, buffer, red, green, blue, alpha,
                (float) ax, (float) ay, (float) az,
                (float) bx, (float) by, (float) bz,
                (float) cx, (float) cy, (float) cz,
                (float) dx, (float) dy, (float) dz);
    }
}
