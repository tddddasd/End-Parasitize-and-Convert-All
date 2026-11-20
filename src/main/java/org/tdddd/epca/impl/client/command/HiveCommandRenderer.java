package org.tdddd.epca.impl.client.command;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderClientCache;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandHighlight;
import org.tdddd.epca.impl.overworld.data.command.HiveRoute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 指挥系统的世界内渲染（客户端，仅领巢者可见）。1.20.1 实现的等价移植。
 *
 * <h2>画法（严格区分“线”和“面”）</h2>
 * <ul>
 *     <li><b>被选中的寄生体 / 被指定攻击的目标：纯线框。</b>
 *     只画 AABB 的 12 条棱边：底面 4 条 + 顶面 4 条 + 4 条竖棱。没有任何四边形，
 *     所以不会出现“实心面/半透明体块”。AABB 外扩 {@link #EDGE_INFLATE} 格避免与实体表面 z-fighting。</li>
 *     <li><b>路线节点：紫色填充小方块 + 紫色线框描边。</b>填充走 {@code POSITION_COLOR} 四边面，
 *     描边走 {@code LINES} 的 12 条棱。</li>
 *     <li><b>路线连线：面向摄像机的带状四边形（billboard ribbon），不是 GL 线。</b>
 *     普通路线带宽 0.07 格、编辑中 0.09 格；侧向量取 {@code normalize(cross(线段方向, 视线方向))}，
 *     所以四边形平面永远包含线段且正对镜头。</li>
 *     <li><b>路线库全画：</b>普通路线 = 暗紫；自己“正在编辑”的那条 = 亮紫（连线更宽、节点更大），
 *     并且最后画，视觉上盖在其它路线上面。</li>
 * </ul>
 *
 * <h2>26.1.2 差异（渲染层的唯一实质降级/改造处）</h2>
 * <p>1.20.1 用 {@code RenderSystem.disableDepthTest()/depthMask(false)/lineWidth(...)} 直接改 GL 状态；
 * 26.1.2 的渲染状态被固化进 {@link RenderType}/RenderPipeline，渲染阶段不能再直接改。</p>
 *
 * <p>0.148n 之后改为直接用原版 {@link HiveCommandRenderTypes#lines()} /
 * {@link HiveCommandRenderTypes#quads()}（= {@code RenderTypes.lines()} /
 * {@code RenderTypes.debugQuads()}）：<b>放弃“透墙可见”</b>，接受“会被方块遮挡”的原版行为。
 * 这样彻底删掉了“自定义 pipeline 是否注册/编译成功”这个会导致整段几何静默不出现的未知量。
 * 线宽仍由顶点属性 {@code LineWidth} 提供；连线本身是 ribbon 四边形，不依赖 GL 线宽。</p>
 *
 * <h2>0.148n 真正导致“紫线与节点完全看不见”的原因（已修）</h2>
 * <p><b>不是渲染层，而是入口守卫把整帧拦掉了。</b>旧代码第一道判断是</p>
 * <pre>{@code if (!HiveLeaderClientCache.isHiveLeader(minecraft.player.getUUID())) return;}</pre>
 * <p>而客户端那份“谁是领巢者”的集合 {@code HiveLeaderClientCache} <b>只有</b>
 * {@code SyncHiveLeadersPacket} 会写，服务端又<b>只在</b>
 * {@code addHiveLeader/removeHiveLeader} 时才广播它 —— 玩家登录 / 换维度都<b>不会补发</b>。
 * 于是只要“领巢者身份是上一次游戏就存进存档的”（最常见情况），本次会话客户端集合就是空的：
 * 命令菜单照常能开关（菜单数据走 {@code SyncHiveCommandsPacket}，和这份集合无关），
 * 玩家也确实画出了路线，但世界内渲染<b>每帧都在第一行 return</b> ——
 * 紫线、紫色节点、实体线框全部不画，而且<b>零报错</b>，与日志现象完全一致。</p>
 *
 * <p>现在两层都修：</p>
 * <ol>
 *     <li>服务端在 {@code PlayerLoggedInEvent} 里给该玩家补发一次领巢者集合
 *     （{@code HiveLeaderManager#syncTo}），让所有依赖该缓存的功能
 *     （指挥系统紫线、生物量 HUD、寄生诅咒判定）登录后立刻正确；</li>
 *     <li>渲染器守卫加客户端兜底：能收到指挥同步包本身就证明自己是领巢者
 *     （见 {@code HiveCommandClientData#hasReceivedSync}），不再依赖登录时序。</li>
 * </ol>
 *
 * <h2>诊断日志</h2>
 * <p>每 {@value #DIAG_FRAME_INTERVAL} 帧打一行
 * {@code [epca-cmd-render] entries=N routes=M drawnLines=X drawnQuads=Y visible=...}：
 * 前 {@value #DIAG_INFO_REPORTS} 行用 INFO（方便改完直接确认），之后降为 DEBUG 不刷屏。
 * 若守卫拦下了渲染，会额外打一行一次性的 {@code skipped: ...} 说明原因。</p>
 *
 * <h2>26.1.2 的 {@code BufferSource} 陷阱（曾经导致 "Not building!" 崩溃）</h2>
 * <p>{@link MultiBufferSource.BufferSource} 对非 fixed 的 RenderType 只维护<b>一个共享的
 * {@code ByteBufferBuilder}</b>：{@code getBuffer(quadType)} 会先把上一个 RenderType（这里是
 * {@code lineType}）的 builder {@code build()} 掉并从 {@code startedBuilders} 移除，
 * 那个 builder 随即变成“not building”。旧实现先 {@code getBuffer(lineType)} 拿 {@code lines}、
 * 紧接着 {@code getBuffer(quadType)} 拿 {@code quads}，于是 {@code lines} 当场失效；
 * 再往 {@code lines} 里 {@code addVertex} 就抛 {@code IllegalStateException: Not building!}，
 * 异常冒泡到事件总线后升级成 {@code Minecraft/FATAL} 崩溃。现在所有绘制都走
 * {@link #writeBatch}：一批只用一个 RenderType，现取现用、写完立即 {@code endBatch}，
 * 并用 try/catch 把 {@code IllegalStateException} 挡在事件总线之外（最坏只丢一帧）。</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class HiveCommandRenderer {

    /** 诊断日志的采样间隔（帧）。60 帧 ≈ 1 秒。 */
    private static final int DIAG_FRAME_INTERVAL = 60;
    /** 前几行诊断日志用 INFO 输出（方便改完直接看日志确认），之后降到 DEBUG。 */
    private static final int DIAG_INFO_REPORTS = 3;

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
    /** 线框线宽（顶点属性 LineWidth，26 的写法）。 */
    private static final float LINE_WIDTH = 2.0F;
    /**
     * 普通路线连线的半宽（格）。带宽 = 0.07 格：比节点方块（0.2）细得多，但远粗于 1px 的 GL 线。
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

    /** 绘制层异常只报一次，避免每帧刷屏。 */
    private static boolean renderFailureReported;
    /** 守卫拦下渲染的原因只报一次（0.148n 定位“整帧被 return 掉”用的）。 */
    private static boolean gateReported;

    // ---- 诊断计数器（每帧重置）----
    /** 本帧写进 LINES 缓冲区的顶点数。 */
    private static int drawnLines;
    /** 本帧写进 QUADS 缓冲区的顶点数。 */
    private static int drawnQuads;
    /** 诊断日志的帧计数（只用于按 {@link #DIAG_FRAME_INTERVAL} 采样）。 */
    private static int diagFrame;
    /** 已经用 INFO 输出过几行诊断。 */
    private static int diagReports;

    private HiveCommandRenderer() {
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        List<HiveCommandHighlight> entries = HiveCommandClientData.entries();
        List<HiveRoute> routes = HiveCommandClientData.routes();
        UUID self = minecraft.player.getUUID();

        // 0.148n 修复：旧代码只认 HiveLeaderClientCache，而那份集合只在“有人被加入/移除领巢者”
        // 时广播、登录不补发 → 存档里早已是领巢者的人本次会话缓存为空 → 下面整帧 return，
        // 紫线/节点/线框全都不画且零报错。能收到指挥同步包本身就证明自己是领巢者。
        boolean leaderCached = HiveLeaderClientCache.isHiveLeader(self);
        if (!leaderCached && !HiveCommandClientData.hasReceivedSync()) {
            reportGateRejection(leaderCached, entries.size(), routes.size());
            return;
        }
        if (entries.isEmpty() && routes.isEmpty()) {
            reportGateRejection(leaderCached, 0, 0);
            return;
        }

        RenderType lineType = HiveCommandRenderTypes.lines();
        RenderType quadType = HiveCommandRenderTypes.quads();

        String dimension = minecraft.level.dimension().identifier().toString();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        boolean failed = false;
        drawnLines = 0;
        drawnQuads = 0;

        pose.pushPose();
        try {
            pose.translate(-camera.x, -camera.y, -camera.z);
            PoseStack.Pose last = pose.last();

            // ---------- 1. 寄生体/目标：只有线，没有面 ----------
            drawEntityHighlights(minecraft, entries, dimension, self, camera, last, buffers, lineType);

            // ---------- 2. 路线库：先画其它路线，最后画正在编辑的那条 ----------
            String editingId = HiveCommandClientData.editingRouteId(self);
            drawRoutes(routes, editingId, dimension, camera, last, buffers, lineType, quadType, false);
            if (editingId != null) {
                drawRoutes(routes, editingId, dimension, camera, last, buffers, lineType, quadType, true);
            }
        } catch (IllegalStateException exception) {
            // 防御：26 的 BufferSource 只要有一处“拿到 consumer 之后又被别的 RenderType 顶掉”，
            // 后面的 addVertex 就会抛 IllegalStateException("Not building!")。这里吞掉它、放弃本帧，
            // 绝不让异常冒泡到事件总线 —— 那会变成 Minecraft/FATAL: Unreported exception thrown! 直接崩游戏。
            failed = true;
            reportRenderFailure(exception);
        } finally {
            // 就算中途异常也要把残留顶点刷出去，否则会把之后的每一帧渲染带崩。
            // 这两个都是非 fixed 的 RenderType，必须作为 key 传进 endBatch(renderType)，
            // 无参 endBatch() 只会刷“最后一个共享类型”。
            buffers.endBatch(lineType);
            buffers.endBatch(quadType);
            pose.popPose();
        }
        reportFrame(entries.size(), routes.size(), drawnLines, drawnQuads, failed);
    }

    // ------------------------------------------------------------------
    // 诊断（0.148n 新增：一次到底地看清“有没有跑到绘制、写了多少顶点”）
    // ------------------------------------------------------------------

    /**
     * 每 {@link #DIAG_FRAME_INTERVAL} 帧打一行：
     * {@code [epca-cmd-render] entries=N routes=M drawnLines=X drawnQuads=Y visible=true/false}。
     *
     * <p>{@code visible} 为 {@code true} 表示这一帧真的往缓冲区写了顶点；
     * 如果它是 {@code true} 但屏幕上仍然没有，问题就只可能在 pipeline/RenderType（现在已经是原版的了）；
     * 如果它是 {@code false}，说明数据侧没东西可画（看上面的 entries/routes 计数）。</p>
     */
    private static void reportFrame(int entryCount, int routeCount, int lines, int quads, boolean failed) {
        diagFrame++;
        if (diagFrame % DIAG_FRAME_INTERVAL != 0) {
            return;
        }
        boolean visible = lines > 0 || quads > 0;
        String message = "[epca-cmd-render] entries={} routes={} drawnLines={} drawnQuads={} visible={}"
                + (failed ? " (last frame threw)" : "");
        if (diagReports < DIAG_INFO_REPORTS) {
            diagReports++;
            epca.LOGGER.info(message, entryCount, routeCount, lines, quads, visible);
        } else {
            epca.LOGGER.debug(message, entryCount, routeCount, lines, quads, visible);
        }
    }

    /** 渲染被守卫拦下时只报一次，说明是“不是领巢者”还是“没有数据”。 */
    private static void reportGateRejection(boolean leaderCached, int entryCount, int routeCount) {
        if (gateReported) {
            return;
        }
        gateReported = true;
        epca.LOGGER.info(
                "[epca-cmd-render] skipped: leaderCached={} receivedSync={} clientLeaderCount={} entries={} routes={}",
                leaderCached, HiveCommandClientData.hasReceivedSync(),
                HiveLeaderClientCache.size(), entryCount, routeCount);
    }

    // ------------------------------------------------------------------
    // 实体 / 目标：12 条棱的线框
    // ------------------------------------------------------------------

    private static void drawEntityHighlights(Minecraft minecraft, List<HiveCommandHighlight> entries,
                                             String dimension, UUID self, Vec3 camera, PoseStack.Pose pose,
                                             MultiBufferSource.BufferSource buffers, RenderType lineType) {
        // 整段实体/目标描边放进同一个 LINES 批次：一批里只允许出现 lineType 这一个 RenderType，
        // 写完立刻 endBatch，之后绝不再碰这个 consumer。
        writeBatch(buffers, lineType, lines -> {
            Map<Integer, Boolean> targets = new LinkedHashMap<>();

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
                            drawWireBox(pose, lines, box,
                                    OWN_PARASITE_R, OWN_PARASITE_G, OWN_PARASITE_B, 0.95F);
                        } else {
                            drawWireBox(pose, lines, box,
                                    OTHER_PARASITE_R, OTHER_PARASITE_G, OTHER_PARASITE_B, 0.95F);
                        }
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
                    drawWireBox(pose, lines, box, OWN_TARGET_R, OWN_TARGET_G, OWN_TARGET_B, 0.95F);
                } else {
                    drawWireBox(pose, lines, box, OTHER_TARGET_R, OTHER_TARGET_G, OTHER_TARGET_B, 0.95F);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // 路线
    // ------------------------------------------------------------------

    /**
     * 画路线库：{@code onlyEditing} 为 true 时只画“正在编辑”的那条（用亮紫 + 更宽的连线带）。
     *
     * <h2>为什么拆成两个批次（26 的 BufferSource 语义）</h2>
     * <p>{@link MultiBufferSource.BufferSource} 里所有非 fixed 的 RenderType <b>共用同一个
     * {@code ByteBufferBuilder}</b>：{@code getBuffer(B)} 会先把上一个 RenderType 的 builder
     * {@code build()} 掉（并把它从 startedBuilders 里移除），被 build 过的 builder 立刻变成
     * “not building”。因此<b>同一时刻只能持有一个 RenderType 的 consumer</b>，
     * 先 {@code getBuffer(lineType)} 再 {@code getBuffer(quadType)} 会让前者立刻失效。
     * 这里因此严格分成两批，每批只取一个 RenderType：</p>
     * <ol>
     *     <li><b>QUADS 批次</b>：先写所有连线带（ribbon），再写所有节点填充方块。
     *     同一批次按写入顺序光栅化，所以节点填充必然盖在连线之上；</li>
     *     <li><b>LINES 批次</b>：路线节点（以及节点立方体）的 12 条棱描边。</li>
     * </ol>
     * <p>编辑中的那条整体后画一次（第二个 {@code drawRoutes} 调用），
     * 视觉上压在所有其它路线上面。</p>
     */
    private static void drawRoutes(List<HiveRoute> routes, String editingId, String dimension, Vec3 camera,
                                   PoseStack.Pose pose, MultiBufferSource.BufferSource buffers,
                                   RenderType lineType, RenderType quadType, boolean onlyEditing) {
        List<HiveRoute> visible = collectVisibleRoutes(routes, editingId, dimension, camera, onlyEditing);
        if (visible.isEmpty()) {
            return;
        }
        float red = onlyEditing ? EDITING_R : ROUTE_R;
        float green = onlyEditing ? EDITING_G : ROUTE_G;
        float blue = onlyEditing ? EDITING_B : ROUTE_B;
        double half = onlyEditing ? NODE_HALF_EDITING : NODE_HALF;
        double ribbonHalf = onlyEditing ? EDITING_RIBBON_HALF : ROUTE_RIBBON_HALF;
        float fillAlpha = onlyEditing ? EDITING_FILL_ALPHA : ROUTE_FILL_ALPHA;

        // 批次 1：QUADS —— 连线带 + 节点填充（填充后写，盖住连线）
        writeBatch(buffers, quadType, quads -> {
            for (HiveRoute route : visible) {
                drawRouteLines(pose, quads, route.points(), route.closed(), camera, ribbonHalf,
                        red, green, blue, ROUTE_LINE_ALPHA);
            }
            for (HiveRoute route : visible) {
                drawRouteFills(pose, quads, route.points(), camera, half,
                        red, green, blue, fillAlpha);
            }
        });

        // 批次 2：LINES —— 节点描边（另一个 RenderType，必须单独成批）
        writeBatch(buffers, lineType, lines -> {
            for (HiveRoute route : visible) {
                drawRouteOutlines(pose, lines, route.points(), camera, half,
                        red, green, blue, ROUTE_LINE_ALPHA);
            }
        });
    }

    /** 过滤出本批次要画的路线：编辑态、维度、起点视距。 */
    private static List<HiveRoute> collectVisibleRoutes(List<HiveRoute> routes, String editingId,
                                                       String dimension, Vec3 camera, boolean onlyEditing) {
        List<HiveRoute> visible = new ArrayList<>();
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
            visible.add(route);
        }
        return visible;
    }

    /** 画路线连线：相邻节点中心之间铺一条面向摄像机的带状四边形（含闭合段）。 */
    private static void drawRouteLines(PoseStack.Pose pose, VertexConsumer quadBuffer, List<BlockPos> route,
                                       boolean closed, Vec3 camera, double halfWidth,
                                       float red, float green, float blue, float alpha) {
        if (route.size() < 2) {
            return;
        }
        for (int i = 0; i < route.size() - 1; i++) {
            drawRibbonSegment(pose, quadBuffer, route.get(i), route.get(i + 1), camera, halfWidth,
                    red, green, blue, alpha);
        }
        if (closed && route.size() > 2) {
            drawRibbonSegment(pose, quadBuffer, route.get(route.size() - 1), route.get(0), camera, halfWidth,
                    red, green, blue, alpha);
        }
    }

    /**
     * 路线节点填充（QUADS）：需求原文要求路线节点保留“紫色填充小方块”。
     *
     * <p>只拿 quad consumer，不碰 LINES 的 consumer —— 见 {@link #drawRoutes} 里“同一时刻只能持有一个
     * RenderType consumer”的说明。</p>
     */
    private static void drawRouteFills(PoseStack.Pose pose, VertexConsumer quadBuffer, List<BlockPos> route,
                                       Vec3 camera, double half,
                                       float red, float green, float blue, float fillAlpha) {
        for (BlockPos pos : route) {
            if (!isNodeVisible(pos, camera)) {
                continue;
            }
            drawSolidBox(pose, quadBuffer, nodeCube(pos, half), red, green, blue, fillAlpha);
        }
    }

    /** 路线节点描边（LINES）：节点的 12 条棱，独立批次。 */
    private static void drawRouteOutlines(PoseStack.Pose pose, VertexConsumer lineBuffer, List<BlockPos> route,
                                          Vec3 camera, double half,
                                          float red, float green, float blue, float alpha) {
        for (BlockPos pos : route) {
            if (!isNodeVisible(pos, camera)) {
                continue;
            }
            drawWireBox(pose, lineBuffer, nodeCube(pos, half), red, green, blue, alpha);
        }
    }

    private static boolean isNodeVisible(BlockPos pos, Vec3 camera) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        return camera.distanceToSqr(cx, cy, cz) <= ROUTE_VIEW_DISTANCE * ROUTE_VIEW_DISTANCE;
    }

    private static AABB nodeCube(BlockPos pos, double half) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        return new AABB(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half);
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
     * 因此节点与连线之间不会出现缝隙。两面各写一次顶点（顺序相反），剔除状态无关。</p>
     */
    private static void drawRibbonSegment(PoseStack.Pose pose, VertexConsumer buffer, BlockPos from, BlockPos to,
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
        quad(pose, buffer, red, green, blue, alpha,
                ax - sx, ay - sy, az - sz,
                ax + sx, ay + sy, az + sz,
                bx + sx, by + sy, bz + sz,
                bx - sx, by - sy, bz - sz);
        quad(pose, buffer, red, green, blue, alpha,
                bx - sx, by - sy, bz - sz,
                bx + sx, by + sy, bz + sz,
                ax + sx, ay + sy, az + sz,
                ax - sx, ay - sy, az - sz);
    }

    // ------------------------------------------------------------------
    // 批次收口（26 的 BufferSource 语义）
    // ------------------------------------------------------------------

    /** 一批顶点写入：consumer 由 {@link #writeBatch} 现取现给，写完后立即失效，不允许多批共用。 */
    @FunctionalInterface
    private interface VertexWriter {
        void write(VertexConsumer buffer);
    }

    /**
     * 取一个 RenderType 的 consumer、写完、立刻 {@code endBatch}。
     *
     * <p>26 的 {@link MultiBufferSource.BufferSource} 把所有非 fixed 的 RenderType 塞进同一个
     * {@code ByteBufferBuilder}：拿到另一个 RenderType 的 consumer 会先把当前 builder build 掉，
     * 被 build 的 builder 再 {@code addVertex} 就抛 {@code IllegalStateException("Not building!")}。
     * 所以这里保证“一批只用一个 RenderType、取到就写、写完立刻 flush”，
     * 上层永远拿不到、也留不住一个已经失效的 consumer。</p>
     *
     * <p>{@code finally} 里也要 {@code endBatch}：即便写顶点途中抛异常，
     * 也不能把半个 builder 留在 {@code startedBuilders} 里污染之后的每一帧。</p>
     */
    private static void writeBatch(MultiBufferSource.BufferSource buffers, RenderType renderType,
                                   VertexWriter writer) {
        VertexConsumer buffer = buffers.getBuffer(renderType);
        try {
            writer.write(buffer);
        } finally {
            buffers.endBatch(renderType);
        }
    }

    /** 每帧最多打印一次：绘制层的问题只丢帧，不刷屏、不冒泡成 FATAL。 */
    private static void reportRenderFailure(IllegalStateException exception) {
        if (renderFailureReported) {
            return;
        }
        renderFailureReported = true;
        epca.LOGGER.warn("Hive command renderer skipped a frame instead of crashing: {}",
                exception.toString(), exception);
    }

    // ------------------------------------------------------------------
    // 顶点写入
    // ------------------------------------------------------------------

    /** 只画立方体的 12 条棱边（不画任何面）：底面 4 条、顶面 4 条、竖直 4 条。 */
    private static void drawWireBox(PoseStack.Pose pose, VertexConsumer buffer, AABB box,
                                    float red, float green, float blue, float alpha) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        // 底面（y1）
        edge(pose, buffer, x1, y1, z1, x2, y1, z1, red, green, blue, alpha);
        edge(pose, buffer, x2, y1, z1, x2, y1, z2, red, green, blue, alpha);
        edge(pose, buffer, x2, y1, z2, x1, y1, z2, red, green, blue, alpha);
        edge(pose, buffer, x1, y1, z2, x1, y1, z1, red, green, blue, alpha);
        // 顶面（y2）
        edge(pose, buffer, x1, y2, z1, x2, y2, z1, red, green, blue, alpha);
        edge(pose, buffer, x2, y2, z1, x2, y2, z2, red, green, blue, alpha);
        edge(pose, buffer, x2, y2, z2, x1, y2, z2, red, green, blue, alpha);
        edge(pose, buffer, x1, y2, z2, x1, y2, z1, red, green, blue, alpha);
        // 4 条竖棱
        edge(pose, buffer, x1, y1, z1, x1, y2, z1, red, green, blue, alpha);
        edge(pose, buffer, x2, y1, z1, x2, y2, z1, red, green, blue, alpha);
        edge(pose, buffer, x2, y1, z2, x2, y2, z2, red, green, blue, alpha);
        edge(pose, buffer, x1, y1, z2, x1, y2, z2, red, green, blue, alpha);
    }

    private static void edge(PoseStack.Pose pose, VertexConsumer buffer,
                             double ax, double ay, double az, double bx, double by, double bz,
                             float red, float green, float blue, float alpha) {
        lineVertex(pose, buffer, ax, ay, az, red, green, blue, alpha);
        lineVertex(pose, buffer, bx, by, bz, red, green, blue, alpha);
    }

    /**
     * LINES 的顶点格式在 26 是 {@code POSITION_COLOR_NORMAL_LINE_WIDTH}：
     * 位置、颜色、法线、线宽四个元素都要写（线宽靠 {@code setLineWidth}，不靠 GL 状态）。
     */
    private static void lineVertex(PoseStack.Pose pose, VertexConsumer buffer, double x, double y, double z,
                                   float red, float green, float blue, float alpha) {
        drawnLines++;
        buffer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setNormal(0.0F, 1.0F, 0.0F)
                .setLineWidth(LINE_WIDTH);
    }

    /** POSITION_COLOR：仅用于路线节点的“填充小方块”，六个面各四个顶点。 */
    private static void drawSolidBox(PoseStack.Pose pose, VertexConsumer buffer, AABB box,
                                     float red, float green, float blue, float alpha) {
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        quad(pose, buffer, red, green, blue, alpha, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        quad(pose, buffer, red, green, blue, alpha, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        quad(pose, buffer, red, green, blue, alpha, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        quad(pose, buffer, red, green, blue, alpha, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        quad(pose, buffer, red, green, blue, alpha, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        quad(pose, buffer, red, green, blue, alpha, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float red, float green, float blue,
                             float alpha,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        drawnQuads += 4;
        buffer.addVertex(pose, ax, ay, az).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, bx, by, bz).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, cx, cy, cz).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, dx, dy, dz).setColor(red, green, blue, alpha);
    }

    /** 双精度版本：路线连线带的四个角是 double 运算出来的（世界坐标可能很大），这里统一收口转 float。 */
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float red, float green, float blue,
                             float alpha,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz) {
        quad(pose, buffer, red, green, blue, alpha,
                (float) ax, (float) ay, (float) az,
                (float) bx, (float) by, (float) bz,
                (float) cx, (float) cy, (float) cz,
                (float) dx, (float) dy, (float) dz);
    }
}
