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

    @Override
    public void addLayerData(GeoRenderState renderState, float partialTick) {
        Entity entity = EpcaGeoModel.entityOf(renderState);
        if (!(entity instanceof LivingEntity living)) return;

        int currentTick = (int) living.level().getGameTime();
        trySpawn(living, renderState, currentTick);

        List<AfterimageData> afterimages = AFTERIMAGES.get(living.getUUID());
        if (afterimages != null) {
            afterimages.removeIf(data -> !data.isAlive(currentTick));
        }
    }

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

            renderer.submitModelWithAlpha(passInfo, collector, order++, afterimageRenderType, fade * MAX_ALPHA);

            poseStack.popPose();
        }
    }

    private static void trySpawn(LivingEntity entity, GeoRenderState renderState, int currentTick) {
        if (!(renderState instanceof EntityRenderState state)) return;
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

        if (list.size() < MAX_AFTERIMAGES && entity.getRandom().nextFloat() < SPAWN_CHANCE) {

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
                    Map.of()
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
