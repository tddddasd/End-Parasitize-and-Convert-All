package org.tdddd.epca.impl.client.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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

public class EndermanAfterimageLayer implements IGeoLayerProvider {

    private static final float SPAWN_CHANCE = 0.30F;
    private static final int AFTERIMAGE_LIFETIME = 20;
    private static final int MAX_AFTERIMAGES = 12;
    private static final float MAX_ALPHA = 0.55F;

    private static final Map<UUID, List<AfterimageData>> AFTERIMAGES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SPAWN_TICK = new HashMap<>();

    private static final Map<ResourceLocation, ResourceLocation> TEX_CACHE = new HashMap<>();

    private static ResourceLocation getAfterimageTexture(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (key == null) return new ResourceLocation("epca", "textures/entity/none.png");

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

        ResourceLocation cacheKey = isUnstable ?
                new ResourceLocation(key.getNamespace(), key.getPath() + "_unstable") :
                key;

        boolean finalIsUnstable = isUnstable;
        return TEX_CACHE.computeIfAbsent(cacheKey, k -> {
            String namespace = key.getNamespace();
            String path = key.getPath();

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

        trySpawn(entity, bakedModel, currentTick);

        List<AfterimageData> afterimages = AFTERIMAGES.get(entity.getUUID());
        if (afterimages == null || afterimages.isEmpty()) return;
        afterimages.removeIf(data -> !data.isAlive(currentTick));

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

            float liveYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(-liveYaw));
            poseStack.mulPose(Axis.YP.rotationDegrees(data.yRot));

            Map<String, BoneSnapshot> savedPose = new HashMap<>();
            walkApply(bakedModel, data.bonePose, savedPose);
            float alpha = fade * MAX_ALPHA;
            VertexConsumer buf = bufferSource.getBuffer(afterimageRenderType);
            renderer.renderModelWithAlphaFrozenPose(poseStack, entity, bakedModel, afterimageRenderType,
                    bufferSource, buf, partialTick, packedLight,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha, data.bonePose);
            walkRestore(bakedModel, savedPose);

            poseStack.popPose();
        }
    }

    private static List<CoreGeoBone> topLevelBones(BakedGeoModel model) {
        List<CoreGeoBone> roots = new ArrayList<>();
        for (CoreGeoBone bone : model.getBones()) {
            if (bone.getParent() == null) roots.add(bone);
        }
        return roots;
    }

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

    private static void trySpawn(LivingEntity entity, BakedGeoModel bakedModel, int currentTick) {
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

        if (list.size() < MAX_AFTERIMAGES && entity.level().random.nextFloat() < SPAWN_CHANCE) {

            Map<String, BoneSnapshot> bonePose = new HashMap<>();
            for (CoreGeoBone root : topLevelBones(bakedModel)) {
                AfterimageData.captureRecursive(root, bonePose);
            }
            list.add(new AfterimageData(
                    entity.position(),
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
