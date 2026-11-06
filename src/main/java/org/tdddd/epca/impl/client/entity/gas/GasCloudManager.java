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

public final class GasCloudManager {

    public static final boolean DEBUG = false;

    private static final long DEBUG_REPEAT_MS = 1000L;

    private static final Map<Integer, long[]> DEBUG_OWNER_STATE = new HashMap<>();

    private static int debugTotalSpawned;

    private static int debugLayerDrawCalls;
    private static int debugLayerCloudsDrawn;

    private static Long DEBUG_PASSIVE_SIGNATURE;

    private static boolean debugTickLogged;

    private static final int DBG_STATE_NONE = 0;
    private static final int DBG_STATE_IDLE = 1;
    private static final int DBG_STATE_GASSING = 2;
    private static final int DBG_STATE_PASSIVE = 3;
    private static final int DBG_STATE_WATER = 4;

    public static final int ACTIVE_BIG_CLOUD_COUNT = 1;

    public static final int ACTIVE_SMALL_CLOUD_MIN = 2;
    public static final int ACTIVE_SMALL_CLOUD_MAX = 4;

    public static final float BIG_CLOUD_BASE_SCALE = 1.2F;
    public static final float BIG_CLOUD_TARGET_SCALE = 3.5F;
    public static final float BIG_CLOUD_GROW_FRACTION = 0.45F;

    public static final float ACTIVE_SMALL_BASE_SCALE = 0.8F;
    public static final float ACTIVE_SMALL_TARGET_SCALE = 1.6F;
    public static final float ACTIVE_SMALL_GROW_FRACTION = 0.65F;

    public static final float ACTIVE_CLOUD_ALPHA = 0.9F;

    public static final double ACTIVE_EMIT_RADIUS = 3.5D;

    public static final double ACTIVE_EMIT_HEIGHT = 2.0D;

    public static final int BIG_CLOUD_LIFETIME = 50;

    public static final int CLOUD_LIFETIME = 45;

    public static final float PASSIVE_BASE_SCALE = 0.5F;
    public static final float PASSIVE_TARGET_SCALE = 1.0F;
    public static final float PASSIVE_GROW_FRACTION = 0.6F;

    public static final float PASSIVE_CLOUD_ALPHA = 0.45F;

    public static final int PASSIVE_EMIT_INTERVAL = 20;

    public static final double PASSIVE_EMIT_RADIUS_XZ = 3.0D;

    public static final double PASSIVE_EMIT_HEIGHT_RANGE = 5.0D;

    public static final double PASSIVE_DRIFT_XZ = 0.25D;

    public static final double PASSIVE_DRIFT_Y = 0.18D;

    private static final int PASSIVE_MIN_ENTITY_AGE = 40;

    public static final float YELLOWEYE_BASE_SCALE = 0.7F;
    public static final float YELLOWEYE_TARGET_SCALE = 1.4F;
    public static final float YELLOWEYE_GROW_FRACTION = 0.6F;

    public static final float YELLOWEYE_CLOUD_ALPHA = 0.9F;

    public static final int YELLOWEYE_MAX_CLOUDS = 6;

    public static final int YELLOWEYE_GASSING_DURATION = 30;

    private static final int YELLOWEYE_PARTICLE_START_ELAPSED = 10;

    private static final int YELLOWEYE_PARTICLE_PERIOD = 2;

    public static final double YELLOWEYE_PARTICLE_OFFSET_X = 0.6D;
    public static final double YELLOWEYE_PARTICLE_OFFSET_Y = -0.3D;
    public static final double YELLOWEYE_PARTICLE_OFFSET_Z = 0.4D;

    public static final double YELLOWEYE_LINE_MAX_T = 0.8D;

    public static final double YELLOWEYE_RANGE = YELLOWEYE_PARTICLE_OFFSET_X * (1.0D + YELLOWEYE_LINE_MAX_T);

    public static final double YELLOWEYE_VERTICAL_RANGE =
            Math.abs(YELLOWEYE_PARTICLE_OFFSET_Y) * (1.0D + YELLOWEYE_LINE_MAX_T);

    public static final double YELLOWEYE_FORWARD_OFFSET = YELLOWEYE_PARTICLE_OFFSET_Z;

    public static final int WATER_CLOUD_MIN = 3;
    public static final int WATER_CLOUD_MAX = 5;

    public static final int WATER_CLOUD_SPAWN_INTERVAL = 8;

    public static final int WATER_CLOUD_LIFETIME = 45;

    public static final float WATER_CLOUD_BASE_SCALE = 0.5F;
    public static final float WATER_CLOUD_TARGET_SCALE = 0.9F;
    public static final float WATER_CLOUD_GROW_FRACTION = 0.5F;

    public static final float WATER_CLOUD_ALPHA = 0.55F;

    public static final double WATER_CLOUD_DRIFT_XZ = 0.12D;

    public static final double WATER_CLOUD_DRIFT_Y = 0.05D;

    public static final int WATER_SPEC_MIN = 10;
    public static final int WATER_SPEC_MAX = 24;
    public static final int WATER_SPEC_SPAWN_INTERVAL = 4;
    public static final int WATER_SPEC_SPAWN_MIN = 1;
    public static final int WATER_SPEC_SPAWN_MAX = 3;

    public static final int WATER_SPEC_LIFETIME_MIN = 30;
    public static final int WATER_SPEC_LIFETIME_MAX = 80;

    public static final int WATER_BLOCK_REFRESH_INTERVAL = 20;

    public static final int WATER_SCAN_MAX_BLOCKS = 4096;

    public static final double WATER_POS_INSET = 0.05D;

    public static final int MAX_CLOUDS = 512;

    private static final double MAX_OWNER_DISTANCE = 96.0D;
    private static final double MAX_OWNER_DISTANCE_SQR = MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE;

    private static final List<GasCloud> CLOUDS = new ArrayList<>();
    private static final Map<Integer, List<GasCloud>> BY_OWNER = new HashMap<>();

    private static final List<WaterSpec> SPECS = new ArrayList<>();
    private static final Map<Integer, List<WaterSpec>> SPECS_BY_OWNER = new HashMap<>();

    private static final Map<Integer, WaterScan> WATER_SCANS = new HashMap<>();

    private static final Set<UUID> ACTIVE_SPAWNED = new HashSet<>();

    private static final Map<UUID, Integer> YELLOWEYE_LAST_ELAPSED = new HashMap<>();

    private static final Map<UUID, Integer> YELLOWEYE_SPAWNED = new HashMap<>();

    private static final Set<Integer> PASSIVE_ACTIVE_OWNERS = new HashSet<>();

    private static final Set<Integer> DEBUG_PASSIVE_REPORTED = new HashSet<>();

    private static ClientLevel levelIdentity;

    private GasCloudManager() {
    }

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

    public static String debugSummary() {
        return "total=" + CLOUDS.size()
                + " owners=" + BY_OWNER.size()
                + " specs=" + SPECS.size()
                + " spawned=" + debugTotalSpawned
                + " passiveOwners=" + PASSIVE_ACTIVE_OWNERS.size()
                + " layerDraws=" + debugLayerDrawCalls
                + " layerClouds=" + debugLayerCloudsDrawn;
    }

    public static void debugLayerDrew(int cloudCount) {
        debugLayerDrawCalls++;
        debugLayerCloudsDrawn += cloudCount;
    }

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

        WATER_SCANS.keySet().removeIf(ownerId -> !liveOwnerIds.contains(ownerId));
    }

    private static void tickLongarms(ReshapeLongarms longarms, Vec3 cameraPos) {
        UUID id = longarms.getUUID();
        if (longarms.distanceToSqr(cameraPos) > MAX_OWNER_DISTANCE_SQR) {

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

            YELLOWEYE_SPAWNED.put(id, 0);
        }
        YELLOWEYE_LAST_ELAPSED.put(id, elapsed);

        int spawned = YELLOWEYE_SPAWNED.getOrDefault(id, 0);

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

        Iterator<WaterSpec> specIterator = SPECS.iterator();
        while (specIterator.hasNext()) {
            WaterSpec spec = specIterator.next();
            if (!spec.tick()) {
                specIterator.remove();
                removeSpecFromOwnerIndex(spec);
            }
        }
    }

    private static void spawnLongarmsActiveClouds(RandomSource random, ReshapeLongarms longarms) {
        int ownerId = longarms.getId();
        Vec3 emitPoint = new Vec3(longarms.getX(), longarms.getY() + ACTIVE_EMIT_HEIGHT, longarms.getZ());

        spawn(new GasCloud(ownerId, emitPoint, BIG_CLOUD_LIFETIME,
                BIG_CLOUD_BASE_SCALE, BIG_CLOUD_TARGET_SCALE, BIG_CLOUD_GROW_FRACTION,
                ACTIVE_CLOUD_ALPHA, Vec3.ZERO, true, false, random));
        int spawned = 1;

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

    private static final class WaterScan {
        private final List<BlockPos> water = new ArrayList<>();
        private final List<BlockPos> surface = new ArrayList<>();
        private final List<BlockPos> suspended = new ArrayList<>();
        private int refreshTicks;
        private int cloudCooldown;
        private int specCooldown;

        private int loggedWaterBlocks = -1;

        private boolean loggedHugeBox;
    }

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

        for (BlockPos pos : BlockPos.betweenClosed(box)) {

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

        RandomSource specRandom = RandomSource.create(random.nextLong());
        spawnSpec(new WaterSpec(water.getId(),
                surfaceKind ? WaterSpec.Kind.SURFACE : WaterSpec.Kind.SUSPENDED,
                new Vec3(x, y, z), drift, lifetime, halfExtent, alpha,
                random.nextFloat() * (float) (Math.PI * 2.0D), specRandom));
    }

    private static float levelFluidHeight(ContaminatedWater water, BlockPos pos) {
        return water.level().getBlockState(pos).getFluidState().getHeight(water.level(), pos);
    }

    private static float fluidOwnHeight(ContaminatedWater water, BlockPos pos) {
        return water.level().getBlockState(pos).getFluidState().getOwnHeight();
    }

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

    private static void debugFirstSpawn(String kind, int ownerId, Vec3 position, int count,
                                        float baseScale, float targetScale, float alpha) {
        if (!DEBUG) {
            return;
        }
        epca.LOGGER.info("[gascloud] manager: {} owner={} pos=({}, {}, {}) spawned={} scale={}->{} alpha={}",
                kind, ownerId, position.x, position.y, position.z,
                count, baseScale, targetScale, alpha);
    }

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

    public static List<WaterSpec> getSpecsFor(int ownerEntityId) {
        List<WaterSpec> owned = SPECS_BY_OWNER.get(ownerEntityId);
        return owned == null ? Collections.emptyList() : owned;
    }

    public static boolean hasSpecsFor(int ownerEntityId) {
        List<WaterSpec> owned = SPECS_BY_OWNER.get(ownerEntityId);
        return owned != null && !owned.isEmpty();
    }

    public static int getWaterBlockCount(int ownerEntityId) {
        WaterScan scan = WATER_SCANS.get(ownerEntityId);
        return scan == null ? 0 : scan.water.size();
    }

    public static int getTotalSpecCount() {
        return SPECS.size();
    }

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
