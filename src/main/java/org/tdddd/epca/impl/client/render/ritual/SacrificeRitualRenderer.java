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
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderType;

/**
 * Draws the sacrifice-ritual aura: a bright purple pillar from the altar to the sky, two 45-degree
 * squares of glowing purple lines around it, and an expanding diagonal wavefield in the air and on the
 * nearby block tops - plus the purple sky the whole thing fades into.
 *
 * <h2>Geometry (all constants tunable below)</h2>
 * <ul>
 *   <li><b>Pillar</b>: two crossed vertical panels {@link #PILLAR_WIDTH} wide with a wider, dimmer
 *       {@link #PILLAR_GLOW_WIDTH} layer each, from the altar top up to {@link #PILLAR_HEIGHT}, fading
 *       out towards the top so it does not look cut off.</li>
 *   <li><b>Squares</b>: a horizontal square rotated 45 degrees, drawn as four glowing prism edges
 *       with a {@link #SQUARE_THICKNESS} x {@link #SQUARE_THICKNESS} block cross-section, centred on
 *       the pillar axis. The lower one sits {@link #SQUARE_LOW_OFFSET} above the altar top with side
 *       {@link #SQUARE_LOW_SIDE}, the upper one {@link #SQUARE_HIGH_OFFSET} with side
 *       {@link #SQUARE_HIGH_SIDE}.</li>
 *   <li><b>Wavefield</b>: {@link #RING_COUNT} diagonal rings {@link #RING_LINE_WIDTH} wide that expand
 *       from radius 0 to {@link #RING_MAX_RADIUS} over {@link #RING_PERIOD_TICKS} ticks and fade as
 *       they grow. They are drawn in the air just above the altar top, and - clipped to the cell that
 *       the diagonal actually crosses - on the nearest block top surface of every block column inside
 *       {@link #WAVE_BAND} of the ring, which is what makes the wave appear on the ground in patches
 *       and, further out, as half a diamond at a time (the opposite half lands in the mirror cell).</li>
 *   <li><b>Sky</b>: one huge {@link #SKY_DISTANCE} away, camera facing, filled with
 *       {@link #SKY_RED}/{@link #SKY_GREEN}/{@link #SKY_BLUE} at up to {@link #SKY_MAX_ALPHA}. It is
 *       drawn after the vanilla sky and before the terrain, so terrain still occludes it.</li>
 * </ul>
 *
 * <h2>Timing</h2>
 * <p>The aura follows {@code SacrificeRitualClientCache}: it fades in over
 * {@code FADE_IN_TICKS} once the server reports the ritual, stays while it is reported, and fades out
 * over {@code FADE_OUT_TICKS} after it stops - which is exactly when the ritual's lightning falls.
 * The pillar and the squares additionally breathe with {@link #PULSE_DEPTH} over
 * {@link #PULSE_PERIOD_TICKS} ("间隔的变暗/变亮").</p>
 *
 * <h2>Rendering</h2>
 * <p>Everything is drawn through the mod's proven custom pipeline with the geometry-agnostic
 * {@link GasCloudRenderType#RITUAL_STYLE_CHANNEL} branch of {@code gas_cloud.fsh}, from
 * {@code RenderLevelStageEvent.AFTER_PARTICLES} (after the terrain, so the wavefield sits on the block
 * tops, still depth tested so hills occlude the pillar correctly). The pose stack of that stage is the
 * camera-relative world space, so every vertex is emitted as {@code worldPos - cameraPos}.</p>
 */
public final class SacrificeRitualRenderer {

    // ---- pillar -----------------------------------------------------------------------------------

    /** Width of one of the two crossed pillar panels. */
    public static final float PILLAR_WIDTH = 0.30F;
    /** Width of the wider, dimmer halo layer behind each panel. */
    public static final float PILLAR_GLOW_WIDTH = 1.05F;
    /** Opacity of the halo layer relative to the core layer. */
    public static final float PILLAR_GLOW_ALPHA = 0.32F;
    /** How far the pillar reaches above the altar top. */
    public static final float PILLAR_HEIGHT = 128.0F;
    /** Opacity of the pillar at its foot. */
    public static final float PILLAR_ALPHA = 0.85F;
    /** Opacity of the pillar right below its top. */
    public static final float PILLAR_TOP_ALPHA = 0.0F;

    // ---- the two 45-degree squares -----------------------------------------------------------------

    /** Height of the lower square above the altar top. */
    public static final float SQUARE_LOW_OFFSET = 1.25F;
    /** Side length of the lower square. */
    public static final float SQUARE_LOW_SIDE = 2.5F;
    /** Height of the upper square above the altar top. */
    public static final float SQUARE_HIGH_OFFSET = 3.25F;
    /** Side length of the upper square. */
    public static final float SQUARE_HIGH_SIDE = 4.0F;
    /**
     * Cross-section of one glowing square edge: it is a square prism, {@code SQUARE_THICKNESS} wide and
     * {@code SQUARE_THICKNESS} tall, not a flat quad.
     */
    public static final float SQUARE_THICKNESS = 0.20F;
    /** Opacity of the square edges at full visibility. */
    public static final float SQUARE_ALPHA = 0.85F;

    // ---- wavefield ---------------------------------------------------------------------------------

    /** Ticks one ring needs to travel from the centre to {@link #RING_MAX_RADIUS}. */
    public static final int RING_PERIOD_TICKS = 40;
    /** How many rings are in flight at once, evenly phase shifted. */
    public static final int RING_COUNT = 3;
    /** Largest ring radius, in blocks. */
    public static final float RING_MAX_RADIUS = 16.0F;
    /** Thickness of one ring line. */
    public static final float RING_LINE_WIDTH = 0.07F;
    /** Opacity of the air rings. */
    public static final float RING_ALPHA = 0.35F;
    /** Opacity of the ring segments on the block top surfaces. */
    public static final float WAVE_SURFACE_ALPHA = 0.50F;
    /** Height above the altar top the air rings are drawn at. */
    public static final float RING_OFFSET = 0.08F;
    /** Height above a block's top surface its wave segment is drawn at. */
    public static final float WAVE_SURFACE_OFFSET = 0.03F;

    // ---- pulsing -----------------------------------------------------------------------------------

    /** Ticks of one bright/dim cycle. */
    public static final int PULSE_PERIOD_TICKS = 40;
    /** How much the pillar and the square lines dim at the dark end of the cycle. */
    public static final float PULSE_DEPTH = 0.45F;

    // ---- colours -----------------------------------------------------------------------------------

    /** Core purple of the pillar and the square lines. */
    public static final float CORE_RED = 0.72F;
    public static final float CORE_GREEN = 0.26F;
    public static final float CORE_BLUE = 1.00F;
    /** Brighter, whiter purple of the expanding wavefield lines. */
    public static final float WAVE_RED = 0.84F;
    public static final float WAVE_GREEN = 0.52F;
    public static final float WAVE_BLUE = 1.00F;
    /**
     * The sky fades to this purple. Deliberately a soft, low-saturation lavender rather than a fully
     * saturated violet: the cover is a huge, evenly lit quad, so a saturated colour reads as a flat
     * filter over the whole sky instead of a tinted sky.
     */
    public static final float SKY_RED = 0.62F;
    public static final float SKY_GREEN = 0.36F;
    public static final float SKY_BLUE = 0.94F;

    // ---- sky cover ---------------------------------------------------------------------------------

    /**
     * Distance of the sky quad from the camera, in blocks. Small on purpose: the quad is only an
     * underlay (everything later draws over it), and it has to stay well inside the projection's far
     * plane - {@code GameRenderer.getDepthFar()} is {@code effectiveRenderDistance * 64} blocks, so a
     * value in the hundreds would be clipped away at the lower render distances.
     */
    public static final float SKY_DISTANCE = 96.0F;
    /** Size of the sky quad; wide enough to cover the frustum at {@link #SKY_DISTANCE}. */
    public static final float SKY_SIZE = 4000.0F;
    /** Opacity of the fully ramped purple sky: a tint over the vanilla sky, not a lid. */
    public static final float SKY_MAX_ALPHA = 0.72F;

    /** Skip segments farther than this from the camera (blocks). */
    public static final double MAX_SEGMENT_DISTANCE = 160.0D;

    /** Skip whole pillars whose foot is farther than this from the camera (blocks). */
    public static final double MAX_PILLAR_DISTANCE = 192.0D;

    /**
     * The pipeline every ritual quad is drawn through: {@link RitualQuadRenderType#get()}, i.e. plain
     * {@code POSITION_COLOR} quads with the vanilla {@code position_color} program, translucent blending,
     * depth test on, depth writes off and culling off.
     *
     * <p>The mod's custom {@code gas_cloud} pipeline was tried first and produced nothing at this level
     * stage. A white control quad drawn through the built-in {@code RenderType.debugQuads()} at the very
     * same pose stack, stage and buffer source did show, which proves the draw call, the pose space, the
     * camera math and the render target; only the custom program or its state was at fault. This type
     * reuses the vanilla program (what the working control quad used) and adds the {@code NO_CULL} the
     * aura needs, because its quads are seen from above and from below.</p>
     */
    private static final RenderType QUAD_RENDER_TYPE = RitualQuadRenderType.get();

    /** Width factor of the dim halo quad emitted behind every glowing line. */
    public static final float LINE_HALO_WIDTH = 2.6F;

    /** Opacity factor of that halo quad. */
    public static final float LINE_HALO_ALPHA = 0.30F;

    /**
     * Temporary diagnostics: one INFO line per second while a ritual is known, plus a white control quad
     * drawn through the built-in {@code RenderType.debugQuads()} at every altar. The control quad tells
     * "nothing reaches the screen at all" apart from "the custom pipeline produces nothing"; switch it
     * off once the look has been confirmed in game.
     */
    public static final boolean DEBUG_DIAGNOSTICS = true;

    /** Vertices emitted this frame (diagnostics only). */
    private static int debugVertices;
    /** Level tick of the last diagnostics line, so it prints at most once a second. */
    private static long debugLastTick = Long.MIN_VALUE;

    private SacrificeRitualRenderer() {
    }

    /** Entry point for the aura geometry, from {@code SacrificeRitualStageListener} (AFTER_PARTICLES). */
    public static void renderGeometry(RenderLevelStageEvent event) {
        if (!SacrificeRitualClientCache.hasAny()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        // World time drives the pulse and the rings, so the animation is smooth and frame rate
        // independent; the partial tick keeps it continuous between ticks.
        float time = level.getGameTime() + (float) event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        // The pose stack of a level stage is world space (the camera offset is applied per object, as
        // LevelRenderer.renderEntity proves with Mth.lerp(...) - camera.getPosition()), so shift it to
        // the camera-relative frame the custom pipeline expects and emit absolute world coordinates.
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(QUAD_RENDER_TYPE);
        debugVertices = 0;
        try {
            SacrificeRitualClientCache.forEach(entry ->
                    drawRitual(consumer, matrix, level, entry.center, entry.fade(), time, camera));
            if (DEBUG_DIAGNOSTICS) {
                long tick = level.getGameTime();
                if (tick % 20L == 0L && tick != debugLastTick) {
                    debugLastTick = tick;
                    org.tdddd.epca.impl.epca.LOGGER.info(
                            "[ritual] client render: entries={} quads={} fade={} cam=({},{},{}) "
                                    + "pose=({},{},{}) partial={}",
                            countEntries(), debugVertices / 4, SacrificeRitualClientCache.strongestFade(),
                            fmt(camera.x), fmt(camera.y), fmt(camera.z),
                            fmt(matrix.m30()), fmt(matrix.m31()), fmt(matrix.m32()),
                            fmt(event.getPartialTick()));
                }
            }
        } finally {
            buffers.endBatch(QUAD_RENDER_TYPE);
            poseStack.popPose();
        }
    }

    /** Number of cached rituals; diagnostics only. */
    private static int countEntries() {
        int[] count = new int[1];
        SacrificeRitualClientCache.forEach(entry -> count[0]++);
        return count[0];
    }

    /** Locale-independent two-decimal formatting for the diagnostics line. */
    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    /**
     * Entry point for the purple sky, from {@code SacrificeRitualStageListener} (AFTER_SKY).
     *
     * <p>Deliberately the earliest stage: whatever is drawn afterwards - terrain, entities, the aura's
     * own geometry - is simply drawn on top, which is what "the sky turns purple" has to look like. The
     * quad sits well inside the projection's far plane ({@link #SKY_DISTANCE}), so it can never be
     * clipped; being an underlay it does not have to be at the horizon either.</p>
     */
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

    // ===============================================================================================
    //  Sky
    // ===============================================================================================

    /** The purple sky cover: one huge camera-facing quad, drawn last so it covers clouds and weather. */
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
            // Looking straight up or down: any horizontal axis will do.
            right.set(1.0F, 0.0F, 0.0F);
        }
        right.normalize();
        Vector3f realUp = new Vector3f(right).cross(look).normalize();

        // Absolute world coordinates (the caller already shifted the pose stack to camera-relative).
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

        // All four corners use V = 0.5, which the glow profile leaves at full strength -> flat fill.
        vertex(consumer, matrix, centreX - rx - ux, centreY - ry - uy, centreZ - rz - uz,
                SKY_RED, SKY_GREEN, SKY_BLUE, alpha, 0.5F);
        vertex(consumer, matrix, centreX + rx - ux, centreY + ry - uy, centreZ + rz - uz,
                SKY_RED, SKY_GREEN, SKY_BLUE, alpha, 0.5F);
        vertex(consumer, matrix, centreX + rx + ux, centreY + ry + uy, centreZ + rz + uz,
                SKY_RED, SKY_GREEN, SKY_BLUE, alpha, 0.5F);
        vertex(consumer, matrix, centreX - rx + ux, centreY - ry + uy, centreZ - rz + uz,
                SKY_RED, SKY_GREEN, SKY_BLUE, alpha, 0.5F);
    }

    // ===============================================================================================
    //  One ritual
    // ===============================================================================================

    private static void drawRitual(VertexConsumer consumer, Matrix4f matrix, ClientLevel level,
                                   BlockPos center, float fade, float time, Vec3 camera) {
        if (fade <= 0.004F) {
            return;
        }

        // The altar's top face is the anchor of the whole look: the pillar starts there, the squares
        // and the wavefield are measured from it.
        double baseX = center.getX() + 0.5D;
        double baseY = center.getY() + 1.0D;
        double baseZ = center.getZ() + 0.5D;

        // Pulsing: the sine is cubed, so the aura spends most of the cycle at full strength and only
        // dips sharply at the dark end - a punchier breathing than the plain linear sine ramp.
        float swing = 0.5F + 0.5F * Mth.sin(time * (float) (Math.PI * 2.0D) / PULSE_PERIOD_TICKS);
        float pulse = 1.0F - PULSE_DEPTH * swing * swing * swing;
        // Entrance: ease-out cubic, so the aura slams in and then settles instead of fading in linearly
        // (1 - (1 - x)^3).
        float auraFade = easeOutCubic(fade) * pulse;

        drawPillar(consumer, matrix, baseX, baseY, baseZ, camera, auraFade);
        drawSquare(consumer, matrix, baseX, baseY + SQUARE_LOW_OFFSET, baseZ, SQUARE_LOW_SIDE,
                camera, auraFade);
        drawSquare(consumer, matrix, baseX, baseY + SQUARE_HIGH_OFFSET, baseZ, SQUARE_HIGH_SIDE,
                camera, auraFade);
        drawRings(consumer, matrix, level, center, baseX, baseY, baseZ, camera, fade, time);
    }

    /** Two crossed panels (plus their halo layers) from the altar top up into the sky. */
    private static void drawPillar(VertexConsumer consumer, Matrix4f matrix,
                                   double x, double y0, double z, Vec3 camera, float alpha) {
        double top = y0 + PILLAR_HEIGHT;
        // Panel A spans X (its normal is Z), panel B spans Z (its normal is X): crossed, so the beam
        // reads as a column from any direction, exactly like a beacon beam.
        emitVerticalPanel(consumer, matrix, x, y0, z, camera, 1.0D, 0.0D, PILLAR_WIDTH,
                alpha * PILLAR_ALPHA, alpha * PILLAR_TOP_ALPHA);
        emitVerticalPanel(consumer, matrix, x, y0, z, camera, 0.0D, 1.0D, PILLAR_WIDTH,
                alpha * PILLAR_ALPHA, alpha * PILLAR_TOP_ALPHA);
        emitVerticalPanel(consumer, matrix, x, y0, z, camera, 1.0D, 0.0D, PILLAR_GLOW_WIDTH,
                alpha * PILLAR_ALPHA * PILLAR_GLOW_ALPHA, alpha * PILLAR_TOP_ALPHA);
        emitVerticalPanel(consumer, matrix, x, y0, z, camera, 0.0D, 1.0D, PILLAR_GLOW_WIDTH,
                alpha * PILLAR_ALPHA * PILLAR_GLOW_ALPHA, alpha * PILLAR_TOP_ALPHA);
    }

    /**
     * One horizontal square rotated 45 degrees: four glowing prism edges with the corners on the axes.
     *
     * <p>The edges are solid bars now, not flat quads: each one is emitted by {@link #emitEdgePrism}
     * with a {@link #SQUARE_THICKNESS} x {@link #SQUARE_THICKNESS} block cross-section, so the square
     * has real volume from every viewing angle. A second, wider and much dimmer prism keeps the same
     * halo the flat lines had.</p>
     */
    private static void drawSquare(VertexConsumer consumer, Matrix4f matrix,
                                   double x, double y, double z, float side, Vec3 camera, float alpha) {
        // A 45-degree square of side `side` has its corners at +-side/sqrt(2) on the two horizontal
        // axes, i.e. a diamond whose half diagonal is side / sqrt(2).
        double half = side / Math.sqrt(2.0D);
        float a = alpha * SQUARE_ALPHA;
        // Core prism half extents, and the halo prism's: LINE_HALO_WIDTH/LINE_HALO_ALPHA keep working
        // exactly as they did for the flat lines.
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

    /** The expanding diagonal wavefield: {@link #RING_COUNT} rings in the air and on the block tops. */
    private static void drawRings(VertexConsumer consumer, Matrix4f matrix, ClientLevel level,
                                  BlockPos center, double x, double y, double z, Vec3 camera,
                                  float fade, float time) {
        for (int ring = 0; ring < RING_COUNT; ring++) {
            // Each ring owns a phase slot, so they leave the centre one after another.
            float phase = wrap01(time / RING_PERIOD_TICKS + (float) ring / RING_COUNT);
            // Two speed curves for force: the radius uses the ease-out cubic (1 - (1 - x)^3), so the
            // ring shoots out of the altar and then slows down towards RING_MAX_RADIUS, and the fade is
            // quadratic, so the ring stays bright while it races and vanishes snappily at the rim.
            float radius = easeOutCubic(phase) * RING_MAX_RADIUS;
            float ringFade = (1.0F - phase) * (1.0F - phase) * fade;
            if (ringFade <= 0.02F || radius <= 0.05F) {
                continue;
            }

            // (1) the ring itself, floating in the air just above the altar top.
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

            // (2) the same diagonal projected onto the nearby block top surfaces. A block column counts
            // when the diamond |X| + |Z| = radius actually crosses its cell; the piece inside the cell
            // is found by clipping the line exactly and interpolating it linearly, so the pieces of
            // neighbouring cells join up into one straight diamond line (and, once the ring is wide, a
            // single edge or a diamond corner per cell with the mirrored half in the cell opposite).
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

    /**
     * True when the diamond {@code |X| + |Z| = radius} may cross the cell
     * {@code [dx, dx + 1] x [dz, dz + 1]}: the radius has to lie between the smallest and the largest
     * corner diagonal of that cell. The test is deliberately a conservative bound - a cell it lets
     * through that the line misses simply emits nothing.
     */
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

    /**
     * Emits the piece of the diamond edge that crosses one block cell.
     *
     * <p>The edge inside the cell is not approximated by a segment through the cell centre along the
     * local tangent any more: the edge line of the cell's quadrant is clipped to the cell exactly and
     * its two end points are obtained by <b>linear interpolation</b> along the line, so the pieces of
     * neighbouring cells line up into a single straight diamond edge instead of a stepped chain.</p>
     */
    private static void emitDiagonalCell(VertexConsumer consumer, Matrix4f matrix,
                                         double cellOriginX, double surfaceY, double cellOriginZ,
                                         int dx, int dz, double radius, float alpha, Vec3 camera) {
        // A cell lies in exactly one quadrant (dx == 0 and dx == -1 only touch the axis with an edge),
        // so only that quadrant's edge of the diamond can cross it: sx * X + sz * Z = radius.
        double sx = dx >= 0 ? 1.0D : -1.0D;
        double sz = dz >= 0 ? 1.0D : -1.0D;
        double x0 = dx;
        double x1 = dx + 1.0D;
        double z0 = dz;
        double z1 = dz + 1.0D;

        // Walk the edge over the cell's X range and interpolate Z linearly on it.
        double ax = x0;
        double bx = x1;
        double az = (radius - sx * ax) / sz;
        double bz = (radius - sx * bx) / sz;
        double zSpan = bz - az;

        // Clip the parameter range [0,1] to the part where the interpolated Z stays inside the cell.
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

        // Both end points, linearly interpolated between the cell's X bounds.
        double px0 = ax + (bx - ax) * t0;
        double pz0 = az + zSpan * t0;
        double px1 = ax + (bx - ax) * t1;
        double pz1 = az + zSpan * t1;
        emitSegment(consumer, matrix,
                cellOriginX + px0, surfaceY, cellOriginZ + pz0,
                cellOriginX + px1, surfaceY, cellOriginZ + pz1,
                RING_LINE_WIDTH, WAVE_RED, WAVE_GREEN, WAVE_BLUE, alpha, camera);
    }

    // ===============================================================================================
    //  Quad emitters
    // ===============================================================================================

    /**
     * One vertical panel of the given width, centred on {@code (x, z)} and spanning {@code y0..y0+}
     * {@link #PILLAR_HEIGHT}, with its normal along {@code (normalX, normalZ)}. The bottom vertices get
     * {@code bottomAlpha}, the top ones {@code topAlpha}, so the panel fades along its length.
     */
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

    /**
     * One horizontal glowing segment of the given width, from {@code (x1, y, z1)} to
     * {@code (x2, y, z2)}. The width runs along the quad's V axis, which the shader turns into the soft
     * glow profile.
     */
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
        // Cheap distance probe: a ritual the server does not even announce from far away never costs
        // more than this one test.
        double midX = (x1 + x2) * 0.5D - camera.x;
        double midY = y1 - camera.y;
        double midZ = (z1 + z2) * 0.5D - camera.z;
        if (midX * midX + midY * midY + midZ * midZ > MAX_SEGMENT_DISTANCE * MAX_SEGMENT_DISTANCE) {
            return;
        }
        emitSegmentQuad(consumer, matrix, x1, y1, z1, x2, y2, z2, width, red, green, blue, alpha);
        // Halo: the same line, wider and much dimmer, so a hard-edged quad still reads as a glow.
        emitSegmentQuad(consumer, matrix, x1, y1, z1, x2, y2, z2,
                width * LINE_HALO_WIDTH, red, green, blue, alpha * LINE_HALO_ALPHA);
    }

    /** One flat quad of the given width from {@code (x1, y1, z1)} to {@code (x2, y2, z2)}. */
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

    /**
     * One square prism along the horizontal edge {@code (x1, y1, z1) -> (x2, y2, z2)}: the four side
     * faces of a bar whose cross-section is {@code 2 * halfThickness} wide and {@code 2 * halfHeight}
     * tall (the two are equal for the {@link #SQUARE_THICKNESS} squares).
     *
     * <p>The perpendicular is the horizontal unit vector {@code (-dz, dx) / length}, i.e. the same
     * offset direction {@link #emitSegmentQuad} uses, and the faces are emitted as plain winding-less
     * quads - the pipeline has culling off, so the order of the faces does not matter.</p>
     */
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
        // Same cheap distance probe the flat segment emitter does: a ritual the server does not even
        // announce from far away never costs more than this one test.
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

        // Top face, at y + halfHeight, from -n to +n.
        vertex(consumer, matrix, x1 - ox, y1 + halfHeight, z1 - oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 - ox, y2 + halfHeight, z2 - oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 + ox, y2 + halfHeight, z2 + oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x1 + ox, y1 + halfHeight, z1 + oz, red, green, blue, alpha, 1.0F);
        // Bottom face, at y - halfHeight.
        vertex(consumer, matrix, x1 - ox, y1 - halfHeight, z1 - oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x1 + ox, y1 - halfHeight, z1 + oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x2 + ox, y2 - halfHeight, z2 + oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x2 - ox, y2 - halfHeight, z2 - oz, red, green, blue, alpha, 0.0F);
        // Side face on +n, spanning y - halfHeight .. y + halfHeight.
        vertex(consumer, matrix, x1 + ox, y1 - halfHeight, z1 + oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 + ox, y2 - halfHeight, z2 + oz, red, green, blue, alpha, 0.0F);
        vertex(consumer, matrix, x2 + ox, y2 + halfHeight, z2 + oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x1 + ox, y1 + halfHeight, z1 + oz, red, green, blue, alpha, 1.0F);
        // Side face on -n.
        vertex(consumer, matrix, x1 - ox, y1 - halfHeight, z1 - oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x1 - ox, y1 + halfHeight, z1 - oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x2 - ox, y2 + halfHeight, z2 - oz, red, green, blue, alpha, 1.0F);
        vertex(consumer, matrix, x2 - ox, y2 - halfHeight, z2 - oz, red, green, blue, alpha, 1.0F);
    }

    /**
     * Emits one vertex in camera-relative world space.
     *
     * <p>The pipeline's format is {@code POSITION_COLOR}, so only the position and the colour are
     * written; {@code v} is kept in the signature because every caller already computes the width
     * coordinate (and a future soft profile could use it again).</p>
     */
    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               double x, double y, double z,
                               float red, float green, float blue, float alpha, float v) {
        debugVertices++;
        consumer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    /** Wraps any value into {@code [0,1)}. */
    private static float wrap01(float value) {
        float wrapped = value % 1.0F;
        return wrapped < 0.0F ? wrapped + 1.0F : wrapped;
    }

    /**
     * The "1 - (1 - x)^3" speed curve the request asks for: a fast start that gently settles into the
     * end value, which is what makes a motion driven by it read as a punch instead of a linear ramp.
     * Defined for {@code x} in {@code [0,1]}.
     */
    private static float easeOutCubic(float x) {
        float c = 1.0F - x;
        return 1.0F - c * c * c;
    }
}
