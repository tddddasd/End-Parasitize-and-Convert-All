package org.tdddd.epca.impl.client.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.client.entity.AfterimageData;
import org.tdddd.epca.impl.client.entity.AfterimageData.BoneSnapshot;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.IGeoLayerProvider;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingEndermanHead;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.*;

/**
 * Layer provider that renders fading afterimage ghosts behind a moving entity.
 * Each afterimage captures the entity's bone pose at spawn time so it renders
 * frozen at that moment rather than following the current animation.
 *
 * <p>Movement detection uses the per-tick position delta ({@code getX() - xo}),
 * which is reliable on the client side. Spawning is throttled to once per game tick.</p>
 */
public class EndermanAfterimageLayer implements IGeoLayerProvider {

    private static final float SPAWN_CHANCE = 0.30F;
    private static final int AFTERIMAGE_LIFETIME = 20;
    private static final int MAX_AFTERIMAGES = 12;
    private static final float MAX_ALPHA = 0.55F;

    private static final Map<UUID, List<AfterimageData>> AFTERIMAGES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SPAWN_TICK = new HashMap<>();

    /** Cache: base texture → afterimage texture. */
    private static final Map<ResourceLocation, ResourceLocation> TEX_CACHE = new HashMap<>();

    /** Derive afterimage texture from the entity type's Forge registry key. */
    private static ResourceLocation getAfterimageTexture(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (key == null) return new ResourceLocation("epca", "textures/entity/none.png");

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
        ResourceLocation cacheKey = isUnstable ?
                new ResourceLocation(key.getNamespace(), key.getPath() + "_unstable") :
                key;

        boolean finalIsUnstable = isUnstable;
        return TEX_CACHE.computeIfAbsent(cacheKey, k -> {
            String namespace = key.getNamespace();
            String path = key.getPath();
            // 不稳定变种使用 _unstable_afterimage 后缀
            String suffix = finalIsUnstable ? "_unstable_afterimage" : "_afterimage";
            return new ResourceLocation(namespace, "textures/entity/" + path + suffix + ".png");
        });
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void renderAdditionalLayer(
            EpcaGeoRenderer renderer,
            LivingEntity entity,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            PoseStack poseStack,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        int currentTick = (int) entity.level().getGameTime();

        // ── 1. Try spawn (with bone snapshot from current bakedModel) ──
        trySpawn(entity, bakedModel, currentTick);

        // ── 2. Get active afterimages, prune expired ──
        List<AfterimageData> afterimages = AFTERIMAGES.get(entity.getUUID());
        if (afterimages == null || afterimages.isEmpty()) return;
        afterimages.removeIf(data -> !data.isAlive(currentTick));

        // ── 3. Render each afterimage with its frozen bone pose ──
        ResourceLocation afterimageTex = getAfterimageTexture(entity);
        RenderType afterimageRenderType = RenderType.entityTranslucent(afterimageTex);
        for (AfterimageData data : afterimages) {
            float fade = data.getAlpha(currentTick);
            if (fade <= 0.0F) continue;

            poseStack.pushPose();

            Vec3 afterPos = data.position;
            poseStack.translate(
                    afterPos.x - entity.getX(),
                    afterPos.y - entity.getY() + 0.02,
                    afterPos.z - entity.getZ()
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - data.yRot));

            // Apply the frozen bone pose, then render
            Map<String, BoneSnapshot> savedPose = new HashMap<>();
            walkApply(bakedModel, data.bonePose, savedPose);
            float alpha = fade * MAX_ALPHA;
            VertexConsumer buf = bufferSource.getBuffer(afterimageRenderType);
            renderer.renderModelWithAlpha(poseStack, entity, bakedModel, afterimageRenderType,
                    bufferSource, buf, partialTick, packedLight,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
            walkRestore(bakedModel, savedPose);

            poseStack.popPose();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Bone pose save / apply / restore
    // ═══════════════════════════════════════════════════════════════

    // ── Bone iteration: walk the baked model's bone tree ──

    /** Find top-level bones (parent == null) from the baked model's bone list. */
    private static List<CoreGeoBone> topLevelBones(BakedGeoModel model) {
        List<CoreGeoBone> roots = new ArrayList<>();
        for (CoreGeoBone bone : model.getBones()) {
            if (bone.getParent() == null) roots.add(bone);
        }
        return roots;
    }

    /** Walk the bone tree and walk the snapshot + save originals in parallel. */
    private static void walkApply(BakedGeoModel model,
                                   Map<String, BoneSnapshot> target,
                                   Map<String, BoneSnapshot> saved) {
        for (CoreGeoBone root : topLevelBones(model)) {
            applyRecursive(root, target, saved);
        }
    }

    private static void applyRecursive(CoreGeoBone bone,
                                        Map<String, BoneSnapshot> target,
                                        Map<String, BoneSnapshot> saved) {
        saved.put(bone.getName(), new BoneSnapshot(bone));
        BoneSnapshot snap = target.get(bone.getName());
        if (snap != null) snap.applyTo(bone);
        for (CoreGeoBone child : bone.getChildBones()) {
            applyRecursive(child, target, saved);
        }
    }

    /** Walk the bone tree and restore saved values. */
    private static void walkRestore(BakedGeoModel model, Map<String, BoneSnapshot> saved) {
        for (CoreGeoBone root : topLevelBones(model)) {
            restoreRecursive(root, saved);
        }
    }

    private static void restoreRecursive(CoreGeoBone bone, Map<String, BoneSnapshot> saved) {
        BoneSnapshot snap = saved.get(bone.getName());
        if (snap != null) snap.applyTo(bone);
        for (CoreGeoBone child : bone.getChildBones()) {
            restoreRecursive(child, saved);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Spawning
    // ═══════════════════════════════════════════════════════════════

    private static void trySpawn(LivingEntity entity, BakedGeoModel bakedModel, int currentTick) {
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

        if (list.size() < MAX_AFTERIMAGES && entity.level().random.nextFloat() < SPAWN_CHANCE) {
            // Capture the current bone pose from the baked model
            Map<String, BoneSnapshot> bonePose = new HashMap<>();
            for (CoreGeoBone root : topLevelBones(bakedModel)) {
                AfterimageData.captureRecursive(root, bonePose);
            }
            list.add(new AfterimageData(
                    entity.position(),
                    entity.getYRot(),
                    currentTick,
                    AFTERIMAGE_LIFETIME,
                    bonePose
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
