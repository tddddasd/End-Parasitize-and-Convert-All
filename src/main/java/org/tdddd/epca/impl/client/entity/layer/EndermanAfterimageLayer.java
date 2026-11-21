package org.tdddd.epca.impl.client.entity.layer;

import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.AfterimageData;
import org.tdddd.epca.impl.client.entity.AfterimageData.BonePose;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.IGeoLayerProvider;
import org.tdddd.epca.impl.client.render.AfterimagePipelines;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingEndermanHead;

import java.util.*;

/**
 * 末影人 / 行走头颅 / 末影螨共用的「半透明残影」层。
 *
 * <h2>与 1.20.1 的对应关系</h2>
 * <p>1.20.1（{@code G:\epca\eej\epca}）的做法是：生成残影时把当下每个 {@code CoreGeoBone} 的
 * rot / pos / scale 抄进 {@code AfterimageData.bonePose}，渲染时先保存当前骨骼姿态、把冻结值
 * 写进骨骼、{@code renderModelWithAlphaFrozenPose} 里再通过 {@code FrozenGhostRender}
 * 在 GeckoLib 的 {@code preRender} 之后<b>二次覆写</b>，不写深度、alpha = 0.55×fade、
 * 从远到近排序。</p>
 *
 * <p>26.1.2 的 GeckoLib 5 是"渲染态 + 提交式"的：{@code submitLayer} 里只能拿到
 * {@link RenderPassInfo}，几何体要到后面填 buffer 时才生成，骨骼姿态也只在
 * {@code RenderPassInfo#renderPosed} 里由 {@code BoneUpdater} 组装成 {@code BoneSnapshot[]}
 * 临时挂到 {@code GeoBone.frameSnapshot} 上，用完就 {@code cleanup()}。所以：</p>
 * <ul>
 *   <li><b>抓取点</b>：生成残影时（{@link #trySpawn}）直接借用<b>本体这一帧的 pass</b>：
 *       给它挂上最后一个 {@code BoneUpdater} 把整套数值抄进 {@link BonePose}，再调一次
 *       {@code RenderPassInfo#renderPosed} 让 GeckoLib 立刻把快照组出来（不提交任何几何体）。
 *       因为快照数组会被 {@code DeferredCache} 缓存，本体模型随后填 buffer 时用的就是同一套数值，
 *       等于"抄下玩家这一刻真正看到的姿态"（含动画、头部朝向、CAVE 变体抬腿等所有来源）。</li>
 *   <li><b>应用点</b>：每条残影各自开一个 {@link RenderPassInfo}（复用同一份 {@code BakedGeoModel}），
 *       把 {@link BonePose#applyTo} 注册为该 pass 的<b>最后一个</b> updater —— 等价于 1.20.1
 *       在 GeckoLib 摆好姿势之后的二次覆写（{@code FrozenGhostRender.reapplyPose}）。之所以必须
 *       一条残影一个 pass：{@code BoneUpdater} 在同一个 pass 里只会被编译一次，共用一个 pass 的话
 *       所有残影都会拿到同一套骨骼数值。三条实体（末影人 / 头颅 / 末影螨）走的是同一个
 *       {@code EpcaGeoRenderer}，所以三者都自动生效。</li>
 *   <li><b>模型复用</b>：{@code BakedGeoModel} 始终是 GeckoLib 缓存里的那一份（按 model resource 共享），
 *       每帧不为残影重建模型对象；每条残影的"脏"状态只有 {@link BonePose} 里那份扁平数组。</li>
 *   <li><b>不写深度 / 半透明 / 排序</b>：仍走 {@link AfterimagePipelines} 的
 *       {@code writeDepth=false} pipeline 与 {@code 0.55 × fade}，几何体依旧按残影到本体的距离
 *       从远到近、一条一个 {@code order} 提交。</li>
 * </ul>
 */
public class EndermanAfterimageLayer implements IGeoLayerProvider {

    private static final float SPAWN_CHANCE = 0.30F;
    private static final int AFTERIMAGE_LIFETIME = 20;
    private static final int MAX_AFTERIMAGES = 12;
    private static final float MAX_ALPHA = 0.55F;

    private static final Map<UUID, List<AfterimageData>> AFTERIMAGES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SPAWN_TICK = new HashMap<>();

    private static final Map<Identifier, Identifier> TEX_CACHE = new HashMap<>();

    private static Identifier getAfterimageTexture(LivingEntity entity) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (key == null) return Identifier.fromNamespaceAndPath("epca", "textures/entity/none.png");

        boolean isUnstable = false;
        if (entity instanceof InfestedEndermite endermite) {
            isUnstable = endermite.getVariant() == InfestedEndermite.Variant.UNSTABLE;
        }
        if (entity instanceof InfestedEnderman enderman) {
            isUnstable = enderman.getVariant() == InfestedEnderman.Variant.UNSTABLE;
        }
        if (entity instanceof WalkingEndermanHead endermanHead) {
            isUnstable = endermanHead.getVariant() == WalkingEndermanHead.Variant.UNSTABLE;
        }

        Identifier cacheKey = isUnstable ?
                Identifier.fromNamespaceAndPath(key.getNamespace(), key.getPath() + "_unstable") :
                key;

        boolean finalIsUnstable = isUnstable;
        return TEX_CACHE.computeIfAbsent(cacheKey, k -> {
            String namespace = key.getNamespace();
            String path = key.getPath();

            String suffix = finalIsUnstable ? "_unstable_afterimage" : "_afterimage";
            return Identifier.fromNamespaceAndPath(namespace, "textures/entity/" + path + suffix + ".png");
        });
    }

    /**
     * 渲染态阶段：这里拿不到渲染器 / pass，只做回收。
     * 生成残影必须放在 {@link #submitLayer}（那里才有 {@link RenderPassInfo} 可以抓骨骼姿态），
     * 这也与 1.20.1 在"层渲染时"生成残影的时机一致。
     */
    @Override
    public void addLayerData(GeoRenderState renderState, float partialTick) {
        Entity entity = EpcaGeoModel.entityOf(renderState);
        if (!(entity instanceof LivingEntity living)) return;

        List<AfterimageData> afterimages = AFTERIMAGES.get(living.getUUID());
        if (afterimages != null) {
            afterimages.removeIf(data -> !data.isAlive((int) living.level().getGameTime()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
        Entity entity = EpcaGeoModel.entityOf(passInfo.renderState());
        if (!(entity instanceof LivingEntity living)) return;
        if (!(passInfo.renderer() instanceof EpcaGeoRenderer<?> renderer)) return;
        if (!(passInfo.renderState() instanceof EntityRenderState renderedState)) return;

        int currentTick = (int) living.level().getGameTime();

        // 1) 生成：条件满足时抓取「此刻」的骨骼姿态（一条残影一份，见 AfterimageData.BonePose）。
        trySpawn(living, renderedState, passInfo, currentTick);

        List<AfterimageData> afterimages = AFTERIMAGES.get(living.getUUID());
        if (afterimages == null || afterimages.isEmpty()) return;
        afterimages.removeIf(data -> !data.isAlive(currentTick));
        if (afterimages.isEmpty()) return;

        Identifier afterimageTex = getAfterimageTexture(living);
        // 残影必须是"真半透明且不写深度"：写深度会让后面的实体 / 粒子在深度测试里被挡掉。
        // 26 的深度写入由 pipeline 的 DepthStencilState 决定，所以这里用 AfterimagePipelines 的自定义
        // pipeline（ENTITY_SNIPPET + TRANSLUCENT + LEQUAL + writeDepth=false + NO_CULL），
        // 等价于 1.20.1 的 AfterimageRenderType（COLOR_WRITE writeMask）。
        RenderType afterimageRenderType = AfterimagePipelines.renderTypeFor(afterimageTex);

        // 从远到近绘制：残影之间按距离正确混合，近处的残影后画，不会盖住远处那只。
        afterimages.sort((a, b) -> Double.compare(
                b.position.distanceToSqr(renderedState.x, renderedState.y, renderedState.z),
                a.position.distanceToSqr(renderedState.x, renderedState.y, renderedState.z)));

        float partialTick = passInfo.renderState().getPartialTick();
        float liveYaw = Mth.rotLerp(partialTick, living.yRotO, living.getYRot());

        PoseStack poseStack = passInfo.poseStack();
        int order = 10;
        for (AfterimageData data : afterimages) {
            float fade = data.getAlpha(currentTick);
            if (fade <= 0.0F) continue;

            poseStack.pushPose();

            poseStack.last().set(passInfo.getPreRenderMatrixPose());

            Vec3 afterPos = data.position;
            poseStack.translate(
                    afterPos.x - renderedState.x,
                    afterPos.y - renderedState.y + 0.02,
                    afterPos.z - renderedState.z
            );

            poseStack.mulPose(Axis.YP.rotationDegrees(-liveYaw));
            poseStack.mulPose(Axis.YP.rotationDegrees(data.yRot));

            // 2) 应用冻结姿态：每条残影一个独立的 pass，这样同一个实体这一帧的所有残影
            //    可以各用各的（生成时那一帧的）姿态 —— 共用一个 pass 时 GeckoLib 的
            //    BoneUpdater 只会被编译一次，所有残影会不可避免地拿到同一套骨骼数值。
            RenderPassInfo frozenPass = frozenPassInfo(passInfo, poseStack, data);
            renderer.submitModelWithAlpha(frozenPass, collector, order++, afterimageRenderType, fade * MAX_ALPHA);

            poseStack.popPose();
        }
    }

    /**
     * 为一条残影建立"冻结姿态"的渲染 pass。
     *
     * <p>{@link RenderPassInfo#create} 会先塞入 {@code applyAnimationControllers}
     * 与渲染器的 {@code adjustModelBonesForRender}；我们再把 {@link BonePose#applyTo}
     * 加在最后，于是动画算完的数值会被生成时冻结的数值覆盖 —— 效果等同 1.20.1 的
     * {@code FrozenGhostRender.reapplyPose}。</p>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RenderPassInfo frozenPassInfo(RenderPassInfo base, PoseStack poseStack, AfterimageData data) {
        RenderPassInfo pass = RenderPassInfo.create(
                base.renderer(), base.renderState(), poseStack, base.cameraState(), true);
        BonePose pose = data.bonePose;
        if (pose != null) {
            pass.addBoneUpdater((info, snapshots) -> pose.applyTo(snapshots));
        }
        return pass;
    }

    /**
     * 抓取本体这一帧的骨骼姿态。
     *
     * <p>做法是给本体自己的 {@link RenderPassInfo} 挂一个"抄数值"的 {@code BoneUpdater}
     * （放在最后，于是看到的是动画 + 渲染器骨骼修正之后、真正用于绘制的数值），然后
     * {@code renderPosed(空任务)} 让 GeckoLib 立刻把快照数组组出来。</p>
     *
     * <p>这样做没有额外成本：{@code RenderPassInfo} 的快照数组是 {@code DeferredCache}，
     * 本体模型随后填 buffer 时拿到的是<b>同一份缓存</b>，既不会重复求值动画、也不会改变本体外观
     * （本 updater 只读不写）；它也顺带保证了"残影姿态"与"当时看到的姿态"逐字段一致，
     * 包括 {@code IHeadRotatable} 的头部朝向、CAVE 变体蜘蛛的抬腿这类非动画来源的修正。</p>
     *
     * @return 快照；模型缺失 / 该 pass 的快照已被提交过时返回 {@code null}（退化为实时姿态）
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BonePose captureBonePose(RenderPassInfo passInfo) {
        // 有 per-bone render task 时 GeckoLib 会提前 compile 快照，此时再 addBoneUpdater 会被拒绝，
        // 直接放弃抓取（残影退化为跟随实时姿态），比让 GeckoLib 打错误日志干净。
        if (!passInfo.getBoneRenderTasks().isEmpty()) return null;

        BonePose[] captured = new BonePose[1];
        passInfo.addBoneUpdater((info, snapshots) -> captured[0] = BonePose.capture(info.model(), snapshots));
        passInfo.renderPosed(() -> {
        });
        return captured[0];
    }

    @SuppressWarnings("rawtypes")
    private static void trySpawn(LivingEntity entity, EntityRenderState state, RenderPassInfo passInfo,
                                 int currentTick) {
        UUID id = entity.getUUID();

        Integer lastTick = LAST_SPAWN_TICK.get(id);
        if (lastTick != null && lastTick == currentTick) return;
        LAST_SPAWN_TICK.put(id, currentTick);

        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        if (dx * dx + dy * dy + dz * dz < 0.0001) return;

        List<AfterimageData> list = AFTERIMAGES.computeIfAbsent(id, k -> new ArrayList<>());
        list.removeIf(data -> !data.isAlive(currentTick));

        if (list.size() >= MAX_AFTERIMAGES) return;
        if (entity.getRandom().nextFloat() >= SPAWN_CHANCE) return;

        // 对应 1.20.1 的 AfterimageData.captureRecursive(...)：抄下这一帧的骨骼姿态。
        BonePose bonePose;
        try {
            bonePose = captureBonePose(passInfo);
        } catch (RuntimeException e) {
            // 抓取失败时退化为旧行为（跟随本体实时姿态），不影响残影本身。
            bonePose = null;
        }

        list.add(new AfterimageData(
                new Vec3(state.x, state.y, state.z),
                entity.getYRot(),
                entity.yBodyRot,
                entity.getYHeadRot(),
                entity.yRotO,
                entity.yBodyRotO,
                entity.yHeadRotO,
                currentTick,
                AFTERIMAGE_LIFETIME,
                bonePose
        ));
    }

    public static void cleanupOrphaned() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            AFTERIMAGES.clear();
            LAST_SPAWN_TICK.clear();
            return;
        }
        Set<UUID> aliveUUIDs = new HashSet<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.isAlive()) aliveUUIDs.add(e.getUUID());
        }
        int currentTick = (int) mc.level.getGameTime();
        AFTERIMAGES.entrySet().removeIf(entry -> {
            UUID id = entry.getKey();
            if (!aliveUUIDs.contains(id)) {
                LAST_SPAWN_TICK.remove(id);
                return true;
            }
            entry.getValue().removeIf(data -> !data.isAlive(currentTick));
            if (entry.getValue().isEmpty()) {
                LAST_SPAWN_TICK.remove(id);
                return true;
            }
            return false;
        });
    }
}
