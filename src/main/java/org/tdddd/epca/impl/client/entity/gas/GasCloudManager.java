package org.tdddd.epca.impl.client.entity.gas;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.ContaminatedWater;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeYelloweye;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client-only manager for the shader-rendered gas clouds.
 *
 * <p>Everything here is derived from state that is already synced to the client
 * ({@code ReshapeLongarms#isGassing()}, {@code ReshapeLongarms#getGassingTimer()},
 * {@code ReshapeYelloweye#isGassing()}, {@code ReshapeYelloweye.DATA_GASSING_TIMER}), so no new
 * packets are introduced and the clouds appear for every client that can see the mob, not only
 * for the player who triggered the skill.</p>
 *
 * <p>The manager is ticked once per client tick from {@code ClientEvents}. It</p>
 * <ol>
 *   <li>spawns the longarms active clouds on the rising edge of the active jet skill,</li>
 *   <li>spawns one longarms passive cloud per 20-tick back emission while the back part is gone,</li>
 *   <li>spawns yelloweye clouds at the cadence of that skill's own particle emission,</li>
 *   <li>scans the volume of every visible {@code ContaminatedWater} entity for water blocks and
 *       spawns the water-touching small clouds and the dark-red micro rectangles ({@link WaterSpec})
 *       there,</li>
 *   <li>ages every cloud and every speck and drops the expired ones.</li>
 * </ol>
 *
 * <p>Clouds are held in one flat list plus a per-owner index so the render layer can fetch only
 * what it needs; the water specks have their own list and index because their motion differs, and
 * both share the global {@link #MAX_CLOUDS} cap. Everything is cleared whenever the client level
 * changes.</p>
 *
 * <h2>26.1.2</h2>
 * <p>Port of the 1.20.1 manager. Two version differences:</p>
 * <ul>
 *   <li>{@code Camera#getPosition()} became {@code Camera#position()}.</li>
 *   <li>{@code ReshapeYelloweye} exposes {@code DATA_GASSING_TIMER} as a public accessor but has
 *       no {@code getGassingTimer()} getter in this tree, so the timer is read straight from
 *       {@code Entity#getEntityData()} instead of touching the entity class.</li>
 * </ul>
 */
public final class GasCloudManager {

    // =================================================================
    //  Diagnostics
    // =================================================================

    /**
     * Master switch for the temporary {@code [gascloud]} diagnostics.
     *
     * <p>Set to {@code false} to silence every log line this class and {@link GasCloudLayer} add;
     * nothing else depends on it, so removing the diagnostics later is a pure delete.</p>
     */
    public static final boolean DEBUG = false;

    /** Minimum wall-clock gap between two diagnostics for the same owner while its state repeats. */
    private static final long DEBUG_REPEAT_MS = 1000L;

    /**
     * Per-owner diagnostics state, keyed by entity id: {@code [0] = last logged state code,
     * [1] = last log wall-clock in ms, [2] = how many times this owner has been logged}.
     */
    private static final Map<Integer, long[]> DEBUG_OWNER_STATE = new HashMap<>();

    /** Total clouds spawned since the last reset, for the end-of-run summary. */
    private static int debugTotalSpawned;
    /** Sum of the cloud counts reported by the render layer, used for the cross-check summary. */
    private static int debugLayerDrawCalls;
    private static int debugLayerCloudsDrawn;
    /** Last logged passive-scan signature, so the scan line only prints when the counts change. */
    private static Long DEBUG_PASSIVE_SIGNATURE;
    /** True once the first tick of a level has been logged, so the tick hook proof prints once. */
    private static boolean debugTickLogged;

    private static final int DBG_STATE_NONE = 0;
    private static final int DBG_STATE_IDLE = 1;
    private static final int DBG_STATE_GASSING = 2;
    private static final int DBG_STATE_PASSIVE = 3;
    private static final int DBG_STATE_WATER = 4;

    // =================================================================
    //  Longarms active jet skill
    // =================================================================

    /** Spec: exactly one big cloud per activation. */
    public static final int ACTIVE_BIG_CLOUD_COUNT = 1;
    /** Spec: 2-4 small clouds per activation. */
    public static final int ACTIVE_SMALL_CLOUD_MIN = 2;
    public static final int ACTIVE_SMALL_CLOUD_MAX = 4;
    /** Spec: big cloud 1.2 -> 3.5, reaching 3.5 at ~45% of its life. */
    public static final float BIG_CLOUD_BASE_SCALE = 1.2F;
    public static final float BIG_CLOUD_TARGET_SCALE = 3.5F;
    public static final float BIG_CLOUD_GROW_FRACTION = 0.45F;
    /** Spec: small active clouds 0.8 -> 1.6. */
    public static final float ACTIVE_SMALL_BASE_SCALE = 0.8F;
    public static final float ACTIVE_SMALL_TARGET_SCALE = 1.6F;
    public static final float ACTIVE_SMALL_GROW_FRACTION = 0.65F;
    /** Spec: longarms active clouds (big and small) start at alpha 0.9. */
    public static final float ACTIVE_CLOUD_ALPHA = 0.9F;
    /** Radius of the longarms active skill emit area, mirrored from its own spawn box (+/- 3.5 blocks). */
    public static final double ACTIVE_EMIT_RADIUS = 3.5D;
    /** Height above the entity feet where the active jet emits (mirrors spawnMovingInfestiveGasParticles). */
    public static final double ACTIVE_EMIT_HEIGHT = 2.0D;
    /**
     * Spec: "the active big cloud may live as long as GASSING_DURATION if that is longer".
     * The longarms gassing skill lasts 50 ticks while the particle lifetime is 45 ticks, so the
     * big cloud uses the longer 50-tick duration. Every other cloud uses the particle lifetime.
     */
    public static final int BIG_CLOUD_LIFETIME = 50;

    // =================================================================
    //  Shared lifetime
    // =================================================================

    /** Spec: lifetime of every cloud is 45 ticks, matching the INFESTIVE_GAS particle lifetime. */
    public static final int CLOUD_LIFETIME = 45;

    // =================================================================
    //  Longarms passive jet
    // =================================================================

    /** Spec: passive small clouds 0.5 -> 1.0. */
    public static final float PASSIVE_BASE_SCALE = 0.5F;
    public static final float PASSIVE_TARGET_SCALE = 1.0F;
    public static final float PASSIVE_GROW_FRACTION = 0.6F;
    /** Spec: passive clouds use the lower alpha 0.45. */
    public static final float PASSIVE_CLOUD_ALPHA = 0.45F;
    /** The passive jet emits one particle batch every 20 ticks (mirrors the server gasEmitTimer). */
    public static final int PASSIVE_EMIT_INTERVAL = 20;
    /** Radius of the passive emit scatter (mirrors the passive jet's +/- 3 block x/z spread). */
    public static final double PASSIVE_EMIT_RADIUS_XZ = 3.0D;
    /** Height range of the passive emit scatter (mirrors the passive jet's 5 block y spread). */
    public static final double PASSIVE_EMIT_HEIGHT_RANGE = 5.0D;
    /** Small outward drift for the passive puffs. */
    public static final double PASSIVE_DRIFT_XZ = 0.25D;
    /** Small upward drift for the passive puffs. */
    public static final double PASSIVE_DRIFT_Y = 0.18D;
    /** The owner must have existed for this long before the passive jet is considered active. */
    private static final int PASSIVE_MIN_ENTITY_AGE = 40;

    // =================================================================
    //  Yelloweye jet skill
    // =================================================================

    /** Spec: yelloweye 0.7 -> 1.4. */
    public static final float YELLOWEYE_BASE_SCALE = 0.7F;
    public static final float YELLOWEYE_TARGET_SCALE = 1.4F;
    public static final float YELLOWEYE_GROW_FRACTION = 0.6F;
    /** Spec: yelloweye alpha identical to the longarms active cloud. */
    public static final float YELLOWEYE_CLOUD_ALPHA = 0.9F;
    /** Spec: hard cap of 6 simultaneous yelloweye clouds (far fewer than the particles). */
    public static final int YELLOWEYE_MAX_CLOUDS = 6;
    /** Yelloweye gassing duration, mirrors {@code ReshapeYelloweye.GASSING_DURATION}. */
    public static final int YELLOWEYE_GASSING_DURATION = 30;
    /**
     * Elapsed tick (counted from the start of the skill) at which the skill's particle line burst
     * fires.
     *
     * <p>{@code ReshapeYelloweye}'s server loop emits the burst when its remaining-timer field equals
     * {@code GASSING_DURATION - 10} <em>and writes that same remaining value into
     * {@code DATA_GASSING_TIMER}</em>. The client therefore sees {@code timer == 20} on the burst
     * tick, i.e. {@code elapsed = GASSING_DURATION - timer = 10}. The old
     * {@code elapsed == GASSING_DURATION - 10} test compared an elapsed value against a remaining
     * value: it matched 10 ticks AFTER the burst, in the last third of the skill, so the rendered
     * clouds could only ever appear (and could be missed entirely on a mid-skill start) while the
     * skill's own particles were long since visible.</p>
     */
    private static final int YELLOWEYE_PARTICLE_START_ELAPSED = 10;
    /** After the burst the skill emits one particle every 2 ticks. */
    private static final int YELLOWEYE_PARTICLE_PERIOD = 2;
    /**
     * Real gas range, taken from the skill's own particle spawn code:
     * {@code spawnSingleGassingParticle} offsets a particle by (0.6, -0.3, 0.4) from a centre at
     * {@code position() + (0, bbHeight * 0.5, 0)}, and {@code spawnGassingParticleLine} repeats that
     * offset up to {@code t = 0.8}. That gives a maximum horizontal reach of 0.6 * (1 + 0.8) = 1.08
     * blocks. (The separate {@code applyGassingAreaEffect} box only spans the mob's own body width,
     * half-width 1.375, and is the tighter of the two; the particle reach is used because the spec
     * asks the clouds to match the particles the mob actually emits.)
     */
    public static final double YELLOWEYE_PARTICLE_OFFSET_X = 0.6D;
    public static final double YELLOWEYE_PARTICLE_OFFSET_Y = -0.3D;
    public static final double YELLOWEYE_PARTICLE_OFFSET_Z = 0.4D;
    /** Maximum multiplier of the particle line offset (the line uses t = 0, 0.4, 0.8). */
    public static final double YELLOWEYE_LINE_MAX_T = 0.8D;
    /** Half-extent of the spawn scatter around the mob centre. */
    public static final double YELLOWEYE_RANGE = YELLOWEYE_PARTICLE_OFFSET_X * (1.0D + YELLOWEYE_LINE_MAX_T);
    /** Vertical half-extent of the spawn scatter around the mob centre. */
    public static final double YELLOWEYE_VERTICAL_RANGE =
            Math.abs(YELLOWEYE_PARTICLE_OFFSET_Y) * (1.0D + YELLOWEYE_LINE_MAX_T);
    /** Forward offset used when picking the flank spawn point. */
    public static final double YELLOWEYE_FORWARD_OFFSET = YELLOWEYE_PARTICLE_OFFSET_Z;

    // =================================================================
    //  Contaminated water
    // =================================================================

    /**
     * Spec: 3-5 small red gas clouds float on the part of the water entity's volume that touches
     * water. They use the very same {@link GasCloud} geometry as the reshape mobs, with their own
     * smaller scale and a lower start alpha.
     */
    public static final int WATER_CLOUD_MIN = 3;
    public static final int WATER_CLOUD_MAX = 5;
    /** One cloud is spawned every this many ticks while the pool is below the maximum. */
    public static final int WATER_CLOUD_SPAWN_INTERVAL = 8;
    /** Spec: water clouds live 45 ticks, like every other cloud. */
    public static final int WATER_CLOUD_LIFETIME = 45;
    /** Spec: water clouds grow from 0.5 to 0.9, reaching 0.9 at half of their life. */
    public static final float WATER_CLOUD_BASE_SCALE = 0.5F;
    public static final float WATER_CLOUD_TARGET_SCALE = 0.9F;
    public static final float WATER_CLOUD_GROW_FRACTION = 0.5F;
    /** Spec: water clouds start at alpha 0.55 and fade linearly to 0. */
    public static final float WATER_CLOUD_ALPHA = 0.55F;
    /** Small random outward drift of a water cloud, in blocks over its whole life. */
    public static final double WATER_CLOUD_DRIFT_XZ = 0.12D;
    /** Small random upward drift of a water cloud, in blocks over its whole life. */
    public static final double WATER_CLOUD_DRIFT_Y = 0.05D;

    /**
     * Spec: 10-24 concurrent dark-red micro rectangles ("specks") per water entity, spawned 1-3 at
     * a time every {@link #WATER_SPEC_SPAWN_INTERVAL} ticks. Below the minimum the batch is topped
     * up immediately so the pool reaches {@link #WATER_SPEC_MIN} instead of ramping up slowly.
     */
    public static final int WATER_SPEC_MIN = 10;
    public static final int WATER_SPEC_MAX = 24;
    public static final int WATER_SPEC_SPAWN_INTERVAL = 4;
    public static final int WATER_SPEC_SPAWN_MIN = 1;
    public static final int WATER_SPEC_SPAWN_MAX = 3;
    /** Spec: each speck lives a random 30-80 ticks. */
    public static final int WATER_SPEC_LIFETIME_MIN = 30;
    public static final int WATER_SPEC_LIFETIME_MAX = 80;

    /** The cached water block list of one entity is refreshed every this many ticks. */
    public static final int WATER_BLOCK_REFRESH_INTERVAL = 20;
    /**
     * Upper bound on the scanned volume, in blocks. The registered entity is 5x5 and its bounding
     * box is inflated by 2.5 blocks in the constructor (at most 10x10x10 = 1000 blocks), so this
     * cap never triggers for a healthy entity - it only makes an absurd or corrupted box cheap to
     * reject instead of scanning it every 20 ticks.
     */
    public static final int WATER_SCAN_MAX_BLOCKS = 4096;
    /** Inset used when a spawn point is picked inside a water block, so it stays inside it. */
    public static final double WATER_POS_INSET = 0.05D;

    // =================================================================
    //  Global limits
    // =================================================================

    /**
     * Spec: cap the total instance count so nothing can leak.
     *
     * <p>Raised from 64 to 512 for the contaminated water effect: one water entity can hold up to
     * {@link #WATER_CLOUD_MAX} clouds plus {@link #WATER_SPEC_MAX} specks at once, and the entity
     * lives 200 ticks while several of them can be visible together, so 64 would have throttled the
     * new effect almost immediately. 512 still bounds the geometry at a few thousand quads, and a
     * speck is a single quad, so the cost stays trivial.</p>
     */
    public static final int MAX_CLOUDS = 512;
    /** Only mobs within this distance of the camera get clouds. */
    private static final double MAX_OWNER_DISTANCE = 96.0D;
    private static final double MAX_OWNER_DISTANCE_SQR = MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE;

    private static final List<GasCloud> CLOUDS = new ArrayList<>();
    private static final Map<Integer, List<GasCloud>> BY_OWNER = new HashMap<>();
    /**
     * Dark-red micro rectangles of the contaminated water effect. They live in their own list
     * because their motion and their shader style differ from a gas cloud, but they share the same
     * global cap and the same rendering helper.
     */
    private static final List<WaterSpec> SPECS = new ArrayList<>();
    private static final Map<Integer, List<WaterSpec>> SPECS_BY_OWNER = new HashMap<>();
    /** Cached water block lists per contaminated water entity id, refreshed every 20 ticks. */
    private static final Map<Integer, WaterScan> WATER_SCANS = new HashMap<>();
    /**
     * Longarms owners whose current gassing activation has already spawned its cloud batch.
     *
     * <p>This is the activation latch: {@code Set#add} returning {@code true} means "this owner has
     * not spawned for the current activation yet". It therefore spawns on the rising edge of
     * {@code isGassing()} AND on the very first observation of an owner that is already gassing
     * (fresh client join, mob entering view mid-skill, a synced flag that was already {@code true}
     * when the manager first saw the entity), while a repeated {@code true} without an intervening
     * {@code false} can never spawn twice. No per-owner guard can suppress a first activation.</p>
     */
    private static final Set<UUID> ACTIVE_SPAWNED = new HashSet<>();
    /** Last observed skill elapsed tick per yelloweye entity, for rising-edge detection. */
    private static final Map<UUID, Integer> YELLOWEYE_LAST_ELAPSED = new HashMap<>();
    /** Yelloweye clouds already spawned during the current activation. */
    private static final Map<UUID, Integer> YELLOWEYE_SPAWNED = new HashMap<>();
    /** Longarms entity ids whose back part is currently gone (the passive jet's precondition). */
    private static final Set<Integer> PASSIVE_ACTIVE_OWNERS = new HashSet<>();
    /** Longarms entity ids whose first passive cloud has already been reported (diagnostics only). */
    private static final Set<Integer> DEBUG_PASSIVE_REPORTED = new HashSet<>();
    /** Identity of the client level the current state belongs to; a change resets everything. */
    private static ClientLevel levelIdentity;

    private GasCloudManager() {
    }

    // =================================================================
    //  Diagnostics
    // =================================================================

    /**
     * Records that the manager observed {@code ownerId} in the given state and emits one
     * {@code [gascloud]} line when the state is new (or at most once per {@link #DEBUG_REPEAT_MS}
     * while it keeps repeating). Never called when {@link #DEBUG} is false.
     */
    private static void debugOwnerState(int ownerId, String ownerKind, int state, String detail) {
        long now = System.currentTimeMillis();
        long[] slot = DEBUG_OWNER_STATE.get(ownerId);
        if (slot == null) {
            slot = new long[]{Long.MIN_VALUE, 0L, 0L};
            DEBUG_OWNER_STATE.put(ownerId, slot);
        }
        boolean stateChanged = slot[0] != state;
        if (!stateChanged && now - slot[1] < DEBUG_REPEAT_MS) {
            return;
        }
        slot[0] = state;
        slot[1] = now;
        slot[2]++;
        epca.LOGGER.info("[gascloud] manager: owner={} id={} state={} logs={} clouds={} {}",
                ownerKind, ownerId, debugStateName(state), slot[2], CLOUDS.size(), detail);
    }

    private static String debugStateName(int state) {
        return switch (state) {
            case DBG_STATE_GASSING -> "GASSING";
            case DBG_STATE_PASSIVE -> "PASSIVE-OWNER";
            case DBG_STATE_WATER -> "WATER";
            case DBG_STATE_IDLE -> "IDLE";
            default -> "UNSEEN";
        };
    }

    /** One-line summary of the cloud list, used by the layer cross-check. */
    public static String debugSummary() {
        return "total=" + CLOUDS.size()
                + " owners=" + BY_OWNER.size()
                + " specs=" + SPECS.size()
                + " spawned=" + debugTotalSpawned
                + " passiveOwners=" + PASSIVE_ACTIVE_OWNERS.size()
                + " layerDraws=" + debugLayerDrawCalls
                + " layerClouds=" + debugLayerCloudsDrawn;
    }

    /** Called by {@link GasCloudLayer} once per submitted draw so the summary can be cross-checked. */
    public static void debugLayerDrew(int cloudCount) {
        debugLayerDrawCalls++;
        debugLayerCloudsDrawn += cloudCount;
    }

    // =================================================================
    //  Tick entry point
    // =================================================================

    /** Called once per client tick from the mod's client tick hook. */
    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            reset();
            return;
        }
        if (levelIdentity != level) {
            reset();
            levelIdentity = level;
        }

        refreshPassiveOwners(level);
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();
        if (DEBUG && !debugTickLogged) {
            debugTickLogged = true;
            epca.LOGGER.info("[gascloud] manager: client tick hook is running camera=({},{},{}) {}",
                    fmt(cameraPos.x), fmt(cameraPos.y), fmt(cameraPos.z),
                    "pipelineRegistered=" + GasCloudRenderType.isPipelineRegistered());
        }

        Set<Integer> liveOwners = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ReshapeLongarms longarms) {
                liveOwners.add(longarms.getId());
                tickLongarms(longarms, cameraPos);
            } else if (entity instanceof ReshapeYelloweye yelloweye) {
                liveOwners.add(yelloweye.getId());
                tickYelloweye(yelloweye, cameraPos);
            } else if (entity instanceof ContaminatedWater water) {
                liveOwners.add(water.getId());
                tickContaminatedWater(level, water, cameraPos);
            }
        }

        ageClouds();
        pruneOrphanedOwners(liveOwners);
    }

    /**
     * Drops the clouds of owners that are no longer in the level. The lifetime already bounds every
     * cloud, so this is only there to satisfy "nothing may outlive its owner" (a removed mob leaves
     * no trail behind).
     */
    private static void pruneOrphanedOwners(Set<Integer> liveOwnerIds) {
        Iterator<GasCloud> iterator = CLOUDS.iterator();
        while (iterator.hasNext()) {
            GasCloud cloud = iterator.next();
            if (!liveOwnerIds.contains(cloud.getOwnerEntityId())) {
                iterator.remove();
                removeFromOwnerIndex(cloud);
            }
        }
        Iterator<WaterSpec> specIterator = SPECS.iterator();
        while (specIterator.hasNext()) {
            WaterSpec spec = specIterator.next();
            if (!liveOwnerIds.contains(spec.getOwnerEntityId())) {
                specIterator.remove();
                removeSpecFromOwnerIndex(spec);
            }
        }
        // The cached water block lists belong to entities too; drop them with the entity.
        WATER_SCANS.keySet().removeIf(ownerId -> !liveOwnerIds.contains(ownerId));
    }

    private static void tickLongarms(ReshapeLongarms longarms, Vec3 cameraPos) {
        UUID id = longarms.getUUID();
        if (longarms.distanceToSqr(cameraPos) > MAX_OWNER_DISTANCE_SQR) {
            // Out of sight: forget the activation latch so the next visible activation spawns again.
            ACTIVE_SPAWNED.remove(id);
            if (DEBUG) {
                debugOwnerState(longarms.getId(), "longarms", DBG_STATE_NONE,
                        "SKIPPED out-of-range distSqr=" + (long) longarms.distanceToSqr(cameraPos)
                                + " limit=" + (long) MAX_OWNER_DISTANCE_SQR);
            }
            return;
        }

        RandomSource random = longarms.getRandom();
        boolean gassing = longarms.isGassing();

        // -- Active jet skill: one big cloud + 2-4 small ones, exactly once per activation. --
        // The latch is set on the rising edge AND on the first observation of an owner that is
        // already gassing, so a fresh client join or a mob that enters view mid-skill cannot be
        // suppressed; a repeated `true` without an intervening `false` cannot spawn twice.
        if (!gassing) {
            ACTIVE_SPAWNED.remove(id);
            if (DEBUG) {
                debugOwnerState(longarms.getId(), "longarms", DBG_STATE_IDLE,
                        "idle isGassing=false tickCount=" + longarms.tickCount
                                + " passiveOwner=" + PASSIVE_ACTIVE_OWNERS.contains(longarms.getId()));
            }
        } else if (ACTIVE_SPAWNED.add(id)) {
            if (DEBUG) {
                debugOwnerState(longarms.getId(), "longarms", DBG_STATE_GASSING,
                        "EDGE isGassing=true pos=(" + fmt(longarms.getX()) + "," + fmt(longarms.getY())
                                + "," + fmt(longarms.getZ()) + ") tickCount=" + longarms.tickCount);
            }
            spawnLongarmsActiveClouds(random, longarms);
        }

        // -- Passive jet: one cloud per 20-tick back emission, never while the active skill runs. --
        if (gassing || longarms.tickCount < PASSIVE_MIN_ENTITY_AGE) {
            if (DEBUG && !gassing) {
                debugOwnerState(longarms.getId(), "longarms", DBG_STATE_IDLE,
                        "passive-blocked: entity too young (tickCount=" + longarms.tickCount
                                + " < " + PASSIVE_MIN_ENTITY_AGE + ")");
            }
            return;
        }
        if (!PASSIVE_ACTIVE_OWNERS.contains(longarms.getId())) {
            if (DEBUG) {
                debugOwnerState(longarms.getId(), "longarms", DBG_STATE_IDLE,
                        "PASSIVE-BLOCKED: back part still present (PASSIVE_ACTIVE_OWNERS="
                                + PASSIVE_ACTIVE_OWNERS + ")");
            }
            return;
        }
        if (Math.floorMod(longarms.tickCount, PASSIVE_EMIT_INTERVAL) == 0) {
            spawnLongarmsPassiveCloud(random, longarms);
        } else if (DEBUG) {
            debugOwnerState(longarms.getId(), "longarms", DBG_STATE_PASSIVE,
                    "passive-active owner, waiting for emit tick (tickCount=" + longarms.tickCount
                            + " % " + PASSIVE_EMIT_INTERVAL + " = "
                            + Math.floorMod(longarms.tickCount, PASSIVE_EMIT_INTERVAL) + ")");
        }
    }

    private static void tickYelloweye(ReshapeYelloweye yelloweye, Vec3 cameraPos) {
        UUID id = yelloweye.getUUID();
        if (yelloweye.distanceToSqr(cameraPos) > MAX_OWNER_DISTANCE_SQR) {
            YELLOWEYE_LAST_ELAPSED.remove(id);
            YELLOWEYE_SPAWNED.remove(id);
            if (DEBUG) {
                debugOwnerState(yelloweye.getId(), "yelloweye", DBG_STATE_NONE,
                        "SKIPPED out-of-range distSqr=" + (long) yelloweye.distanceToSqr(cameraPos)
                                + " limit=" + (long) MAX_OWNER_DISTANCE_SQR);
            }
            return;
        }

        // 26.1.2: DATA_GASSING_TIMER is public but the entity exposes no getter in this tree.
        int timer = yelloweye.getEntityData().get(ReshapeYelloweye.DATA_GASSING_TIMER);
        boolean gassing = yelloweye.isGassing();
        if (!gassing || timer <= 0) {
            YELLOWEYE_LAST_ELAPSED.remove(id);
            YELLOWEYE_SPAWNED.remove(id);
            if (DEBUG) {
                debugOwnerState(yelloweye.getId(), "yelloweye", DBG_STATE_IDLE,
                        "idle gassing=" + gassing + " timer=" + timer);
            }
            return;
        }

        int elapsed = YELLOWEYE_GASSING_DURATION - timer;
        Integer previousElapsed = YELLOWEYE_LAST_ELAPSED.get(id);
        if (previousElapsed == null || elapsed <= previousElapsed) {
            // Rising edge of the skill, or a timer reset: a new emission run starts.
            YELLOWEYE_SPAWNED.put(id, 0);
        }
        YELLOWEYE_LAST_ELAPSED.put(id, elapsed);

        int spawned = YELLOWEYE_SPAWNED.getOrDefault(id, 0);
        // Same cadence as spawnGassingParticleLine() / spawnSingleGassingParticle(): the burst at
        // elapsed == 10, then one cloud every 2 ticks. `spawned == 0` additionally covers a
        // mid-skill start (the first observed tick at or after the burst spawns immediately), so a
        // cloud run can never be skipped just because no client tick landed exactly on the burst.
        boolean onCadence = elapsed >= YELLOWEYE_PARTICLE_START_ELAPSED
                && (elapsed - YELLOWEYE_PARTICLE_START_ELAPSED) % YELLOWEYE_PARTICLE_PERIOD == 0;
        boolean midSkillStart = spawned == 0 && elapsed >= YELLOWEYE_PARTICLE_START_ELAPSED;
        if (DEBUG) {
            debugOwnerState(yelloweye.getId(), "yelloweye", DBG_STATE_GASSING,
                    "gassing timer=" + timer + " elapsed=" + elapsed + " spawned=" + spawned
                            + " onCadence=" + onCadence + " midSkillStart=" + midSkillStart);
        }
        if (!onCadence && !midSkillStart) {
            return;
        }
        if (spawned >= YELLOWEYE_MAX_CLOUDS || countOwned(yelloweye.getId()) >= YELLOWEYE_MAX_CLOUDS) {
            return;
        }
        if (spawnYelloweyeCloud(yelloweye.getRandom(), yelloweye)) {
            YELLOWEYE_SPAWNED.put(id, spawned + 1);
        }
    }

    private static void ageClouds() {
        Iterator<GasCloud> iterator = CLOUDS.iterator();
        while (iterator.hasNext()) {
            GasCloud cloud = iterator.next();
            if (!cloud.tick()) {
                iterator.remove();
                removeFromOwnerIndex(cloud);
            }
        }
        // Water specks carry their own RandomSource for the drift re-randomisation, so they age on
        // their own.
        Iterator<WaterSpec> specIterator = SPECS.iterator();
        while (specIterator.hasNext()) {
            WaterSpec spec = specIterator.next();
            if (!spec.tick()) {
                specIterator.remove();
                removeSpecFromOwnerIndex(spec);
            }
        }
    }

    // =================================================================
    //  Spawning
    // =================================================================

    private static void spawnLongarmsActiveClouds(RandomSource random, ReshapeLongarms longarms) {
        int ownerId = longarms.getId();
        Vec3 emitPoint = new Vec3(longarms.getX(), longarms.getY() + ACTIVE_EMIT_HEIGHT, longarms.getZ());

        // One big cloud, anchored at the emit point, growing far more than it drifts.
        spawn(new GasCloud(ownerId, emitPoint, BIG_CLOUD_LIFETIME,
                BIG_CLOUD_BASE_SCALE, BIG_CLOUD_TARGET_SCALE, BIG_CLOUD_GROW_FRACTION,
                ACTIVE_CLOUD_ALPHA, Vec3.ZERO, true, false, random));
        int spawned = 1;

        // 2-4 small clouds at random points inside the skill's own emit box.
        int smallCount = ACTIVE_SMALL_CLOUD_MIN
                + random.nextInt(ACTIVE_SMALL_CLOUD_MAX - ACTIVE_SMALL_CLOUD_MIN + 1);
        for (int i = 0; i < smallCount; i++) {
            if (reachedCap()) {
                break;
            }
            Vec3 point = randomPointInBox(random, emitPoint, ACTIVE_EMIT_RADIUS);
            spawn(new GasCloud(ownerId, point, CLOUD_LIFETIME,
                    ACTIVE_SMALL_BASE_SCALE, ACTIVE_SMALL_TARGET_SCALE, ACTIVE_SMALL_GROW_FRACTION,
                    ACTIVE_CLOUD_ALPHA, Vec3.ZERO, true, false, random));
            spawned++;
        }

        debugFirstSpawn("longarms-active", ownerId, emitPoint, spawned,
                BIG_CLOUD_BASE_SCALE, BIG_CLOUD_TARGET_SCALE, ACTIVE_CLOUD_ALPHA);
    }

    private static void spawnLongarmsPassiveCloud(RandomSource random, ReshapeLongarms longarms) {
        if (reachedCap()) {
            return;
        }
        // Mirrors the passive emit scatter: +/- 3 blocks in x/z at the mob's feet, up to 5 blocks up.
        Vec3 offset = new Vec3(
                (random.nextDouble() - 0.5D) * 2.0D * PASSIVE_EMIT_RADIUS_XZ,
                random.nextDouble() * PASSIVE_EMIT_HEIGHT_RANGE,
                (random.nextDouble() - 0.5D) * 2.0D * PASSIVE_EMIT_RADIUS_XZ);
        Vec3 origin = longarms.position().add(offset);
        Vec3 drift = new Vec3(
                (random.nextDouble() - 0.5D) * 2.0D * PASSIVE_DRIFT_XZ,
                PASSIVE_DRIFT_Y,
                (random.nextDouble() - 0.5D) * 2.0D * PASSIVE_DRIFT_XZ);
        spawn(new GasCloud(longarms.getId(), origin, CLOUD_LIFETIME,
                PASSIVE_BASE_SCALE, PASSIVE_TARGET_SCALE, PASSIVE_GROW_FRACTION,
                PASSIVE_CLOUD_ALPHA, drift, false, false, random));

        if (DEBUG_PASSIVE_REPORTED.add(longarms.getId())) {
            debugFirstSpawn("longarms-passive", longarms.getId(), origin, 1,
                    PASSIVE_BASE_SCALE, PASSIVE_TARGET_SCALE, PASSIVE_CLOUD_ALPHA);
        }
    }

    private static boolean spawnYelloweyeCloud(RandomSource random, ReshapeYelloweye yelloweye) {
        if (reachedCap()) {
            return false;
        }
        Vec3 center = yelloweye.position().add(0.0D, yelloweye.getBbHeight() * 0.5D, 0.0D);
        // Pick one of the two flanks the skill's own particles use, then scatter within that range.
        double side = random.nextBoolean() ? 1.0D : -1.0D;
        Vec3 flank = new Vec3(YELLOWEYE_PARTICLE_OFFSET_X * side,
                YELLOWEYE_PARTICLE_OFFSET_Y,
                YELLOWEYE_FORWARD_OFFSET);
        double t = random.nextDouble() * YELLOWEYE_LINE_MAX_T;
        Vec3 point = center.add(flank.scale(t)).add(
                (random.nextDouble() - 0.5D) * 2.0D * YELLOWEYE_RANGE,
                (random.nextDouble() - 0.5D) * 2.0D * YELLOWEYE_VERTICAL_RANGE,
                (random.nextDouble() - 0.5D) * 2.0D * YELLOWEYE_RANGE);
        spawn(new GasCloud(yelloweye.getId(), point, CLOUD_LIFETIME,
                YELLOWEYE_BASE_SCALE, YELLOWEYE_TARGET_SCALE, YELLOWEYE_GROW_FRACTION,
                YELLOWEYE_CLOUD_ALPHA, Vec3.ZERO, true, true, random));

        // Only the first cloud of an activation is reported; YELLOWEYE_SPAWNED is reset per
        // activation by tickYelloweye, so this stays one line per jet skill.
        if (YELLOWEYE_SPAWNED.getOrDefault(yelloweye.getId(), 0) == 0) {
            debugFirstSpawn("yelloweye", yelloweye.getId(), point, 1,
                    YELLOWEYE_BASE_SCALE, YELLOWEYE_TARGET_SCALE, YELLOWEYE_CLOUD_ALPHA);
        }
        return true;
    }

    private static Vec3 randomPointInBox(RandomSource random, Vec3 center, double radius) {
        return new Vec3(
                center.x + (random.nextDouble() - 0.5D) * 2.0D * radius,
                center.y + (random.nextDouble() - 0.5D) * 2.0D * radius,
                center.z + (random.nextDouble() - 0.5D) * 2.0D * radius);
    }

    // =================================================================
    //  Contaminated water
    // =================================================================

    /**
     * Cached water block positions of one contaminated water entity.
     *
     * <p>The list is rebuilt at most every {@link #WATER_BLOCK_REFRESH_INTERVAL} ticks, so the block
     * lookup stays cheap even though a spawn decision is taken every few ticks. {@code surface}
     * holds the positions with no water above them (the water surface), {@code suspended} the ones
     * with water above (inside the body of water); {@code water} is the union.</p>
     */
    private static final class WaterScan {
        private final List<BlockPos> water = new ArrayList<>();
        private final List<BlockPos> surface = new ArrayList<>();
        private final List<BlockPos> suspended = new ArrayList<>();
        private int refreshTicks;
        private int cloudCooldown;
        private int specCooldown;
        /** Last water block count reported to the diagnostics, so the line only prints on change. */
        private int loggedWaterBlocks = -1;
        /** Set once a box bigger than {@link #WATER_SCAN_MAX_BLOCKS} has been reported. */
        private boolean loggedHugeBox;
    }

    /**
     * Client-side tick of one contaminated water entity: maintains the cached water block list and
     * spawns the water-touching gas clouds and the dark-red specks. Everything is derived from the
     * entity's own synced position and its bounding box; no packet and no entity data is involved.
     */
    private static void tickContaminatedWater(ClientLevel level, ContaminatedWater water, Vec3 cameraPos) {
        int ownerId = water.getId();
        if (water.distanceToSqr(cameraPos) > MAX_OWNER_DISTANCE_SQR) {
            if (DEBUG) {
                debugOwnerState(ownerId, "contaminated_water", DBG_STATE_NONE,
                        "SKIPPED out-of-range distSqr=" + (long) water.distanceToSqr(cameraPos)
                                + " limit=" + (long) MAX_OWNER_DISTANCE_SQR);
            }
            return;
        }

        WaterScan scan = WATER_SCANS.computeIfAbsent(ownerId, key -> new WaterScan());
        if (scan.refreshTicks <= 0) {
            refreshWaterScan(level, water, scan);
            scan.refreshTicks = WATER_BLOCK_REFRESH_INTERVAL;
        } else {
            scan.refreshTicks--;
        }

        if (scan.water.isEmpty()) {
            // The effect only exists where the entity actually touches water.
            if (DEBUG && scan.loggedWaterBlocks != 0) {
                scan.loggedWaterBlocks = 0;
                debugOwnerState(ownerId, "contaminated_water", DBG_STATE_WATER,
                        "no water block inside the entity volume - nothing spawned");
            }
            return;
        }
        if (DEBUG && scan.loggedWaterBlocks != scan.water.size()) {
            scan.loggedWaterBlocks = scan.water.size();
            debugOwnerState(ownerId, "contaminated_water", DBG_STATE_WATER,
                    "waterBlocks=" + scan.water.size() + " surface=" + scan.surface.size()
                            + " suspended=" + scan.suspended.size()
                            + " clouds=" + countOwned(ownerId) + " specks=" + countSpecsOwned(ownerId));
        }

        if (scan.cloudCooldown <= 0) {
            scan.cloudCooldown = WATER_CLOUD_SPAWN_INTERVAL;
            int owned = countOwned(ownerId);
            // Below the minimum the pool is topped up at once; afterwards one cloud per interval.
            int wanted = owned < WATER_CLOUD_MIN ? WATER_CLOUD_MIN - owned : 1;
            for (int i = 0; i < wanted && countOwned(ownerId) < WATER_CLOUD_MAX && !reachedCap(); i++) {
                spawnWaterCloud(water, scan);
            }
        } else {
            scan.cloudCooldown--;
        }

        if (scan.specCooldown <= 0) {
            scan.specCooldown = WATER_SPEC_SPAWN_INTERVAL;
            int owned = countSpecsOwned(ownerId);
            int wanted = owned < WATER_SPEC_MIN
                    ? WATER_SPEC_MIN - owned
                    : WATER_SPEC_SPAWN_MIN
                            + water.getRandom().nextInt(WATER_SPEC_SPAWN_MAX - WATER_SPEC_SPAWN_MIN + 1);
            for (int i = 0; i < wanted && countSpecsOwned(ownerId) < WATER_SPEC_MAX && !reachedCap(); i++) {
                spawnWaterSpec(water, scan);
            }
        } else {
            scan.specCooldown--;
        }
    }

    /**
     * Rebuilds the cached water block lists of one entity.
     *
     * <p>Iterates every block position inside the entity's bounding box and keeps the ones whose
     * fluid state is in {@code FluidTags.WATER}. A position is a surface position when the block
     * above it is not water, and a suspended position when it is. The box is skipped entirely when
     * its volume exceeds {@link #WATER_SCAN_MAX_BLOCKS}.</p>
     */
    private static void refreshWaterScan(ClientLevel level, ContaminatedWater water, WaterScan scan) {
        scan.water.clear();
        scan.surface.clear();
        scan.suspended.clear();
        AABB box = water.getBoundingBox();
        double volume = box.getXsize() * box.getYsize() * box.getZsize();
        if (volume > (double) WATER_SCAN_MAX_BLOCKS) {
            if (DEBUG && !scan.loggedHugeBox) {
                scan.loggedHugeBox = true;
                debugOwnerState(water.getId(), "contaminated_water", DBG_STATE_WATER,
                        "SKIPPED box volume " + (long) volume + " > " + WATER_SCAN_MAX_BLOCKS);
            }
            return;
        }
        // betweenClosed(box) is the vanilla form of the loop over the blocks the box overlaps; it
        // excludes the far boundary itself, so a 5x5 volume is 125 positions, not 216.
        for (BlockPos pos : BlockPos.betweenClosed(box)) {
            // betweenClosed reuses one mutable position, so the kept positions must be immutable.
            BlockPos immutable = pos.immutable();
            if (!level.getBlockState(immutable).getFluidState().is(FluidTags.WATER)) {
                continue;
            }
            scan.water.add(immutable);
            boolean surface = !level.getBlockState(immutable.above()).getFluidState().is(FluidTags.WATER);
            if (surface) {
                scan.surface.add(immutable);
            } else {
                scan.suspended.add(immutable);
            }
        }
    }

    /** Spawns one water-touching small gas cloud at a random water position of the cached list. */
    private static void spawnWaterCloud(ContaminatedWater water, WaterScan scan) {
        if (reachedCap() || scan.water.isEmpty()) {
            return;
        }
        RandomSource random = water.getRandom();
        BlockPos pos = scan.water.get(random.nextInt(scan.water.size()));
        float fluidHeight = levelFluidHeight(water, pos);
        double x = pos.getX() + WATER_POS_INSET
                + random.nextDouble() * (1.0D - 2.0D * WATER_POS_INSET);
        double y = pos.getY() + WATER_POS_INSET
                + random.nextDouble() * Math.max(fluidHeight - 2.0D * WATER_POS_INSET, 0.0F);
        double z = pos.getZ() + WATER_POS_INSET
                + random.nextDouble() * (1.0D - 2.0D * WATER_POS_INSET);
        Vec3 drift = new Vec3(
                (random.nextDouble() * 2.0D - 1.0D) * WATER_CLOUD_DRIFT_XZ,
                random.nextDouble() * WATER_CLOUD_DRIFT_Y,
                (random.nextDouble() * 2.0D - 1.0D) * WATER_CLOUD_DRIFT_XZ);
        spawn(new GasCloud(water.getId(), new Vec3(x, y, z), WATER_CLOUD_LIFETIME,
                WATER_CLOUD_BASE_SCALE, WATER_CLOUD_TARGET_SCALE, WATER_CLOUD_GROW_FRACTION,
                WATER_CLOUD_ALPHA, drift, false, false, random));
    }

    /**
     * Spawns one dark-red speck. Half of them are {@link WaterSpec.Kind#SURFACE} (locked to the
     * fluid surface of a block with no water above it) and half {@link WaterSpec.Kind#SUSPENDED}
     * (inside the body of water); when one of the two pools is empty the other one is used instead
     * so a fully submerged or a very shallow water volume still shows the effect.
     */
    private static void spawnWaterSpec(ContaminatedWater water, WaterScan scan) {
        if (reachedCap()) {
            return;
        }
        RandomSource random = water.getRandom();
        boolean surfaceKind = random.nextBoolean();
        List<BlockPos> pool = surfaceKind ? scan.surface : scan.suspended;
        if (pool.isEmpty()) {
            surfaceKind = !surfaceKind;
            pool = surfaceKind ? scan.surface : scan.suspended;
            if (pool.isEmpty()) {
                return;
            }
        }
        BlockPos pos = pool.get(random.nextInt(pool.size()));
        double x = pos.getX() + WATER_POS_INSET
                + random.nextDouble() * (1.0D - 2.0D * WATER_POS_INSET);
        double z = pos.getZ() + WATER_POS_INSET
                + random.nextDouble() * (1.0D - 2.0D * WATER_POS_INSET);
        double y;
        if (surfaceKind) {
            // Locked to the water surface: block bottom + the fluid's own height (8/9 for a water
            // source) + the anti-z-fight lift. Only the sine bob in WaterSpec moves it afterwards.
            y = pos.getY() + fluidOwnHeight(water, pos) + WaterSpec.WATER_SPEC_SURFACE_LIFT;
        } else {
            y = pos.getY() + WATER_POS_INSET
                    + random.nextDouble() * (1.0D - 2.0D * WATER_POS_INSET);
        }
        Vec3 drift = surfaceKind
                ? WaterSpec.randomSurfaceDrift(random)
                : WaterSpec.randomSuspendedDrift(random);
        int lifetime = WATER_SPEC_LIFETIME_MIN
                + random.nextInt(WATER_SPEC_LIFETIME_MAX - WATER_SPEC_LIFETIME_MIN + 1);
        float halfExtent = WaterSpec.WATER_SPEC_HALF_EXTENT_MIN
                + random.nextFloat() * (WaterSpec.WATER_SPEC_HALF_EXTENT_MAX
                        - WaterSpec.WATER_SPEC_HALF_EXTENT_MIN);
        float alpha = WaterSpec.WATER_SPEC_ALPHA_MIN
                + random.nextFloat() * (WaterSpec.WATER_SPEC_ALPHA_MAX - WaterSpec.WATER_SPEC_ALPHA_MIN);
        // Each speck owns the RandomSource it uses for its slow direction changes.
        RandomSource specRandom = RandomSource.create(random.nextLong());
        spawnSpec(new WaterSpec(water.getId(),
                surfaceKind ? WaterSpec.Kind.SURFACE : WaterSpec.Kind.SUSPENDED,
                new Vec3(x, y, z), drift, lifetime, halfExtent, alpha,
                random.nextFloat() * (float) (Math.PI * 2.0D), specRandom));
    }

    /**
     * Fluid surface height of one water block, in blocks relative to the block's own bottom.
     * {@code FluidState#getHeight(BlockGetter, BlockPos)} is 1.0 for a block with the same fluid
     * above it and {@code getOwnHeight()} (8/9 for a water source) otherwise. Used for the water
     * cloud spawn height, so a cloud can sit anywhere between the block bottom and that surface.
     */
    private static float levelFluidHeight(ContaminatedWater water, BlockPos pos) {
        return water.level().getBlockState(pos).getFluidState().getHeight(water.level(), pos);
    }

    /**
     * Own height of the fluid in one water block ({@code FluidState#getOwnHeight()}, i.e. the
     * source height 8/9), measured from the block's own bottom. A surface position has no water
     * above it, so this is exactly the visible water surface that a surface speck is locked to.
     */
    private static float fluidOwnHeight(ContaminatedWater water, BlockPos pos) {
        return water.level().getBlockState(pos).getFluidState().getOwnHeight();
    }

    /** Adds one speck to the shared list and to its owner index, unless the global cap is reached. */
    private static void spawnSpec(WaterSpec spec) {
        if (reachedCap()) {
            if (DEBUG) {
                epca.LOGGER.info("[gascloud] manager: SPECK-DROPPED cap reached ({}/{}) for owner={}",
                        CLOUDS.size() + SPECS.size(), MAX_CLOUDS, spec.getOwnerEntityId());
            }
            return;
        }
        SPECS.add(spec);
        SPECS_BY_OWNER.computeIfAbsent(spec.getOwnerEntityId(), key -> new ArrayList<>()).add(spec);
    }

    private static void spawn(GasCloud cloud) {
        if (reachedCap()) {
            if (DEBUG) {
                epca.LOGGER.info("[gascloud] manager: SPAWN-DROPPED cap reached ({}/{}) for owner={}",
                        CLOUDS.size(), MAX_CLOUDS, cloud.getOwnerEntityId());
            }
            return;
        }
        CLOUDS.add(cloud);
        BY_OWNER.computeIfAbsent(cloud.getOwnerEntityId(), key -> new ArrayList<>()).add(cloud);
        if (DEBUG) {
            debugTotalSpawned++;
        }
    }

    /**
     * Prints the single "first cloud batch of this owner" diagnostic line.
     *
     * <p>Dead code while {@link #DEBUG} is off (the flag is a compile-time constant, so the branch
     * is dropped by javac) and otherwise called at most once per activation.</p>
     */
    private static void debugFirstSpawn(String kind, int ownerId, Vec3 position, int count,
                                        float baseScale, float targetScale, float alpha) {
        if (!DEBUG) {
            return;
        }
        epca.LOGGER.info("[gascloud] manager: {} owner={} pos=({}, {}, {}) spawned={} scale={}->{} alpha={}",
                kind, ownerId, position.x, position.y, position.z,
                count, baseScale, targetScale, alpha);
    }

    /** Short numeric formatter for the diagnostics (two decimals IS enough for positions). */
    private static String fmt(double value) {
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }

    private static String fmt(float value) {
        return String.valueOf(Math.round(value * 1000.0F) / 1000.0F);
    }

    private static boolean reachedCap() {
        return CLOUDS.size() + SPECS.size() >= MAX_CLOUDS;
    }

    private static void removeFromOwnerIndex(GasCloud cloud) {
        List<GasCloud> owned = BY_OWNER.get(cloud.getOwnerEntityId());
        if (owned == null) {
            return;
        }
        owned.remove(cloud);
        if (owned.isEmpty()) {
            BY_OWNER.remove(cloud.getOwnerEntityId());
        }
    }

    private static void removeSpecFromOwnerIndex(WaterSpec spec) {
        List<WaterSpec> owned = SPECS_BY_OWNER.get(spec.getOwnerEntityId());
        if (owned == null) {
            return;
        }
        owned.remove(spec);
        if (owned.isEmpty()) {
            SPECS_BY_OWNER.remove(spec.getOwnerEntityId());
        }
    }

    private static int countOwned(int ownerEntityId) {
        List<GasCloud> owned = BY_OWNER.get(ownerEntityId);
        return owned == null ? 0 : owned.size();
    }

    private static int countSpecsOwned(int ownerEntityId) {
        List<WaterSpec> owned = SPECS_BY_OWNER.get(ownerEntityId);
        return owned == null ? 0 : owned.size();
    }

    // =================================================================
    //  Passive jet detection
    // =================================================================

    /**
     * Fills {@link #PASSIVE_ACTIVE_OWNERS} with the longarms entities whose back part is gone.
     *
     * <p>{@code ReshapeLongarms#isBackPartRemoved()} is only ever set server-side (its
     * {@code removeBackPart} returns early on the client), so the client cannot read it. The back
     * part itself is a synced {@code ReshapeLongarms.CustomPart} entity carrying its owner's entity
     * id, so the client-visible equivalent of "back part removed" is "no back part entity exists
     * for this owner any more".</p>
     */
    private static void refreshPassiveOwners(ClientLevel level) {
        PASSIVE_ACTIVE_OWNERS.clear();
        Set<Integer> ownersWithBackPart = new HashSet<>();
        int partEntities = 0;
        int backParts = 0;
        int longarmsEntities = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ReshapeLongarms.CustomPart part) {
                partEntities++;
                if (part.isBackPart()) {
                    backParts++;
                    ownersWithBackPart.add(part.getOwnerEntityId());
                }
            }
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ReshapeLongarms longarms) {
                longarmsEntities++;
                if (!ownersWithBackPart.contains(longarms.getId())) {
                    PASSIVE_ACTIVE_OWNERS.add(longarms.getId());
                }
            }
        }
        if (DEBUG) {
            // Only interesting when the counts change; the per-owner state lines carry the rest.
            long signature = ((long) partEntities << 32) ^ ((long) backParts << 16) ^ longarmsEntities;
            Long previous = DEBUG_PASSIVE_SIGNATURE;
            if (previous == null || previous != signature) {
                DEBUG_PASSIVE_SIGNATURE = signature;
                epca.LOGGER.info("[gascloud] manager: passive scan parts={} backParts={} longarms={} "
                                + "ownersWithBackPart={} passiveActive={}",
                        partEntities, backParts, longarmsEntities, ownersWithBackPart, PASSIVE_ACTIVE_OWNERS);
            }
        }
    }

    // =================================================================
    //  Queries used by the render layer
    // =================================================================

    /** Every live cloud owned by the given entity id; empty when there is none. */
    public static List<GasCloud> getCloudsFor(int ownerEntityId) {
        List<GasCloud> owned = BY_OWNER.get(ownerEntityId);
        return owned == null ? Collections.emptyList() : owned;
    }

    public static boolean hasCloudsFor(int ownerEntityId) {
        List<GasCloud> owned = BY_OWNER.get(ownerEntityId);
        return owned != null && !owned.isEmpty();
    }

    public static int getTotalCloudCount() {
        return CLOUDS.size();
    }

    /** Every live water speck owned by the given entity id; empty when there is none. */
    public static List<WaterSpec> getSpecsFor(int ownerEntityId) {
        List<WaterSpec> owned = SPECS_BY_OWNER.get(ownerEntityId);
        return owned == null ? Collections.emptyList() : owned;
    }

    public static boolean hasSpecsFor(int ownerEntityId) {
        List<WaterSpec> owned = SPECS_BY_OWNER.get(ownerEntityId);
        return owned != null && !owned.isEmpty();
    }

    /**
     * Number of water blocks currently cached for the given contaminated water entity, or 0 while
     * the entity has no cache entry. Used by the renderer's once-per-second diagnostics.
     */
    public static int getWaterBlockCount(int ownerEntityId) {
        WaterScan scan = WATER_SCANS.get(ownerEntityId);
        return scan == null ? 0 : scan.water.size();
    }

    /** Total specks tracked, for the diagnostics. */
    public static int getTotalSpecCount() {
        return SPECS.size();
    }

    /** Clears every tracked cloud and edge state; used when the client level changes. */
    public static void clearAll() {
        reset();
    }

    private static void reset() {
        CLOUDS.clear();
        BY_OWNER.clear();
        SPECS.clear();
        SPECS_BY_OWNER.clear();
        WATER_SCANS.clear();
        ACTIVE_SPAWNED.clear();
        YELLOWEYE_LAST_ELAPSED.clear();
        YELLOWEYE_SPAWNED.clear();
        PASSIVE_ACTIVE_OWNERS.clear();
        DEBUG_PASSIVE_REPORTED.clear();
        levelIdentity = null;
        debugTotalSpawned = 0;
        debugLayerDrawCalls = 0;
        debugLayerCloudsDrawn = 0;
        DEBUG_OWNER_STATE.clear();
        DEBUG_PASSIVE_SIGNATURE = null;
        debugTickLogged = false;
    }
}
