package org.tdddd.epca.impl.client.render;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Vector3fc;
import org.tdddd.epca.impl.client.EpcaClientSettings;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class EpcaGibRenderer {
    private static final List<Piece> PIECES = new ArrayList<>();

    private static final Map<Identifier, RenderType> RENDER_TYPES = new HashMap<>();

    private static final double MIN_PIXEL_SIZE = 0.35D;

    private static RenderType renderTypeFor(Identifier texture) {
        RenderType type = RENDER_TYPES.get(texture);
        if (type == null) {
            type = RenderTypes.entityTranslucent(texture);
            RENDER_TYPES.put(texture, type);
        }
        return type;
    }

    public static void add(String typeId, double x, double y, double z,
                           float yaw, float pitch, float width, float height) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Identifier id = Identifier.tryParse(typeId);
        if (id == null) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(net.minecraft.core.Holder.Reference::value).orElse(null);
        if (type == null || type == EntityType.PLAYER) return;
        Identifier modelId = EpcaEntityManager.getModel(type);
        Identifier texture = EpcaEntityManager.getTexture(type);
        if (modelId == null || texture == null) return;

        Entity dummy = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (dummy == null) return;
        dummy.setPos(x, y, z);
        var renderer = mc.getEntityRenderDispatcher().getRenderer(dummy);
        if (!(renderer instanceof GeoEntityRenderer geo)) return;
        BakedGeoModel baked;
        try {
            baked = geo.getGeoModel().getBakedModel(modelId);
        } catch (Throwable t) {
            return;
        }
        if (baked == null) return;

        List<Piece> candidates = new ArrayList<>();
        for (GeoBone bone : baked.topLevelBones()) {
            collect(bone, 0, 0, 0, candidates, texture);
        }
        if (candidates.isEmpty()) return;

        int count = EpcaClientSettings.resolvePieceCount(candidates.size());
        if (count <= 0) return;
        java.util.Collections.shuffle(candidates, new java.util.Random(level.getRandom().nextLong()));
        for (int i = 0; i < count; i++) {
            Piece p = candidates.get(i % candidates.size()).duplicate();
            p.baseX = (float) x; p.baseY = (float) y; p.baseZ = (float) z;
            p.yaw = p.yawO = yaw; p.pitch = p.pitchO = pitch;
            p.width = width; p.height = height;
            p.initMotion(level);
            PIECES.add(p);
        }
    }

    private static void collect(GeoBone bone, float px, float py, float pz,
                                List<Piece> out, Identifier texture) {
        if (bone == null) return;
        float x = px + bone.pivotX() / 16.0F;
        float y = py + bone.pivotY() / 16.0F;
        float z = pz + bone.pivotZ() / 16.0F;
        if (bone instanceof CuboidGeoBone cuboid && cuboid.cubes != null && cuboid.cubes.length > 0) {
            List<GeoCube> cubes = new ArrayList<>();
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            float cx = 0, cy = 0, cz = 0;
            for (GeoCube cube : cuboid.cubes) {
                cubes.add(cube);
                Vec3 p = cube.pivot();
                Vec3 s = cube.size();
                cx += (float) p.x; cy += (float) p.y; cz += (float) p.z;
                minX = Math.min(minX, (float) p.x - (float) Math.abs(s.x) * 0.5F);
                maxX = Math.max(maxX, (float) p.x + (float) Math.abs(s.x) * 0.5F);
                minY = Math.min(minY, (float) p.y - (float) Math.abs(s.y) * 0.5F);
                maxY = Math.max(maxY, (float) p.y + (float) Math.abs(s.y) * 0.5F);
                minZ = Math.min(minZ, (float) p.z - (float) Math.abs(s.z) * 0.5F);
                maxZ = Math.max(maxZ, (float) p.z + (float) Math.abs(s.z) * 0.5F);
            }
            float n = cubes.size();
            out.add(new Piece(cubes, texture, cx / n, cy / n, cz / n,
                    (maxX - minX) / 32.0F, (maxY - minY) / 32.0F, (maxZ - minZ) / 32.0F,
                    x, y, z));
        }
        for (GeoBone child : bone.children()) {
            collect(child, x, y, z, out, texture);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        Iterator<Piece> it = PIECES.iterator();
        while (it.hasNext()) if (!it.next().tick(level)) it.remove();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (PIECES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.position();
        Vector3fc forward = camera.forwardVector();
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double pxPerUnit = pixelScalePerUnit(mc, camera.getFov());

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        for (int i = 0, size = PIECES.size(); i < size; i++) {
            Piece piece = PIECES.get(i);
            if (!piece.isVisible(cam, forward, pxPerUnit, partial)) continue;
            piece.render(poseStack, buffer, partial);
        }
        poseStack.popPose();
        buffer.endBatch();
    }

    private static double pixelScalePerUnit(Minecraft mc, float fovDegrees) {
        float fov = Mth.clamp(fovDegrees, 20.0F, 150.0F);
        double tanHalf = Math.tan(Math.toRadians(fov) * 0.5D);
        if (tanHalf <= 1.0E-4D) return 1000.0D;
        return mc.getWindow().getHeight() / (2.0D * tanHalf);
    }

    private static final class Piece {
        final List<GeoCube> cubes;
        final Identifier texture;
        final float centerX, centerY, centerZ;
        final float halfX, halfY, halfZ;
        final float spawnX, spawnY, spawnZ;
        double x, y, z, xo, yo, zo, mx, my, mz;
        float yaw, yawO, pitch, pitchO, spinY, spinP;
        float baseX, baseY, baseZ, width, height;
        int age, fadeStart;
        float alpha = 1.0F;

        int argb = 0xFFFFFFFF;

        Piece(List<GeoCube> cubes, Identifier texture, float cx, float cy, float cz,
              float hx, float hy, float hz, float sx, float sy, float sz) {
            this.cubes = cubes; this.texture = texture;
            this.centerX = cx; this.centerY = cy; this.centerZ = cz;
            this.halfX = Math.max(0.02F, hx); this.halfY = Math.max(0.02F, hy); this.halfZ = Math.max(0.02F, hz);
            this.spawnX = sx; this.spawnY = sy; this.spawnZ = sz;
            this.fadeStart = 40 + (int) (Math.random() * 20);
        }

        Piece duplicate() {
            return new Piece(this.cubes, this.texture, this.centerX, this.centerY, this.centerZ,
                    this.halfX, this.halfY, this.halfZ, this.spawnX, this.spawnY, this.spawnZ);
        }

        void initMotion(ClientLevel level) {
            this.x = xo = baseX + spawnX;
            this.y = yo = baseY + spawnY;
            this.z = zo = baseZ + spawnZ;
            double dx = spawnX, dy = spawnY - height * 0.5, dz = spawnZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1e-4) { dx = Math.random() - 0.5; dz = Math.random() - 0.5; len = Math.sqrt(dx * dx + dz * dz); }
            double speed = 0.38 + Math.random() * 0.28;
            this.mx = dx / len * speed; this.my = 0.22 + Math.random() * 0.2; this.mz = dz / len * speed;
            this.spinY = (float) ((Math.random() - 0.5) * 40.0); this.spinP = (float) ((Math.random() - 0.5) * 40.0);
        }

        boolean tick(ClientLevel level) {
            xo = x; yo = y; zo = z; yawO = yaw; pitchO = pitch; age++;
            my -= 0.08;
            x += mx; y += my; z += mz;
            BlockPos below = BlockPos.containing(x, y - halfY - 0.05, z);
            BlockState state = level.getBlockState(below);
            if (state.blocksMotion()) {
                y = below.getY() + 1.0 + halfY;
                my *= 0.8; mx *= 0.8; mz *= 0.8;
            } else {
                yaw += spinY; pitch += spinP;
                spinY *= 0.98F; spinP *= 0.98F;
            }
            my *= 0.98; mx *= 0.91; mz *= 0.91;
            if (age > fadeStart) alpha = 1.0F - (age - fadeStart) / 20.0F;
            argb = ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | 0xFFFFFF;
            return alpha > 0;
        }

        boolean isVisible(Vec3 cam, Vector3fc forward, double pxPerUnit, float partial) {
            float px = (float) (xo + (x - xo) * partial);
            float py = (float) (yo + (y - yo) * partial);
            float pz = (float) (zo + (z - zo) * partial);
            double dx = px - cam.x;
            double dy = py - cam.y;
            double dz = pz - cam.z;

            float radius = Math.max(halfX, Math.max(halfY, halfZ)) + 0.05F;
            if (!Float.isFinite(radius) || alpha <= 0.0F) return false;

            double along = dx * forward.x() + dy * forward.y() + dz * forward.z();
            if (along < -radius) return false;

            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < 1.0E-6D) return true;
            return radius * pxPerUnit / Math.sqrt(distSq) >= MIN_PIXEL_SIZE;
        }

        void render(PoseStack poseStack, MultiBufferSource buffer, float partial) {
            poseStack.pushPose();
            poseStack.translate(xo + (x - xo) * partial, yo + (y - yo) * partial, zo + (z - zo) * partial);
            poseStack.translate(-centerX / 16.0F, -centerY / 16.0F, -centerZ / 16.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partial, yawO, yaw)));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, pitchO, pitch)));
            VertexConsumer consumer = buffer.getBuffer(renderTypeFor(texture));
            for (GeoCube cube : cubes) cube.render(poseStack, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY, argb);
            poseStack.popPose();
        }
    }

    private EpcaGibRenderer() { }
}
