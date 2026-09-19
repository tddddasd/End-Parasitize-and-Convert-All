package org.tdddd.epca.impl.client.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.AfterimageData;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.IGeoLayerProvider;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingEndermanHead;

import java.util.*;

/**
 * Layer provider that renders fading afterimage ghosts behind a moving entity.
 * Each afterimage records the entity's <b>rendered</b> position/yaw at spawn time and is drawn at
 * that exact world position for its whole lifetime, so the ghost stays where it appeared instead of
 * following the entity.
 *
 * <p>Movement detection uses the per-tick position delta ({@code getX() - xo}),
 * which is reliable on the client side. Spawning is throttled to once per game tick.</p>
 *
 * <h2>Frozen position — the 26.1.2 transform chain</h2>
 * <p>Vanilla's {@code EntityRenderDispatcher} translates the pose stack by the entity's
 * <i>interpolated</i> render position ({@code EntityRenderState.x/y/z}) and GeckoLib's
 * {@code GeoEntityRenderer.applyRotations} then post-multiplies its own rotation, so at layer time
 * the stack is</p>
 * <pre>  T(renderPos) · R(180 - bodyYaw) [· living extras]</pre>
 * <p>A plain {@code translate(delta)} from there is applied <b>inside the entity's rotated frame</b>:
 * the offset gets rotated by the entity's current yaw, which makes the ghost drift sideways and
 * appear to follow the entity. The layer therefore resets the current pose to the pass's
 * <i>pre-render</i> matrix ({@link RenderPassInfo#getPreRenderMatrixPose()}, captured in
 * {@code RenderPassInfo}'s constructor <b>before</b> GeckoLib's rotation) and rebuilds the ghost's
 * own transform from world axes:</p>
 * <pre>  T(renderPos) · T(spawnPos - renderPos) · R(180 - spawnYaw)</pre>
 * <p>which puts the ghost exactly at its spawn position, facing its spawn direction, for every
 * frame of its fade — regardless of where the entity has moved or turned since.</p>
 *
 * <h2>GeckoLib 4 → 5.5.2 — pose freeze (documented behaviour change)</h2>
 * <p>GeckoLib 4's layer received the live {@code BakedGeoModel} and could read and write each
 * {@code CoreGeoBone} transform, so afterimages replayed a <b>frozen per-bone pose</b>.
 * GeckoLib 5 no longer exposes a mutable bone transform at submission time: the animated pose
 * only exists inside the render pass's compiled bone snapshots, and re-submitting the model
 * for an afterimage necessarily uses the <i>current</i> frame's snapshots. The pose freeze is
 * therefore not reproducible through the public GeckoLib 5 API and afterimages now replay the
 * spawn-time <b>position, yaw, texture and alpha fade</b> but keep the current animation pose.
 * Everything else (spawn throttling, movement gate, lifetime, max count, fade curve) is
 * unchanged.</p>
 */
public class EndermanAfterimageLayer implements IGeoLayerProvider {

    private static final float SPAWN_CHANCE = 0.30F;
    private static final int AFTERIMAGE_LIFETIME = 20;
    private static final int MAX_AFTERIMAGES = 12;
    private static final float MAX_ALPHA = 0.55F;

    private static final Map<UUID, List<AfterimageData>> AFTERIMAGES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SPAWN_TICK = new HashMap<>();

    /** Cache: base texture → afterimage texture. */
    private static final Map<Identifier, Identifier> TEX_CACHE = new HashMap<>();

    /** Derive afterimage texture from the entity type's registry key. */
    private static Identifier getAfterimageTexture(LivingEntity entity) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (key == null) return Identifier.fromNamespaceAndPath("epca", "textures/entity/none.png");

        // 判断是否为 InfestedEndermite 的不稳定变种
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

        // 为防止默认变种与不稳定变种共用缓存，构造不同的缓存键
        Identifier cacheKey = isUnstable ?
                Identifier.fromNamespaceAndPath(key.getNamespace(), key.getPath() + "_unstable") :
                key;

        boolean finalIsUnstable = isUnstable;
        return TEX_CACHE.computeIfAbsent(cacheKey, k -> {
            String namespace = key.getNamespace();
            String path = key.getPath();
            // 不稳定变种使用 _unstable_afterimage 后缀
            String suffix = finalIsUnstable ? "_unstable_afterimage" : "_afterimage";
            return Identifier.fromNamespaceAndPath(namespace, "textures/entity/" + path + suffix + ".png");
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  Extraction phase — spawn / prune
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void addLayerData(GeoRenderState renderState, float partialTick) {
        Entity entity = EpcaGeoModel.entityOf(renderState);
        if (!(entity instanceof LivingEntity living)) return;

        int currentTick = (int) living.level().getGameTime();
        trySpawn(living, renderState, partialTick, currentTick);

        List<AfterimageData> afterimages = AFTERIMAGES.get(living.getUUID());
        if (afterimages != null) {
            afterimages.removeIf(data -> !data.isAlive(currentTick));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Submission phase — draw every live afterimage
    // ═══════════════════════════════════════════════════════════════

    @Override
    @SuppressWarnings("rawtypes")
    public void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
        Entity entity = EpcaGeoModel.entityOf(passInfo.renderState());
        if (!(entity instanceof LivingEntity living)) return;
        if (!(passInfo.renderer() instanceof EpcaGeoRenderer<?> renderer)) return;
        if (!(passInfo.renderState() instanceof EntityRenderState renderedState)) return;

        List<AfterimageData> afterimages = AFTERIMAGES.get(living.getUUID());
        if (afterimages == null || afterimages.isEmpty()) return;

        int currentTick = (int) living.level().getGameTime();
        Identifier afterimageTex = getAfterimageTexture(living);
        RenderType afterimageRenderType = RenderTypes.entityTranslucent(afterimageTex);

        PoseStack poseStack = passInfo.poseStack();
        int order = 10;
        for (AfterimageData data : afterimages) {
            float fade = data.getAlpha(currentTick);
            if (fade <= 0.0F) continue;

            poseStack.pushPose();

            // See the class javadoc: the live pose already contains GeckoLib's entity rotation, so
            // translate from the pass's pre-render matrix (T(renderPos), world-aligned) instead.
            poseStack.last().set(passInfo.getPreRenderMatrixPose());

            Vec3 afterPos = data.position;
            poseStack.translate(
                    afterPos.x - renderedState.x,
                    afterPos.y - renderedState.y + 0.02,
                    afterPos.z - renderedState.z
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - data.yRot));

            renderer.submitModelWithAlpha(passInfo, collector, order++, afterimageRenderType, fade * MAX_ALPHA);

            poseStack.popPose();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Spawning
    // ═══════════════════════════════════════════════════════════════

    private static void trySpawn(LivingEntity entity, GeoRenderState renderState, float partialTick, int currentTick) {
        if (!(renderState instanceof EntityRenderState state)) return;
        UUID id = entity.getUUID();

        // Throttle: only once per game tick
        Integer lastTick = LAST_SPAWN_TICK.get(id);
        if (lastTick != null && lastTick == currentTick) return;
        LAST_SPAWN_TICK.put(id, currentTick);

        // Movement check via per-tick position delta
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        if (dx * dx + dy * dy + dz * dz < 0.0001) return;

        List<AfterimageData> list = AFTERIMAGES.computeIfAbsent(id, k -> new ArrayList<>());
        list.removeIf(data -> !data.isAlive(currentTick));

        if (list.size() < MAX_AFTERIMAGES && entity.getRandom().nextFloat() < SPAWN_CHANCE) {
            // Record the *rendered* transform, not the raw entity transform: EntityRenderState.x/y/z
            // are the interpolated position the dispatcher translated the pose stack to, and the
            // yaw GeckoLib rotates by is the interpolated body yaw. Using the same values keeps the
            // ghost exactly on the spot the entity occupied that frame (no sub-tick drift).
            list.add(new AfterimageData(
                    new Vec3(state.x, state.y, state.z),
                    Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot),
                    currentTick,
                    AFTERIMAGE_LIFETIME,
                    Map.of()
            ));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Global cleanup
    // ═══════════════════════════════════════════════════════════════

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
