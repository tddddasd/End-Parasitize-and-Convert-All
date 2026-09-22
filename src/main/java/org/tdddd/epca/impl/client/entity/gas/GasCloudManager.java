package org.tdddd.epca.impl.client.entity.gas;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
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
 * {@code ReshapeYelloweye#isGassing()}, {@code ReshapeYelloweye#getGassingTimer()}, and the water
 * blocks around a {@code ContaminatedWater} entity), so no new packets are introduced and the
 * clouds appear for every client that can see the entity.</p>
 *
 * <p>The manager is ticked once per client tick from {@code ClientEvents}. It</p>
 * <ol>
 *   <li>spawns the longarms active clouds on the rising edge of the active jet skill,</li>
 *   <li>spawns one longarms passive cloud per 20-tick back emission while the back part is gone,</li>
 *   <li>spawns yelloweye clouds at the cadence that skill's original gas emission used,</li>
 *   <li>spawns the water-touching red clouds and the dark-red micro rectangles of every visible
 *       {@code epca:contaminated_water} entity (see the "Contaminated water" section),</li>
 *   <li>ages every cloud and speck and drops the expired ones.</li>
 * </ol>
 *
 * <p>Clouds are held in one flat list plus a per-owner index so a renderer can fetch only what it
 * needs. A hard cap keeps the list from growing without bound, and everything is cleared whenever
 * the client level changes.</p>
 */
public final class GasCloudManager {

    // =================================================================
    //  Diagnostics
    // =================================================================

    /**
     * Single on/off switch for every {@code [gascloud]} diagnostic line of the feature.
     *
     * <p>While it is on, at most a handful of lines are printed per gassing activation:</p>
     * <ul>
     *   <li>one {@code [gascloud] manager:} line the first time a gassing owner is seen and a cloud
     *       batch is spawned (kind, owner entity id, position, count, scale range, alpha);</li>
     *   <li>one {@code [gascloud] layer:} line per second per owning entity in the render submit
     *       path (clouds drawn / tracked, shader-ready state, alpha range);</li>
     *   <li>one {@code [gascloud] water:} line per second per contaminated-water entity (water
     *       blocks found, clouds alive, specks alive);</li>
     *   <li>one single {@code [gascloud] layer: shader NOT ready} line if the custom core shader was
     *       never built, plus one {@code [gascloud] shader: ... built} line whenever it is.</li>
     * </ul>
     *
     * <p>Set this to {@code false} to silence the feature completely (the guards make every log
     * statement unreachable); the flag is the only switch and it never changes behaviour.</p>
     */
    public static final boolean DEBUG = false;

    /** Milliseconds between two diagnostic lines of the same kind for the same owner. */
    private static final long DEBUG_LOG_INTERVAL_MS = 1000L;
    /** Upper bound on the per-owner throttle map, so it can never grow with every id a session saw. */
    private static final int DEBUG_THROTTLE_MAX_ENTRIES = 256;

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
    /** Height above the entity feet where the active jet emits (mirrors the active skill's emit point). */
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
    /** The passive jet applies its gas effect every 20 ticks (mirrors the server gasEmitTimer); the clouds follow it. */
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
    /** Yelloweye gassing duration, mirrors ReshapeYelloweye.GASSING_DURATION. */
    public static final int YELLOWEYE_GASSING_DURATION = 30;
    /**
     * Elapsed tick (counted from the start of the skill) at which the skill's original particle
     * line burst fired; the shader-rendered cloud burst keeps that cadence.
     *
     * <p>{@code ReshapeYelloweye}'s server loop triggered the burst when its remaining-timer field
     * equaled {@code GASSING_DURATION - 10} <em>and wrote that same remaining value into
     * {@code DATA_GASSING_TIMER}</em> (the particles themselves have since been removed; only the
     * cadence below still uses this). The client therefore sees {@code timer == 20} on the burst
     * tick, i.e. {@code elapsed = GASSING_DURATION - timer = 10}. The old
     * {@code elapsed == GASSING_DURATION - 10} test compared an elapsed value against a remaining
     * value: it matched 10 ticks AFTER the burst, in the last third of the skill, so the rendered
     * clouds could only ever appear (and could be missed entirely on a mid-skill start) long after
     * the skill's effects had begun.</p>
     */
    private static final int YELLOWEYE_PARTICLE_START_ELAPSED = 10;
    /** After the burst the skill used to emit one particle every 2 ticks; the clouds follow it. */
    private static final int YELLOWEYE_PARTICLE_PERIOD = 2;
    /**
     * Real gas range, taken from the skill's original particle spawn code (now removed; the
     * constants are kept because the clouds must cover the same volume):
     * the single-particle emitter offset a particle by (0.6, -0.3, 0.4) from a centre at
     * {@code position() + (0, bbHeight * 0.5, 0)}, and the line burst repeated that offset up to
     * {@code t = 0.8}. That gives a maximum horizontal reach of 0.6 * (1 + 0.8) = 1.08 blocks; the
     * longarms 3.5 block cloud size is deliberately NOT used here.
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
    //  Contaminated water (epca:contaminated_water)
    // =================================================================

    /**
     * Spec: 3-5 concurrent small red gas clouds per water entity, and they only exist where the
     * entity actually touches water (nothing is spawned for an entity whose box holds no water).
     */
    public static final int WATER_CLOUD_MIN = 3;
    public static final int WATER_CLOUD_MAX = 5;
    /** Spec: one cloud every 8 ticks while below {@link #WATER_CLOUD_MAX}. */
    public static final int WATER_CLOUD_SPAWN_INTERVAL = 8;
    /** Spec: water clouds scale 0.5 -> 0.9. */
    public static final float WATER_CLOUD_BASE_SCALE = 0.5F;
    public static final float WATER_CLOUD_TARGET_SCALE = 0.9F;
    /** Spec: they reach the target scale at 50% of their life. */
    public static final float WATER_CLOUD_GROW_FRACTION = 0.5F;
    /** Spec: they start at alpha 0.55 and fade linearly to 0 over their whole life. */
    public static final float WATER_CLOUD_ALPHA = 0.55F;
    /** Spec: their lifetime is the shared 45-tick cloud lifetime. */
    public static final int WATER_CLOUD_LIFETIME = CLOUD_LIFETIME;
    /**
     * "Small random outward drift" of a water cloud, as the total world offset applied over its
     * whole life (so 0.12 / 45 ~= 0.003 blocks per tick in x/z and 0.05 / 45 ~= 0.001 upward). Kept
     * well below the cloud radius so a cloud stays around the water pocket it was spawned in.
     * Deliberately identical to the 26.1.2 twin's values.
     */
    public static final double WATER_CLOUD_DRIFT_XZ = 0.12D;
    public static final double WATER_CLOUD_DRIFT_Y = 0.05D;

    /** Spec: 10-24 concurrent dark-red micro rectangles per water entity. */
    public static final int WATER_SPEC_MIN = 10;
    public static final int WATER_SPEC_MAX = 24;
    /** Spec: 1-3 specks per spawn, every 4 ticks, while below {@link #WATER_SPEC_MAX}. */
    public static final int WATER_SPEC_SPAWN_INTERVAL = 4;
    public static final int WATER_SPEC_SPAWN_MIN = 1;
    public static final int WATER_SPEC_SPAWN_MAX = 3;

    /**
     * How often the (cheap but not free) water block scan of one entity is repeated. Everything in
     * between is served from {@link #WATER_STATES}. ~1 second is far below the speed at which a
     * water pocket changes shape, and it keeps the scan cost at one block pass per second per
     * entity instead of one per tick.
     */
    public static final int WATER_SCAN_INTERVAL_TICKS = 20;
    /**
     * Upper bound on the number of block positions one scan may touch, and on how many water
     * positions are kept from it. The normal entity box is 10x10x10 = 1000 positions; anything
     * larger is refused outright (see {@link #WATER_SCAN_MAX_VOLUME}) rather than scanned, so the
     * cost of the feature stays bounded even if an entity's box is ever changed.
     */
    public static final int WATER_SCAN_MAX_VOLUME = 4096;
    /** Cap on how many water positions are retained per entity. */
    public static final int WATER_SCAN_MAX_POSITIONS = 512;
    /**
     * Cap on how many water entities are scanned in one client tick. Several entities can overlap,
     * and this bounds the worst-case block work per tick; entities past the cap are picked up on a
     * later tick because the scan set is rotated by the tick counter.
     */
    public static final int WATER_MAX_OWNERS_PER_TICK = 32;
    /** Kick-in delay before the first scan, so an entity that just spawned is not scanned twice. */
    private static final int WATER_SCAN_STAGGER_TICKS = 4;
    /** Small random x/z jitter of a spawn point inside its water block, as a fraction of the block. */
    private static final double WATER_SPAWN_XZ_JITTER = 0.4D;
    /** Vertical (downward) jitter of a spawn point inside its water block, in blocks. */
    private static final double WATER_SPAWN_Y_JITTER = 0.25D;
    /**
     * Vertical (downward) jitter of a SUSPENDED speck, so the kind is not a flat sheet.
     */
    private static final double WATER_SUSPENDED_Y_JITTER = 0.3D;

    // =================================================================
    //  Global limits
    // =================================================================

    /**
     * Cap on the total number of tracked quads owners (clouds + water specks).
     *
     * <p>Raised from 64 to 512 for the contaminated water effect: one water entity alone may hold
     * {@link #WATER_CLOUD_MAX} = 5 clouds and {@link #WATER_SPEC_MAX} = 24 specks, and several of
     * them can overlap, so 64 would have starved the reshape-mob clouds. 512 stays bounded (at most
     * ~512 billboard quads, i.e. the transient buffer's size class) and is never reached by the mob
     * clouds alone (a handful per activation).</p>
     */
    public static final int MAX_CLOUDS = 512;
    /** Only entities within this distance of the camera get clouds. */
    private static final double MAX_OWNER_DISTANCE = 96.0D;
    private static final double MAX_OWNER_DISTANCE_SQR = MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE;
    /** Tick gap after which per-owner bookkeeping for an unseen entity is dropped. */
    private static final int OWNER_STATE_TTL_TICKS = 200;

    private static final List<GasCloud> CLOUDS = new ArrayList<>();
    private static final Map<Integer, List<GasCloud>> BY_OWNER = new HashMap<>();
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
    /** Client tick at which each owner UUID was last updated, used to expire stale bookkeeping. */
    private static final Map<UUID, Long> OWNER_LAST_SEEN = new HashMap<>();
    /** Longarms entity ids whose back part is currently gone (the passive jet's precondition). */
    private static final Set<Integer> PASSIVE_ACTIVE_OWNERS = new HashSet<>();
    /** Longarms entity ids whose first passive cloud has already been reported (diagnostics only). */
    private static final Set<Integer> DEBUG_PASSIVE_REPORTED = new HashSet<>();

    // -- contaminated water bookkeeping ---------------------------------

    /** Per-water-entity scan cache and spawn state, keyed by entity id. */
    private static final Map<Integer, WaterState> WATER_STATES = new HashMap<>();
    /** Live {@code ContaminatedWater} entities of the last tick, in level iteration order. */
    private static final List<ContaminatedWater> waterOwners = new ArrayList<>();
    /** Client tick at which each water entity was last seen, used to expire stale state. */
    private static final Map<Integer, Long> WATER_LAST_SEEN = new HashMap<>();
    /** Per-entity wall clock of the last {@code [gascloud] water:} line (diagnostics only). */
    private static final Map<Integer, Long> DEBUG_WATER_LOG = new HashMap<>();

    /** Identity of the client level the current state belongs to; a change resets everything. */
    private static ClientLevel levelIdentity;
    private static long clientTickCounter;

    private GasCloudManager() {
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
            // Changing dimension / rejoining: every cloud belongs to the old level.
            reset();
            levelIdentity = level;
        }

        clientTickCounter++;
        pruneOrphanedClouds(level);
        refreshPassiveOwners(level);
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

        refreshWaterOwners(level, cameraPos);

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ReshapeLongarms longarms) {
                tickLongarms(longarms, cameraPos);
            } else if (entity instanceof ReshapeYelloweye yelloweye) {
                tickYelloweye(yelloweye, cameraPos);
            }
        }
        tickWater();

        ageClouds();
        pruneOwnerBookkeeping();
        pruneWaterBookkeeping();
    }

    /**
     * Drops clouds whose owning entity is no longer present in the client level, and forgets
     * bookkeeping for owners that have not been seen for {@link #OWNER_STATE_TTL_TICKS} ticks.
     * Without this a despawned mob could leave a cloud behind for at most its lifetime, and its
     * edge-detection state would linger forever.
     */
    private static void pruneOrphanedClouds(ClientLevel level) {
        if (CLOUDS.isEmpty() && BY_OWNER.isEmpty()) {
            return;
        }
        Set<Integer> presentIds = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            presentIds.add(entity.getId());
        }
        CLOUDS.removeIf(cloud -> !presentIds.contains(cloud.getOwnerEntityId()));
        BY_OWNER.entrySet().removeIf(entry -> {
            if (presentIds.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().clear();
            return true;
        });
    }

    private static void pruneOwnerBookkeeping() {
        OWNER_LAST_SEEN.entrySet().removeIf(entry -> {
            if (clientTickCounter - entry.getValue() <= OWNER_STATE_TTL_TICKS) {
                return false;
            }
            ACTIVE_SPAWNED.remove(entry.getKey());
            YELLOWEYE_LAST_ELAPSED.remove(entry.getKey());
            YELLOWEYE_SPAWNED.remove(entry.getKey());
            return true;
        });
    }

    /** Drops the water cache of entities that have not been visible for a while. */
    private static void pruneWaterBookkeeping() {
        if (WATER_LAST_SEEN.isEmpty()) {
            return;
        }
        WATER_LAST_SEEN.entrySet().removeIf(entry -> {
            if (clientTickCounter - entry.getValue() <= OWNER_STATE_TTL_TICKS) {
                return false;
            }
            WATER_STATES.remove(entry.getKey());
            DEBUG_WATER_LOG.remove(entry.getKey());
            return true;
        });
    }

    private static void tickLongarms(ReshapeLongarms longarms, Vec3 cameraPos) {
        UUID id = longarms.getUUID();
        OWNER_LAST_SEEN.put(id, clientTickCounter);
        if (longarms.distanceToSqr(cameraPos) > MAX_OWNER_DISTANCE_SQR) {
            // Out of sight: forget the activation latch so the next visible activation spawns again.
            ACTIVE_SPAWNED.remove(id);
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
        } else if (ACTIVE_SPAWNED.add(id)) {
            spawnLongarmsActiveClouds(random, longarms);
        }

        // -- Passive jet: one cloud per 20-tick back emission, never while the active skill runs. --
        if (gassing || longarms.tickCount < PASSIVE_MIN_ENTITY_AGE) {
            return;
        }
        if (!PASSIVE_ACTIVE_OWNERS.contains(longarms.getId())) {
            return;
        }
        if (Math.floorMod(longarms.tickCount, PASSIVE_EMIT_INTERVAL) == 0) {
            spawnLongarmsPassiveCloud(random, longarms);
        }
    }

    private static void tickYelloweye(ReshapeYelloweye yelloweye, Vec3 cameraPos) {
        UUID id = yelloweye.getUUID();
        OWNER_LAST_SEEN.put(id, clientTickCounter);
        if (yelloweye.distanceToSqr(cameraPos) > MAX_OWNER_DISTANCE_SQR) {
            YELLOWEYE_LAST_ELAPSED.remove(id);
            YELLOWEYE_SPAWNED.remove(id);
            return;
        }

        int timer = yelloweye.getGassingTimer();
        boolean gassing = yelloweye.isGassing();
        if (!gassing || timer <= 0) {
            YELLOWEYE_LAST_ELAPSED.remove(id);
            YELLOWEYE_SPAWNED.remove(id);
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
        // Same cadence the skill's original particle burst used (those particles were removed):
        // the burst at elapsed == 10, then one cloud every 2 ticks. `spawned == 0` additionally
        // covers a mid-skill start (the first observed tick at or after the burst spawns
        // immediately), so a cloud run can never be skipped just because no client tick landed
        // exactly on the burst.
        boolean onCadence = elapsed >= YELLOWEYE_PARTICLE_START_ELAPSED
                && (elapsed - YELLOWEYE_PARTICLE_START_ELAPSED) % YELLOWEYE_PARTICLE_PERIOD == 0;
        boolean midSkillStart = spawned == 0 && elapsed >= YELLOWEYE_PARTICLE_START_ELAPSED;
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
        // Specks age with the client level, not with the tick of one entity, so they still expire
        // while their owner is culled, out of range or simply rotated out of this tick's scan set.
        // clientTick() has already returned when there is no level, so it is non-null here.
        RandomSource random = Minecraft.getInstance().level.getRandom();
        for (WaterState state : WATER_STATES.values()) {
            state.ageSpecks(random);
        }
    }

    // =================================================================
    //  Contaminated water
    // =================================================================

    /**
     * Per-entity water cache and spawn state.
     *
     * <p>The block scan is the only expensive part of the water feature and is therefore cached:
     * {@link #refreshWaterState} re-runs it every {@link #WATER_SCAN_INTERVAL_TICKS} ticks and every
     * spawn in between picks from the cached lists.</p>
     */
    private static final class WaterState {
        /** Top faces of the water blocks of the entity's box (SURFACE specks spawn here). */
        private final List<Vec3> surfacePositions = new ArrayList<>();
        /** Water blocks that still have water above them (SUSPENDED specks spawn here). */
        private final List<Vec3> suspendedPositions = new ArrayList<>();
        /** The live water specks owned by this entity (their drift state lives inside them). */
        private final List<WaterSpec> specks = new ArrayList<>();
        /** Number of water blocks found by the last scan, reported by the diagnostics. */
        private int waterBlockCount;
        /** Tick at which the next water block scan of this entity runs. */
        private int nextScanTick;
        /** Tick at which the next water-cloud spawn may happen (one per 8 ticks at most). */
        private int nextCloudTick;

        /** Ages every speck of this entity and drops the expired ones. */
        private void ageSpecks(RandomSource random) {
            this.specks.removeIf(speck -> !speck.tick(random));
        }
    }

    /** Collects the visible water entities once per tick, in level iteration order. */
    private static void refreshWaterOwners(ClientLevel level, Vec3 cameraPos) {
        waterOwners.clear();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ContaminatedWater water
                    && water.distanceToSqr(cameraPos) <= MAX_OWNER_DISTANCE_SQR) {
                waterOwners.add(water);
            }
        }
    }

    /**
     * Ticks the water effect of every visible contaminated-water entity.
     *
     * <p>When more entities are visible than {@link #WATER_MAX_OWNERS_PER_TICK}, the set that is
     * serviced on this tick is rotated by the tick counter, so every entity is still updated (just
     * less often) and one tick can never do unbounded block work.</p>
     */
    private static void tickWater() {
        int limit = waterOwners.size();
        int offset = limit == 0 ? 0 : Math.floorMod((int) clientTickCounter, limit);
        int serviced = Math.min(limit, WATER_MAX_OWNERS_PER_TICK);
        for (int i = 0; i < serviced; i++) {
            ContaminatedWater water = waterOwners.get((offset + i) % limit);
            WATER_LAST_SEEN.put(water.getId(), clientTickCounter);
            tickWaterEntity(water);
        }
    }

    /** Scans (throttled), spawns and ages the water cloud/speck population of one entity. */
    private static void tickWaterEntity(ContaminatedWater water) {
        int ownerId = water.getId();
        // Entity.getRandom() does not exist in 1.20.1 (the field is protected), so take the level's
        // RandomSource, which is what the entity would have handed out anyway.
        RandomSource random = water.level().getRandom();
        WaterState state = WATER_STATES.get(ownerId);
        if (state == null) {
            // Stagger the first scan per entity so a group of new entities does not scan together.
            state = new WaterState();
            int stagger = WATER_SCAN_STAGGER_TICKS + Math.floorMod(ownerId, WATER_SCAN_INTERVAL_TICKS);
            state.nextScanTick = (int) clientTickCounter + stagger;
            state.nextCloudTick = state.nextScanTick;
            WATER_STATES.put(ownerId, state);
        }

        // The specks are aged once per tick by ageClouds() further down clientTick(), which runs for
        // every entity and not only for the ones serviced here. Both counts are read straight from
        // the live collections, so no separate counter can drift out of sync with them.
        int clouds = countOwned(ownerId);
        int specks = state.specks.size();

        if (clientTickCounter >= state.nextScanTick) {
            refreshWaterState(water, state);
            state.nextScanTick = (int) clientTickCounter + WATER_SCAN_INTERVAL_TICKS;
        }
        if (state.waterBlockCount == 0) {
            // No water in the box: the effect does not exist here (spec) and no spawn is attempted.
            debugWater(ownerId, state, clouds, specks);
            return;
        }

        if (clouds < WATER_CLOUD_MAX && clientTickCounter >= state.nextCloudTick) {
            if (spawnWaterCloud(random, water, state)) {
                state.nextCloudTick = (int) clientTickCounter + WATER_CLOUD_SPAWN_INTERVAL;
                clouds++;
            }
        }
        if (specks < WATER_SPEC_MAX && !reachedCap()
                && Math.floorMod(clientTickCounter + ownerId, WATER_SPEC_SPAWN_INTERVAL) == 0) {
            int wanted = WATER_SPEC_SPAWN_MIN
                    + random.nextInt(WATER_SPEC_SPAWN_MAX - WATER_SPEC_SPAWN_MIN + 1);
            for (int i = 0; i < wanted && specks < WATER_SPEC_MAX; i++) {
                WaterSpec speck = WaterSpec.spawn(state.surfacePositions, state.suspendedPositions, random);
                if (speck == null) {
                    break;
                }
                state.specks.add(speck);
                specks++;
            }
        }

        debugWater(ownerId, state, clouds, specks);
    }

    /** One throttled {@code [gascloud] water:} line per second per entity (diagnostics only). */
    private static void debugWater(int ownerId, WaterState state, int clouds, int specks) {
        if (!DEBUG || !debugWindowOpen(DEBUG_WATER_LOG, ownerId, DEBUG_LOG_INTERVAL_MS)) {
            return;
        }
        epca.LOGGER.info("[gascloud] water: owner={} waterBlocks={} surface={} suspended={} "
                        + "clouds={} specks={}",
                ownerId, state.waterBlockCount, state.surfacePositions.size(),
                state.suspendedPositions.size(), clouds, specks);
    }

    /**
     * Re-runs the water block scan of one entity's bounding box and rebuilds the cached surface and
     * suspended position lists.
     *
     * <p>A position counts as water when its block's fluid state is in {@code FluidTags.WATER}, so
     * both source and flowing water (and waterlogged blocks) qualify. Nothing is cached when the box
     * holds no water, which is what makes "the effect only exists where the entity touches water"
     * true. Absurdly large boxes are skipped outright instead of scanned.</p>
     */
    private static void refreshWaterState(ContaminatedWater water, WaterState state) {
        state.surfacePositions.clear();
        state.suspendedPositions.clear();
        state.waterBlockCount = 0;

        AABB box = water.getBoundingBox();
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume <= 0L || volume > WATER_SCAN_MAX_VOLUME) {
            return;
        }

        ClientLevel level = (ClientLevel) water.level();
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            FluidState fluid = level.getBlockState(pos).getFluidState();
            if (!fluid.is(FluidTags.WATER)) {
                continue;
            }
            state.waterBlockCount++;
            if (state.waterBlockCount > WATER_SCAN_MAX_POSITIONS) {
                // Never let one entity's cache grow past the cap; the first positions are enough for
                // a random spawn point.
                break;
            }
            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();
            // The block above decides the kind: water above means "suspended in the water", no water
            // above means this is a top face and therefore the surface. A surface position is lifted
            // WATER_SPEC_SURFACE_LIFT above the fluid top so the specks locked to it read as floating
            // on the surface instead of z-fighting with the water plane (same convention as the twin).
            FluidState above = level.getBlockState(pos.above()).getFluidState();
            if (above.is(FluidTags.WATER)) {
                state.suspendedPositions.add(new Vec3(x, y, z));
            } else {
                state.surfacePositions.add(new Vec3(x,
                        y + fluid.getOwnHeight() + WaterSpec.WATER_SPEC_SURFACE_LIFT, z));
            }
        }
    }

    /** Spawns one small red water cloud at a random water position of the entity's box. */
    private static boolean spawnWaterCloud(RandomSource random, ContaminatedWater water, WaterState state) {
        Vec3 point = randomWaterPoint(random, state);
        if (point == null) {
            return false;
        }
        Vec3 drift = new Vec3(
                (random.nextDouble() - 0.5D) * 2.0D * WATER_CLOUD_DRIFT_XZ,
                // Upward only, like the passive longarms puff: the spec asks for a random float, not
                // a signed one, on the vertical axis.
                random.nextDouble() * WATER_CLOUD_DRIFT_Y,
                (random.nextDouble() - 0.5D) * 2.0D * WATER_CLOUD_DRIFT_XZ);
        GasCloud cloud = new GasCloud(water.getId(), point, WATER_CLOUD_LIFETIME,
                WATER_CLOUD_BASE_SCALE, WATER_CLOUD_TARGET_SCALE, WATER_CLOUD_GROW_FRACTION,
                WATER_CLOUD_ALPHA, drift, false, false, random);
        return spawn(cloud);
    }

    /**
     * A random spawn point inside one of the entity's water blocks.
     *
     * <p>The surface list is used as-is (its Y is already the water top face); a suspended position
     * gets a small downward jitter so the clouds spawned in mid-water are not all at the very top of
     * their block. When only one of the two lists has entries, that one is used, so an entity that
     * touches water only with its top or only below the surface still produces clouds.</p>
     */
    private static Vec3 randomWaterPoint(RandomSource random, WaterState state) {
        boolean hasSurface = !state.surfacePositions.isEmpty();
        boolean hasSuspended = !state.suspendedPositions.isEmpty();
        if (!hasSurface && !hasSuspended) {
            return null;
        }
        boolean surface = hasSurface && (!hasSuspended || random.nextBoolean());
        List<Vec3> positions = surface ? state.surfacePositions : state.suspendedPositions;
        Vec3 base = positions.get(random.nextInt(positions.size()));
        return jitter(random, base, surface ? 0.0D : WATER_SPAWN_Y_JITTER);
    }

    /** Random x/z jitter inside the block plus an optional downward y jitter. */
    private static Vec3 jitter(RandomSource random, Vec3 base, double yJitter) {
        double dx = (random.nextDouble() - 0.5D) * 2.0D * WATER_SPAWN_XZ_JITTER;
        double dz = (random.nextDouble() - 0.5D) * 2.0D * WATER_SPAWN_XZ_JITTER;
        double dy = yJitter > 0.0D ? -random.nextDouble() * yJitter : 0.0D;
        return new Vec3(base.x + 0.5D + dx, base.y + dy, base.z + 0.5D + dz);
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
        // Mirrors the passive emit scatter: +/- 3 blocks in x/z at the mob's feet, 0..+5 blocks up.
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

    private static Vec3 randomPointInBox(RandomSource random, Vec3 center, double radius) {
        return new Vec3(
                center.x + (random.nextDouble() - 0.5D) * 2.0D * radius,
                center.y + (random.nextDouble() - 0.5D) * 2.0D * radius,
                center.z + (random.nextDouble() - 0.5D) * 2.0D * radius);
    }

    private static boolean spawn(GasCloud cloud) {
        if (reachedCap()) {
            return false;
        }
        CLOUDS.add(cloud);
        BY_OWNER.computeIfAbsent(cloud.getOwnerEntityId(), key -> new ArrayList<>()).add(cloud);
        return true;
    }

    private static boolean reachedCap() {
        return CLOUDS.size() + countWaterSpecks() >= MAX_CLOUDS;
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

    private static int countOwned(int ownerEntityId) {
        List<GasCloud> owned = BY_OWNER.get(ownerEntityId);
        return owned == null ? 0 : owned.size();
    }

    /** Total number of tracked water specks, so the global cap covers them too. */
    private static int countWaterSpecks() {
        int total = 0;
        for (WaterState state : WATER_STATES.values()) {
            total += state.specks.size();
        }
        return total;
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
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ReshapeLongarms.CustomPart part && part.isBackPart()) {
                ownersWithBackPart.add(part.getOwnerEntityId());
            }
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ReshapeLongarms longarms
                    && !ownersWithBackPart.contains(longarms.getId())) {
                PASSIVE_ACTIVE_OWNERS.add(longarms.getId());
            }
        }
    }

    // =================================================================
    //  Diagnostics helpers
    // =================================================================

    /**
     * Opens (at most once per second per owner) the throttling window of one diagnostic line.
     *
     * <p>Shared by every diagnostic in the feature so the throttling cannot drift apart between the
     * mob layer ({@code [gascloud] layer:}) and the water path ({@code [gascloud] water:}). The map
     * is cleared rather than grown when it passes {@link #DEBUG_THROTTLE_MAX_ENTRIES}, so an old
     * entity id can never keep the line silent.</p>
     *
     * @return true when the caller may log now
     */
    public static boolean debugWindowOpen(Map<Integer, Long> log, int ownerId, long intervalMs) {
        long now = System.currentTimeMillis();
        Long last = log.get(ownerId);
        if (last != null && now - last < intervalMs) {
            return false;
        }
        if (log.size() > DEBUG_THROTTLE_MAX_ENTRIES) {
            log.clear();
        }
        log.put(ownerId, now);
        return true;
    }

    // =================================================================
    //  Queries used by the render layer
    // =================================================================

    /** Every live cloud owned by the given entity id; empty when there is none. */
    public static List<GasCloud> getCloudsFor(int ownerEntityId) {
        List<GasCloud> owned = BY_OWNER.get(ownerEntityId);
        return owned == null ? Collections.emptyList() : owned;
    }

    /** Every live water speck owned by the given entity id; empty when there is none. */
    public static List<WaterSpec> getWaterSpecksFor(int ownerEntityId) {
        WaterState state = WATER_STATES.get(ownerEntityId);
        return state == null ? Collections.emptyList() : state.specks;
    }

    public static boolean hasCloudsFor(int ownerEntityId) {
        List<GasCloud> owned = BY_OWNER.get(ownerEntityId);
        return owned != null && !owned.isEmpty();
    }

    public static int getTotalCloudCount() {
        return CLOUDS.size();
    }

    /** Clears every tracked cloud and speck and all edge state; used when the client level changes. */
    public static void clearAll() {
        reset();
    }

    private static void reset() {
        CLOUDS.clear();
        BY_OWNER.clear();
        ACTIVE_SPAWNED.clear();
        YELLOWEYE_LAST_ELAPSED.clear();
        YELLOWEYE_SPAWNED.clear();
        OWNER_LAST_SEEN.clear();
        PASSIVE_ACTIVE_OWNERS.clear();
        DEBUG_PASSIVE_REPORTED.clear();
        WATER_STATES.clear();
        waterOwners.clear();
        WATER_LAST_SEEN.clear();
        DEBUG_WATER_LOG.clear();
        levelIdentity = null;
        clientTickCounter = 0;
    }
}
