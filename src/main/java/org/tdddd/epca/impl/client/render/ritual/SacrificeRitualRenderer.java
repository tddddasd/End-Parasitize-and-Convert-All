package org.tdddd.epca.impl.client.render.ritual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.tdddd.epca.impl.client.effect.SacrificeRitualClientCache;

public final class SacrificeRitualRenderer {

    public static final float PILLAR_WIDTH = 0.30F;

    public static final float PILLAR_GLOW_WIDTH = 1.05F;

    public static final float PILLAR_GLOW_ALPHA = 0.32F;

    public static final float PILLAR_HEIGHT = 128.0F;

    public static final float PILLAR_ALPHA = 0.85F;

    public static final float PILLAR_TOP_ALPHA = 0.0F;

    public static final float SQUARE_LOW_OFFSET = 1.25F;

    public static final float SQUARE_LOW_SIDE = 2.5F;

    public static final float SQUARE_HIGH_OFFSET = 3.25F;

    public static final float SQUARE_HIGH_SIDE = 4.0F;

    public static final float SQUARE_THICKNESS = 0.20F;

    public static final float SQUARE_ALPHA = 0.85F;

    public static final int RING_PERIOD_TICKS = 40;

    public static final int RING_COUNT = 3;

    public static final float RING_MAX_RADIUS = 16.0F;

    public static final float RING_LINE_WIDTH = 0.07F;

    public static final float RING_ALPHA = 0.35F;

    public static final float WAVE_SURFACE_ALPHA = 0.50F;

    public static final float RING_OFFSET = 0.08F;

    public static final float WAVE_SURFACE_OFFSET = 0.03F;

    public static final int PULSE_PERIOD_TICKS = 40;

    public static final float PULSE_DEPTH = 0.45F;

    public static final float CORE_RED = 0.72F;
    public static final float CORE_GREEN = 0.26F;
    public static final float CORE_BLUE = 1.00F;

    public static final float WAVE_RED = 0.84F;
    public static final float WAVE_GREEN = 0.52F;
    public static final float WAVE_BLUE = 1.00F;

    public static final float SKY_RED = 0.62F;
    public static final float SKY_GREEN = 0.36F;
    public static final float SKY_BLUE = 0.94F;

    public static final float SKY_DISTANCE = 96.0F;

    public static final float SKY_SIZE = 4000.0F;

    public static final float SKY_MAX_ALPHA = 0.72F;

    public static final double MAX_SEGMENT_DISTANCE = 160.0D;

    public static final double MAX_PILLAR_DISTANCE = 192.0D;

    private static final RenderType QUAD_RENDER_TYPE = RitualQuadRenderType.get();

    public static final float LINE_HALO_WIDTH = 2.6F;

    public static final float LINE_HALO_ALPHA = 0.30F;

    private SacrificeRitualRenderer() {
    }

    public static void renderGeometry(RenderLevelStageEvent event) {
        if (!SacrificeRitualClientCache.hasAny()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        float time = level.getGameTime() + (float) event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(QUAD_RENDER_TYPE);
        try {
            SacrificeRitualClientCache.forEach(entry ->
                    drawRitual(consumer, matrix, level, entry.center, entry.fade(), time, camera));
        } finally {
            buffers.endBatch(QUAD_RENDER_TYPE);
            poseStack.popPose();
        }
    }

    public static void renderSky(RenderLevelStageEvent event) {
        if (!SacrificeRitualClientCache.hasAny()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(QUAD_RENDER_TYPE);
        try {
            drawSky(consumer, matrix, event, camera, SacrificeRitualClientCache.strongestFade());
        } finally {
            buffers.endBatch(QUAD_RENDER_TYPE);
            poseStack.popPose();
        }
    }

    private static void drawSky(VertexConsumer consumer, Matrix4f matrix, RenderLevelStageEvent event,
                                Vec3 camera, float fade) {
        float alpha = fade * SKY_MAX_ALPHA;
        if (alpha <= 0.004F) {
            return;
        }
        Vector3f look = event.getCamera().getLookVector();
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = new Vector3f(look).cross(up);
        if (right.lengthSquared() < 1.0E-6F) {

            right.set(1.0F, 0.0F, 0.0F);
        }
        right.normalize();
        Vector3f realUp = new Vector3f(right).cross(look).normalize();

        float centreX = (float) camera.x + look.x * SKY_DISTANCE;
        float centreY = (float) camera.y + look.y * SKY_DISTANCE;
        float centreZ = (float) camera.z + look.z * SKY_DISTANCE;
        float half = SKY_SIZE * 0.5F;
        float rx = right.x * half;
        float ry = right.y * half;
        float rz = right.z * half;
        float ux = realUp.x * half;
        float uy = realUp.y * half;
        float uz = realUp.z * half;

        vertex(consumer, matrix, centreX - rx - ux, centreY - ry - uy, centreZ - rz - uz,
                SKY_RED, SKY_GREEN, SKY_BLUE, alpha, 0.5F);
        vertex(consumer, matrix, centreX + rx - ux, centreY + ry - uy, centreZ + rz - uz,
                SKY_RED, SKY_GREEN, SKY_BLUE, alpha, 0.5F);
        vertex(consumer, matrix, centreX + rx + ux, centreY + ry + uy, centreZ + rz + uz,
                SKY_RED, SKY_GREEN, SKY_BLUE, alpha, 0.5F);
        vertex(consumer, matrix, centreX - rx + ux, centreY - ry + uy, centreZ - rz + uz,
                SKY_RED, SKY_GREEN, SKY_BLUE, alpha, 0.5F);
    }

    private static void drawRitual(VertexConsumer consumer, Matrix4f matrix, ClientLevel level,
                                   BlockPos center, float fade, float time, Vec3 camera) {
        if (fade <= 0.004F) {
            return;
        }

        double baseX = center.getX() + 0.5D;
        double baseY = center.getY() + 1.0D;
        double baseZ = center.getZ() + 0.5D;

        float swing = 0.5F + 0.5F * Mth.sin(time * (float) (Math.PI * 2.0D) / PULSE_PERIOD_TICKS);
        float pulse = 1.0F - PULSE_DEPTH * swing * swing * swing;

        float auraFade = easeOutCubic(fade) * pulse;

        drawPillar(consumer, matrix, baseX, baseY, baseZ, camera, auraFade);
        drawSquare(consumer, matrix, baseX, baseY + SQUARE_LOW_OFFSET, baseZ, SQUARE_LOW_SIDE,
                camera, auraFade);
        drawSquare(consumer, matrix, baseX, baseY + SQUARE_HIGH_OFFSET, baseZ, SQUARE_HIGH_SIDE,
                camera, auraFade);
        drawRings(consumer, matrix, level, center, baseX, baseY, baseZ, camera, fade, time);
    }

    private static void drawPillar(VertexConsumer consumer, Matrix4f matrix,
                                   double x, double y0, double z, Vec3 camera, float alpha) {
        double top = y0 + PILLAR_HEIGHT;

        emitVerticalPanel(consumer, matrix, x, y0, z, camera, 1.0D, 0.0D, PILLAR_WIDTH,
                alpha * PILLAR_ALPHA, alpha * PILLAR_TOP_ALPHA);
        emitVerticalPanel(consumer, matrix, x, y0, z, camera, 0.0D, 1.0D, PILLAR_WIDTH,
                alpha * PILLAR_ALPHA, alpha * PILLAR_TOP_ALPHA);
        emitVerticalPanel(consumer, matrix, x, y0, z, camera, 1.0D, 0.0D, PILLAR_GLOW_WIDTH,
                alpha * PILLAR_ALPHA * PILLAR_GLOW_ALPHA, alpha * PILLAR_TOP_ALPHA);
        emitVerticalPanel(consumer, matrix, x, y0, z, camera, 0.0D, 1.0D, PILLAR_GLOW_WIDTH,
                alpha * PILLAR_ALPHA * PILLAR_GLOW_ALPHA, alpha * PILLAR_TOP_ALPHA);
    }

    private static void drawSquare(VertexConsumer consumer, Matrix4f matrix,
                                   double x, double y, double z, float side, Vec3 camera, float alpha) {

        double half = side / Math.sqrt(2.0D);
        float a = alpha * SQUARE_ALPHA;

        float core = SQUARE_THICKNESS * 0.5F;
        float halo = SQUARE_THICKNESS * LINE_HALO_WIDTH * 0.5F;
        emitEdgePrism(consumer, matrix, x + half, y, z, x, y, z + half, core, core,
                CORE_RED, CORE_GREEN, CORE_BLUE, a, camera);
        emitEdgePrism(consumer, matrix, x + half, y, z, x, y, z + half, halo, halo,
                CORE_RED, CORE_GREEN, CORE_BLUE, a * LINE_HALO_ALPHA, camera);
        emitEdgePrism(consumer, matrix, x, y, z + half, x - half, y, z, core, core,
                CORE_RED, CORE_GREEN, CORE_BLUE, a, camera);
        emitEdgePrism(consumer, matrix, x, y, z + half, x - half, y, z, halo, halo,
                CORE_RED, CORE_GREEN, CORE_BLUE, a * LINE_HALO_ALPHA, camera);
        emitEdgePrism(consumer, matrix, x - half, y, z, x, y, z - half, core, core,
                CORE_RED, CORE_GREEN, CORE_BLUE, a, camera);
        emitEdgePrism(consumer, matrix, x - half, y, z, x, y, z - half, halo, halo,
                CORE_RED, CORE_GREEN, CORE_BLUE, a * LINE_HALO_ALPHA, camera);
        emitEdgePrism(consumer, matrix, x, y, z - half, x + half, y, z, core, core,
                CORE_RED, CORE_GREEN, CORE_BLUE, a, camera);
        emitEdgePrism(consumer, matrix, x, y, z - half, x + half, y, z, halo, halo,
                CORE_RED, CORE_GREEN, CORE_BLUE, a * LINE_HALO_ALPHA, camera);
    }

    private static void drawRings(VertexConsumer consumer, Matrix4f matrix, ClientLevel level,
                                  BlockPos center, double x, double y, double z, Vec3 camera,
                                  float fade, float time) {
        for (int ring = 0; ring < RING_COUNT; ring++) {

            float phase = wrap01(time / RING_PERIOD_TICKS + (float) ring / RING_COUNT);

            float radius = easeOutCubic(phase) * RING_MAX_RADIUS;
            float ringFade = (1.0F - phase) * (1.0F - phase) * fade;
            if (ringFade <= 0.02F || radius <= 0.05F) {
                continue;
            }

            double half = radius;
            float airAlpha = ringFade * RING_ALPHA;
            emitSegment(consumer, matrix, x + half, y + RING_OFFSET, z, x, y + RING_OFFSET, z + half,
                    RING_LINE_WIDTH, WAVE_RED, WAVE_GREEN, WAVE_BLUE, airAlpha, camera);
            emitSegment(consumer, matrix, x, y + RING_OFFSET, z + half, x - half, y + RING_OFFSET, z,
                    RING_LINE_WIDTH, WAVE_RED, WAVE_GREEN, WAVE_BLUE, airAlpha, camera);
            emitSegment(consumer, matrix, x - half, y + RING_OFFSET, z, x, y + RING_OFFSET, z - half,
                    RING_LINE_WIDTH, WAVE_RED, WAVE_GREEN, WAVE_BLUE, airAlpha, camera);
            emitSegment(consumer, matrix, x, y + RING_OFFSET, z - half, x + half, y + RING_OFFSET, z,
                    RING_LINE_WIDTH, WAVE_RED, WAVE_GREEN, WAVE_BLUE, airAlpha, camera);

            int reach = Mth.ceil(radius) + 1;
            float surfaceAlpha = ringFade * WAVE_SURFACE_ALPHA;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (!cellCrossesDiamond(dx, dz, radius)) {
                        continue;
                    }
                    BlockPos column = center.offset(dx, 0, dz);
                    BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
                    if (top.getY() <= level.getMinBuildHeight()) {
                        continue;
                    }
                    double surfaceY = top.getY() + WAVE_SURFACE_OFFSET;
                    emitDiagonalCell(consumer, matrix, x + dx, surfaceY, z + dz, dx, dz, radius,
                            surfaceAlpha, camera);
                }
            }
        }
    }

    private static boolean cellCrossesDiamond(int dx, int dz, double radius) {
        double minDiagonal = Double.MAX_VALUE;
        double maxDiagonal = -Double.MAX_VALUE;
        for (int cornerX = 0; cornerX <= 1; cornerX++) {
            for (int cornerZ = 0; cornerZ <= 1; cornerZ++) {
                double corner = Math.abs(dx + cornerX) + Math.abs(dz + cornerZ);
                minDiagonal = Math.min(minDiagonal, corner);
                maxDiagonal = Math.max(maxDiagonal, corner);
            }
        }
        return radius >= minDiagonal && radius <= maxDiagonal;
    }

    private static void emitDiagonalCell(VertexConsumer consumer, Matrix4f matrix,
                                         double cellOriginX, double surfaceY, double cellOriginZ,
                                         int dx, int dz, double radius, float alpha, Vec3 camera) {

        double sx = dx >= 0 ? 1.0D : -1.0D;
        double sz = dz >= 0 ? 1.0D : -1.0D;
        double x0 = dx;
        double x1 = dx + 1.0D;
        double z0 = dz;
        double z1 = dz + 1.0D;

        double ax = x0;
        double bx = x1;
        double az = (radius - sx * ax) / sz;
        double bz = (radius - sx * bx) / sz;
        double zSpan = bz - az;

        double t0 = 0.0D;
        double t1 = 1.0D;
        if (Math.abs(zSpan) > 1.0E-9D) {
            double ta = (z0 - az) / zSpan;
            double tb = (z1 - az) / zSpan;
            t0 = Math.max(t0, Math.min(ta, tb));
            t1 = Math.min(t1, Math.max(ta, tb));
        } else if (az < z0 || az > z1) {
            return;
        }
        if (t1 - t0 <= 1.0E-6D) {
            return;
        }

        double px0 = ax + (bx - ax) * t0;
        double pz0 = az + zSpan * t0;
        double px1 = ax + (bx - ax) * t1;
        double pz1 = az + zSpan * t1;
        emitSegment(consumer, matrix,
                cellOriginX + px0, surfaceY, cellOriginZ + pz0,
                cellOriginX + px1, surfaceY, cellOriginZ + pz1,
                RING_LINE_WIDTH, WAVE_RED, WAVE_GREEN, WAVE_BLUE, alpha, camera);
    }

    private static void emitVerticalPanel(VertexConsumer consumer, Matrix4f matrix,
                                          double x, double y0, double z, Vec3 camera,
                                          double normalX, double normalZ, float width,
                                          float bottomAlpha, float topAlpha) {
        double half = width * 0.5D;
        double offsetX = normalZ * half;
        double offsetZ = normalX * half;
        double top = y0 + PILLAR_HEIGHT;
        double dxc = x - camera.x;
        double dyc = y0 - camera.y;
        double dzc = z - camera.z;
        if (dxc * dxc + dyc * dyc + dzc * dzc > MAX_PILLAR_DISTANCE * MAX_PILLAR_DISTANCE) {
            return;
        }
        vertex(consumer, matrix, x - offsetX, y0, z - offsetZ, CORE_RED, CORE_GREEN, CORE_BLUE,
                bottomAlpha, 0.0F);
        vertex(consumer, matrix, x + offsetX, y0, z + offsetZ, CORE_RED, CORE_GREEN, CORE_BLUE,
                bottomAlpha, 1.0F);
        vertex(consumer, matrix, x + offsetX, top, z + offsetZ, CORE_RED, CORE_GREEN, CORE_BLUE,
                topAlpha, 1.0F);
        vertex(consumer, matrix, x - offsetX, top, z - offsetZ, CORE_RED, CORE_GREEN, CORE_BLUE,
                topAlpha, 0.0F);
    }

    private static void emitSegment(VertexConsumer consumer, Matrix4f matrix,
                                    double x1, double y1, double z1,
                                    double x2, double y2, double z2,
                                    float width, float red, float green, float blue, float alpha,
                                    Vec3 camera) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-5D) {
            return;
        }

        double midX = (x1 + x2) * 0.5D - camera.x;
        double midY = y1 - camera.y;
        double midZ = (z1 + z2) * 0.5D - camera.z;
        if (midX * midX + midY * midY + midZ * midZ > MAX_SEGMENT_DISTANCE * MAX_SEGMENT_DISTANCE) {
            return;
        }
        emitSegmentQuad(consumer, matrix, x1, y1, z1, x2, y2, z2, width, red, green, blue, alpha);

        emitSegmentQuad(consumer, matrix, x1, y1, z1, x2, y2, z2,
                width * LINE_HALO_WIDTH, red, green, blue, alpha * LINE_HALO_ALPHA);
    }

    private static void emitSegmentQuad(VertexConsumer consumer, Matrix4f matrix,
                                        double x1, double y1, double z1,
                                        double x2, double y2, double z2,
                                        float width, float red, float green, float blue, float alpha) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-5D) {
            return;
        }
        double half = width * 0.5D;
        double offsetX = -dz / length * half;
        double offsetZ = dx / length * half;
        vertex(consumer, matrix, x1 - offsetX, y1, z1 - offsetZ, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 - offsetX, y2, z2 - offsetZ, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 + offsetX, y2, z2 + offsetZ, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x1 + offsetX, y1, z1 + offsetZ, red, green, blue, alpha, 1.0F);
    }

    private static void emitEdgePrism(VertexConsumer consumer, Matrix4f matrix,
                                      double x1, double y1, double z1,
                                      double x2, double y2, double z2,
                                      float halfThickness, float halfHeight,
                                      float red, float green, float blue, float alpha, Vec3 camera) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-5D) {
            return;
        }

        double midX = (x1 + x2) * 0.5D - camera.x;
        double midY = (y1 + y2) * 0.5D - camera.y;
        double midZ = (z1 + z2) * 0.5D - camera.z;
        if (midX * midX + midY * midY + midZ * midZ > MAX_SEGMENT_DISTANCE * MAX_SEGMENT_DISTANCE) {
            return;
        }
        double nx = -dz / length;
        double nz = dx / length;
        double ox = nx * halfThickness;
        double oz = nz * halfThickness;

        vertex(consumer, matrix, x1 - ox, y1 + halfHeight, z1 - oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 - ox, y2 + halfHeight, z2 - oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 + ox, y2 + halfHeight, z2 + oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x1 + ox, y1 + halfHeight, z1 + oz, red, green, blue, alpha, 1.0F);

        vertex(consumer, matrix, x1 - ox, y1 - halfHeight, z1 - oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x1 + ox, y1 - halfHeight, z1 + oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x2 + ox, y2 - halfHeight, z2 + oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x2 - ox, y2 - halfHeight, z2 - oz, red, green, blue, alpha, 0.0F);

        vertex(consumer, matrix, x1 + ox, y1 - halfHeight, z1 + oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 + ox, y2 - halfHeight, z2 + oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 + ox, y2 + halfHeight, z2 + oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x1 + ox, y1 + halfHeight, z1 + oz, red, green, blue, alpha, 1.0F);

        vertex(consumer, matrix, x1 - ox, y1 - halfHeight, z1 - oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x1 - ox, y1 + halfHeight, z1 - oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x2 - ox, y2 + halfHeight, z2 - oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x2 - ox, y2 - halfHeight, z2 - oz, red, green, blue, alpha, 1.0F);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               double x, double y, double z,
                               float red, float green, float blue, float alpha, float v) {
        consumer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private static float wrap01(float value) {
        float wrapped = value % 1.0F;
        return wrapped < 0.0F ? wrapped + 1.0F : wrapped;
    }

    private static float easeOutCubic(float x) {
        float c = 1.0F - x;
        return 1.0F - c * c * c;
    }
}
