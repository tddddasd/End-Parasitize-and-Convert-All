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

public final class GasCloudManager {

    public static final boolean DEBUG = false;

    private static final long DEBUG_LOG_INTERVAL_MS = 1000L;

    private static final int DEBUG_THROTTLE_MAX_ENTRIES = 256;

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

    public static final float WATER_CLOUD_BASE_SCALE = 0.5F;
    public static final float WATER_CLOUD_TARGET_SCALE = 0.9F;

    public static final float WATER_CLOUD_GROW_FRACTION = 0.5F;

    public static final float WATER_CLOUD_ALPHA = 0.55F;

    public static final int WATER_CLOUD_LIFETIME = CLOUD_LIFETIME;

    public static final double WATER_CLOUD_DRIFT_XZ = 0.12D;
    public static final double WATER_CLOUD_DRIFT_Y = 0.05D;

    public static final int WATER_SPEC_MIN = 10;
    public static final int WATER_SPEC_MAX = 24;

    public static final int WATER_SPEC_SPAWN_INTERVAL = 4;
    public static final int WATER_SPEC_SPAWN_MIN = 1;
    public static final int WATER_SPEC_SPAWN_MAX = 3;

    public static final int WATER_SCAN_INTERVAL_TICKS = 20;

    public static final int WATER_SCAN_MAX_VOLUME = 4096;

    public static final int WATER_SCAN_MAX_POSITIONS = 512;

    public static final int WATER_MAX_OWNERS_PER_TICK = 32;

    private static final int WATER_SCAN_STAGGER_TICKS = 4;

    private static final double WATER_SPAWN_XZ_JITTER = 0.4D;

    private static final double WATER_SPAWN_Y_JITTER = 0.25D;

    private static final double WATER_SUSPENDED_Y_JITTER = 0.3D;

    public static final int MAX_CLOUDS = 512;

    private static final double MAX_OWNER_DISTANCE = 96.0D;
    private static final double MAX_OWNER_DISTANCE_SQR = MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE;

    private static final int OWNER_STATE_TTL_TICKS = 200;

    private static final List<GasCloud> CLOUDS = new ArrayList<>();
    private static final Map<Integer, List<GasCloud>> BY_OWNER = new HashMap<>();

    private static final Set<UUID> ACTIVE_SPAWNED = new HashSet<>();

    private static final Map<UUID, Integer> YELLOWEYE_LAST_ELAPSED = new HashMap<>();

    private static final Map<UUID, Integer> YELLOWEYE_SPAWNED = new HashMap<>();

    private static final Map<UUID, Long> OWNER_LAST_SEEN = new HashMap<>();

    private static final Set<Integer> PASSIVE_ACTIVE_OWNERS = new HashSet<>();

    private static final Set<Integer> DEBUG_PASSIVE_REPORTED = new HashSet<>();

    private static final Map<Integer, WaterState> WATER_STATES = new HashMap<>();

    private static final List<ContaminatedWater> waterOwners = new ArrayList<>();

    private static final Map<Integer, Long> WATER_LAST_SEEN = new HashMap<>();

    private static final Map<Integer, Long> DEBUG_WATER_LOG = new HashMap<>();

    private static ClientLevel levelIdentity;
    private static long clientTickCounter;

    private GasCloudManager() {
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

        clientTickCounter++;

        Set<Integer> presentIds = collectPresentEntityIds(level);
        pruneOrphanedClouds(presentIds);
        releaseOrphanedWaterVfx(presentIds);
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

    private static Set<Integer> collectPresentEntityIds(ClientLevel level) {
        Set<Integer> presentIds = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            presentIds.add(entity.getId());
        }
        return presentIds;
    }

    private static void pruneOrphanedClouds(Set<Integer> presentIds) {
        if (CLOUDS.isEmpty() && BY_OWNER.isEmpty()) {
            return;
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

    private static void releaseOrphanedWaterVfx(Set<Integer> presentIds) {
        if (WATER_STATES.isEmpty() && WATER_LAST_SEEN.isEmpty() && DEBUG_WATER_LOG.isEmpty()) {
            return;
        }
        WATER_STATES.keySet().removeIf(ownerId -> !presentIds.contains(ownerId));
        WATER_LAST_SEEN.keySet().removeIf(ownerId -> !presentIds.contains(ownerId));
        DEBUG_WATER_LOG.keySet().removeIf(ownerId -> !presentIds.contains(ownerId));
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

            ACTIVE_SPAWNED.remove(id);
            return;
        }

        RandomSource random = longarms.getRandom();
        boolean gassing = longarms.isGassing();

        if (!gassing) {
            ACTIVE_SPAWNED.remove(id);
        } else if (ACTIVE_SPAWNED.add(id)) {
            spawnLongarmsActiveClouds(random, longarms);
        }

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

            YELLOWEYE_SPAWNED.put(id, 0);
        }
        YELLOWEYE_LAST_ELAPSED.put(id, elapsed);

        int spawned = YELLOWEYE_SPAWNED.getOrDefault(id, 0);

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

        RandomSource random = Minecraft.getInstance().level.getRandom();
        for (WaterState state : WATER_STATES.values()) {
            state.ageSpecks(random);
        }
    }

    private static final class WaterState {

        private final List<Vec3> surfacePositions = new ArrayList<>();

        private final List<Vec3> suspendedPositions = new ArrayList<>();

        private final List<WaterSpec> specks = new ArrayList<>();

        private int waterBlockCount;

        private int nextScanTick;

        private int nextCloudTick;

        private void ageSpecks(RandomSource random) {
            this.specks.removeIf(speck -> !speck.tick(random));
        }
    }

    private static void refreshWaterOwners(ClientLevel level, Vec3 cameraPos) {
        waterOwners.clear();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ContaminatedWater water
                    && water.distanceToSqr(cameraPos) <= MAX_OWNER_DISTANCE_SQR) {
                waterOwners.add(water);
            }
        }
    }

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

    private static void tickWaterEntity(ContaminatedWater water) {
        int ownerId = water.getId();

        RandomSource random = water.level().getRandom();
        WaterState state = WATER_STATES.get(ownerId);
        if (state == null) {

            state = new WaterState();
            int stagger = WATER_SCAN_STAGGER_TICKS + Math.floorMod(ownerId, WATER_SCAN_INTERVAL_TICKS);
            state.nextScanTick = (int) clientTickCounter + stagger;
            state.nextCloudTick = state.nextScanTick;
            WATER_STATES.put(ownerId, state);
        }

        int clouds = countOwned(ownerId);
        int specks = state.specks.size();

        if (clientTickCounter >= state.nextScanTick) {
            refreshWaterState(water, state);
            state.nextScanTick = (int) clientTickCounter + WATER_SCAN_INTERVAL_TICKS;
        }
        if (state.waterBlockCount == 0) {

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

    private static void debugWater(int ownerId, WaterState state, int clouds, int specks) {
        if (!DEBUG || !debugWindowOpen(DEBUG_WATER_LOG, ownerId, DEBUG_LOG_INTERVAL_MS)) {
            return;
        }
        epca.LOGGER.info("[gascloud] water: owner={} waterBlocks={} surface={} suspended={} "
                        + "clouds={} specks={}",
                ownerId, state.waterBlockCount, state.surfacePositions.size(),
                state.suspendedPositions.size(), clouds, specks);
    }

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

                break;
            }
            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();

            FluidState above = level.getBlockState(pos.above()).getFluidState();
            if (above.is(FluidTags.WATER)) {
                state.suspendedPositions.add(new Vec3(x, y, z));
            } else {
                state.surfacePositions.add(new Vec3(x,
                        y + fluid.getOwnHeight() + WaterSpec.WATER_SPEC_SURFACE_LIFT, z));
            }
        }
    }

    private static boolean spawnWaterCloud(RandomSource random, ContaminatedWater water, WaterState state) {
        Vec3 point = randomWaterPoint(random, state);
        if (point == null) {
            return false;
        }
        Vec3 drift = new Vec3(
                (random.nextDouble() - 0.5D) * 2.0D * WATER_CLOUD_DRIFT_XZ,

                random.nextDouble() * WATER_CLOUD_DRIFT_Y,
                (random.nextDouble() - 0.5D) * 2.0D * WATER_CLOUD_DRIFT_XZ);
        GasCloud cloud = new GasCloud(water.getId(), point, WATER_CLOUD_LIFETIME,
                WATER_CLOUD_BASE_SCALE, WATER_CLOUD_TARGET_SCALE, WATER_CLOUD_GROW_FRACTION,
                WATER_CLOUD_ALPHA, drift, false, false, random);
        return spawn(cloud);
    }

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

    private static Vec3 jitter(RandomSource random, Vec3 base, double yJitter) {
        double dx = (random.nextDouble() - 0.5D) * 2.0D * WATER_SPAWN_XZ_JITTER;
        double dz = (random.nextDouble() - 0.5D) * 2.0D * WATER_SPAWN_XZ_JITTER;
        double dy = yJitter > 0.0D ? -random.nextDouble() * yJitter : 0.0D;
        return new Vec3(base.x + 0.5D + dx, base.y + dy, base.z + 0.5D + dz);
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

    private static int countWaterSpecks() {
        int total = 0;
        for (WaterState state : WATER_STATES.values()) {
            total += state.specks.size();
        }
        return total;
    }

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

    public static List<GasCloud> getCloudsFor(int ownerEntityId) {
        List<GasCloud> owned = BY_OWNER.get(ownerEntityId);
        return owned == null ? Collections.emptyList() : owned;
    }

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
