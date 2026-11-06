package org.tdddd.epca.impl.compat.vs;

import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class InfestedPumpkinShip {

    private static final int MAX_CARRIED_BLOCKS = 3;
    private static final double CHASE_RANGE = 16.0D;

    private static final double ROLL_SPEED = 0.12D;

    private static final double ROLL_SPIN = 0.35D;

    private static final double CONTACT_HALF = 1.0D;
    private static final float CONTACT_DAMAGE = 4.0F;

    private static final int LIFETIME_TICKS = 30 * 20;

    private static final Map<Long, Set<BlockPos>> BLOCKS = new HashMap<>();
    private static final Map<Long, Integer> AGE = new HashMap<>();
    private static final Map<Long, ServerLevel> LEVELS = new HashMap<>();
    private static final Map<Long, Object> SHIPS = new HashMap<>();

    private static final Set<BlockPos> POSITIONS = new HashSet<>();

    private static final Map<BlockPos, ServerLevel> PENDING = new java.util.LinkedHashMap<>();

    private static final Map<BlockPos, Long> SUPPRESSED = new java.util.HashMap<>();

    private static final Map<BlockPos, net.minecraft.world.level.block.state.BlockState> PENDING_STATE =
            new java.util.LinkedHashMap<>();

    private InfestedPumpkinShip() {
    }

    public static boolean isAvailable() {
        return net.minecraftforge.fml.ModList.get().isLoaded("valkyrienskies");
    }

    public static void spawn(ServerLevel level, BlockPos pos,
                            net.minecraft.world.level.block.state.BlockState state) {

        if (shipNear(level, pos)) {
            return;
        }

        Long until = SUPPRESSED.get(pos.immutable());
        if (until != null) {
            if (level.getGameTime() <= until) {
                SUPPRESSED.remove(pos.immutable());
                return;
            }
            SUPPRESSED.remove(pos.immutable());
        }
        if (!isAvailable()) {
            return;
        }

        PENDING.putIfAbsent(pos.immutable(), level);
        PENDING_STATE.put(pos.immutable(), state);
        if (state == null) {
            return;
        }
        if (true) {
            return;
        }
    }

    private static boolean shipNear(ServerLevel level, BlockPos pos) {
        for (var entry : SHIPS.entrySet()) {
            if (LEVELS.get(entry.getKey()) != level) {
                continue;
            }
            BlockPos at = currentPos(entry.getValue(), null);
            if (at != null && at.closerThan(pos, 3.0D)) {
                return true;
            }
        }
        return false;
    }

    private static void assemble(ServerLevel level, BlockPos pos,
                                 net.minecraft.world.level.block.state.BlockState snapshot) {
        BlockPos immutable = pos.immutable();
        if (POSITIONS.contains(immutable)) {
            return;
        }
        var log = org.apache.logging.log4j.LogManager.getLogger("epca");
        try {

            if (level.getBlockState(immutable).isAir() && snapshot != null) {
                level.setBlock(immutable, snapshot, 3);
            }
            var state = level.getBlockState(immutable);
            boolean validShipBlock = org.valkyrienskies.mod.common.assembly.ShipAssembler.INSTANCE
                    .isValidShipBlock(state);
            if (!validShipBlock) {

                return;
            }
            Set<BlockPos> blocks = new HashSet<>();
            blocks.add(immutable);
            var ship = org.valkyrienskies.mod.common.assembly.ShipAssembler
                    .assembleToShip(level, blocks, 1.0D);
            log.info("[pumpkin] assemble result {}", ship);
            if (ship != null) {
                POSITIONS.add(immutable);
                level.removeBlock(immutable, false);
                BLOCKS.put(ship.getId(), new HashSet<>(Set.of(immutable)));
                AGE.put(ship.getId(), 0);
                LEVELS.put(ship.getId(), level);
                SHIPS.put(ship.getId(), ship);
            }
        } catch (Throwable failure) {
            log.error("[pumpkin] assemble failed at {}", immutable, failure);
        }
    }

    public static void tick(ServerLevel level) {
        if (!isAvailable()) {
            PENDING.clear();
            return;
        }

        if (!PENDING.isEmpty()) {
            var ready = new java.util.ArrayList<BlockPos>();
            for (var e : PENDING.entrySet()) {
                if (e.getValue() == level) {
                    ready.add(e.getKey());
                }
            }
            for (BlockPos queued : ready) {
                PENDING.remove(queued);
                assemble(level, queued, PENDING_STATE.remove(queued));
            }
        }
        if (AGE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Long, Integer>> it = AGE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Integer> entry = it.next();
            long id = entry.getKey();
            ServerLevel owner = LEVELS.get(id);
            if (owner != level) {
                continue;
            }
            int age = entry.getValue() + 1;
            entry.setValue(age);
            Set<BlockPos> blocks = BLOCKS.getOrDefault(id, Set.of());

            boolean inert = blocks.size() > MAX_CARRIED_BLOCKS;
            if (age > LIFETIME_TICKS || inert) {
                if (age > LIFETIME_TICKS) {
                    despawn(level, id, it);
                }
                continue;
            }
            applyTick(level, id, blocks);
        }
    }

    private static void applyTick(ServerLevel level, long shipId, Set<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        BlockPos anchor = blocks.iterator().next();
        var center = new net.minecraft.world.phys.Vec3(anchor.getX() + 0.5D, anchor.getY() + 0.5D, anchor.getZ() + 0.5D);
        LivingEntity target = findTarget(level, center);
        if (target == null) {
            stop(shipId);
            return;
        }

        if (Math.abs(target.getX() - center.x) <= CONTACT_HALF
                && Math.abs(target.getY() - center.y) <= CONTACT_HALF
                && Math.abs(target.getZ() - center.z) <= CONTACT_HALF) {
            hurt(level, target);
        }
    }

    private static LivingEntity findTarget(ServerLevel level, net.minecraft.world.phys.Vec3 center) {
        var search = new net.minecraft.world.phys.AABB(
                center.x - CHASE_RANGE, center.y - CHASE_RANGE, center.z - CHASE_RANGE,
                center.x + CHASE_RANGE, center.y + CHASE_RANGE, center.z + CHASE_RANGE);
        LivingEntity best = null;
        double bestDistance = CHASE_RANGE * CHASE_RANGE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, search)) {
            if (!entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            if (ParasiteEvents.isParasiteByTagOrInterface(entity)) {
                continue;
            }
            double distance = entity.position().distanceToSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private static void roll(long shipId, double nx, double nz) {
        Object held = SHIPS.get(shipId);
        if (!(held instanceof org.valkyrienskies.core.api.ships.Ship ship)) {
            return;
        }
        try {
            var kinematics = ship.getKinematics().toBuilder()
                    .velocity(new org.joml.Vector3d(nx * ROLL_SPEED, ship.getVelocity().y(), nz * ROLL_SPEED))

                    .angularVelocity(new org.joml.Vector3d(nz * ROLL_SPIN, 0.0D, -nx * ROLL_SPIN))
                    .build();
            ((org.valkyrienskies.core.internal.ships.VsiShip) ship).unsafeSetKinematics(kinematics);
        } catch (Throwable failure) {
            org.apache.logging.log4j.LogManager.getLogger("epca").error(
                    "[pumpkin] roll failed", failure);
        }
    }

    private static void stop(long shipId) {
        Object held = SHIPS.get(shipId);
        if (!(held instanceof org.valkyrienskies.core.api.ships.Ship ship)) {
            return;
        }
        try {
            var kinematics = ship.getKinematics().toBuilder()
                    .velocity(new org.joml.Vector3d(0.0D, ship.getVelocity().y(), 0.0D))
                    .angularVelocity(new org.joml.Vector3d(0.0D, 0.0D, 0.0D))
                    .build();
            ((org.valkyrienskies.core.internal.ships.VsiShip) ship).unsafeSetKinematics(kinematics);
        } catch (Throwable ignored) {

        }
    }

    private static void hurt(ServerLevel level, LivingEntity target) {
        target.hurt(level.damageSources().magic(), CONTACT_DAMAGE);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.PARA_CURSE.get(), 30 * 20, 0, false, false, true));
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.FEAR.get(), 15 * 20, 0, false, false, true));
    }

    private static BlockPos currentPos(Object ship, BlockPos fallback) {
        try {
            if (ship instanceof org.valkyrienskies.core.api.ships.Ship s) {
                var position = s.getTransform().getPosition();
                return BlockPos.containing(position.x(), position.y(), position.z());
            }
        } catch (Throwable ignored) {

        }
        return fallback;
    }

    private static void despawn(ServerLevel level, long shipId, Iterator<Map.Entry<Long, Integer>> it) {
        Object ship = SHIPS.get(shipId);
        try {
            if (ship instanceof org.valkyrienskies.core.api.ships.ServerShip serverShip) {
                org.valkyrienskies.mod.common.assembly.ShipAssembler.INSTANCE
                        .deleteShip(level, serverShip, true, true);
            }
        } catch (Throwable ignored) {

        }
        SHIPS.remove(shipId);
        it.remove();
        BLOCKS.remove(shipId);
        LEVELS.remove(shipId);
        POSITIONS.removeIf(p -> !POSITIONS.isEmpty() && false);
    }
}
