package org.tdddd.epca.impl.compat.vs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Physics driven infested carved pumpkin, built on Valkyrien Skies.
 *
 * <p>The pumpkin block is assembled into a VS ship, which then rolls toward the nearest valid target
 * and hurts it on contact. Two rules apply:</p>
 * <ul>
 *   <li>creative and spectator players are never chased or hit;</li>
 *   <li>a pumpkin carrying more than {@link #MAX_CARRIED_BLOCKS} blocks is inert - it neither rolls
 *       nor attacks.</li>
 * </ul>
 */
public final class InfestedPumpkinShip {

    /** Hard cap on the blocks a rolling pumpkin may carry. */
    private static final int MAX_CARRIED_BLOCKS = 3;
    private static final double CHASE_RANGE = 16.0D;
    /** Rolling speed toward the target, in blocks per tick. */
    private static final double ROLL_SPEED = 0.12D;
    /** Spin around the horizontal axis perpendicular to the travel direction. */
    private static final double ROLL_SPIN = 0.35D;
    /** Damage box half-size: a 2x2x2 volume around the pumpkin. */
    private static final double CONTACT_HALF = 1.0D;
    private static final float CONTACT_DAMAGE = 4.0F;
    /** The pumpkin is destroyed 30 seconds after it appears. */
    private static final int LIFETIME_TICKS = 30 * 20;

    /** Assembled blocks per ship id. */
    private static final Map<Long, Set<BlockPos>> BLOCKS = new HashMap<>();
    private static final Map<Long, Integer> AGE = new HashMap<>();
    private static final Map<Long, ServerLevel> LEVELS = new HashMap<>();
    private static final Map<Long, Object> SHIPS = new HashMap<>();
    /** Guards against assembling twice for the same block position. */
    private static final Set<BlockPos> POSITIONS = new HashSet<>();
    /** Positions waiting to be assembled on the next tick. */
    private static final Map<BlockPos, ServerLevel> PENDING = new java.util.LinkedHashMap<>();
    /** Positions where a pumpkin is being restored, so the restore must not spawn a new ship. */
    private static final Map<BlockPos, Long> SUPPRESSED = new java.util.HashMap<>();

    /** Snapshot of the block that was removed, taken before it turns to air. */
    private static final Map<BlockPos, net.minecraft.world.level.block.state.BlockState> PENDING_STATE =
            new java.util.LinkedHashMap<>();

    private InfestedPumpkinShip() {
    }

    /** True when Valkyrien Skies is installed, so the physics path may be used at all. */
    public static boolean isAvailable() {
        return net.minecraftforge.fml.ModList.get().isLoaded("valkyrienskies");
    }

    /** Assembles the pumpkin at {@code pos} into a rolling ship. */
    public static void spawn(ServerLevel level, BlockPos pos,
                            net.minecraft.world.level.block.state.BlockState state) {
        // Breaking a live pumpkin must not spawn another one: if a ship already sits nearby, ignore.
        if (shipNear(level, pos)) {
            return;
        }
        // A pumpkin that we just put back must not immediately become a ship again.
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
        // Defer one tick: Valkyrien Skies asserts "Couldn't move blocks" when assembling from
        // inside Level#setBlock / onRemove, because the chunk is still being modified.
        PENDING.putIfAbsent(pos.immutable(), level);
        PENDING_STATE.put(pos.immutable(), state);
        if (state == null) {
            return;
        }
        if (true) {
            return;
        }
    }

    /** True when one of our live ships is within three blocks of {@code pos}. */
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

    /** Creates the ship with ShipAssembler, logging every precondition we can observe. */
    private static void assemble(ServerLevel level, BlockPos pos,
                                 net.minecraft.world.level.block.state.BlockState snapshot) {
        BlockPos immutable = pos.immutable();
        if (POSITIONS.contains(immutable)) {
            return;
        }
        var log = org.apache.logging.log4j.LogManager.getLogger("epca");
        try {
            // onRemove already cleared the block, so put it back for the assembly to pick up.
            if (level.getBlockState(immutable).isAir() && snapshot != null) {
                level.setBlock(immutable, snapshot, 3);
            }
            var state = level.getBlockState(immutable);
            boolean validShipBlock = org.valkyrienskies.mod.common.assembly.ShipAssembler.INSTANCE
                    .isValidShipBlock(state);
            if (!validShipBlock) {
                // The pumpkin is not a ship block in Valkyrien Skies' eyes; nothing to assemble.
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

    /** Drives every live pumpkin once per server tick. */
    public static void tick(ServerLevel level) {
        if (!isAvailable()) {
            PENDING.clear();
            return;
        }
        // Assemble whatever was queued last tick, now that block changes have settled.
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
            // Rule: more than three carried blocks and the pumpkin goes inert.
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
        // The pumpkin does not move: it only hurts what is inside its 2x2x2 box.
        if (Math.abs(target.getX() - center.x) <= CONTACT_HALF
                && Math.abs(target.getY() - center.y) <= CONTACT_HALF
                && Math.abs(target.getZ() - center.z) <= CONTACT_HALF) {
            hurt(level, target);
        }
    }

    /** Nearest valid target: alive, not a parasite, never a creative or spectator player. */
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
            if (IParasite.isParasiteByTagOrInterface(entity)) {
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

    /** Sets the ship's kinematics so it rolls toward (nx, nz). */
    private static void roll(long shipId, double nx, double nz) {
        Object held = SHIPS.get(shipId);
        if (!(held instanceof org.valkyrienskies.core.api.ships.Ship ship)) {
            return;
        }
        try {
            var kinematics = ship.getKinematics().toBuilder()
                    .velocity(new org.joml.Vector3d(nx * ROLL_SPEED, ship.getVelocity().y(), nz * ROLL_SPEED))
                    // spin about the axis perpendicular to travel: (nz, 0, -nx)
                    .angularVelocity(new org.joml.Vector3d(nz * ROLL_SPIN, 0.0D, -nx * ROLL_SPIN))
                    .build();
            ((org.valkyrienskies.core.internal.ships.VsiShip) ship).unsafeSetKinematics(kinematics);
        } catch (Throwable failure) {
            org.apache.logging.log4j.LogManager.getLogger("epca").error(
                    "[pumpkin] roll failed", failure);
        }
    }

    /** Brings the ship to a halt. */
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
            // ignore
        }
    }

    private static void hurt(ServerLevel level, LivingEntity target) {
        target.hurt(level.damageSources().magic(), CONTACT_DAMAGE);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.COTH.get(), 30 * 20, 0, false, false, true));
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.FEAR.get(), 15 * 20, 0, false, false, true));
    }

    /** Removes the ship and puts the infested carved pumpkin block back where it was. */
    /** The ship's current block position, falling back to the original anchor. */
    private static BlockPos currentPos(Object ship, BlockPos fallback) {
        try {
            if (ship instanceof org.valkyrienskies.core.api.ships.Ship s) {
                var position = s.getTransform().getPosition();
                return BlockPos.containing(position.x(), position.y(), position.z());
            }
        } catch (Throwable ignored) {
            // fall through
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
            // best effort cleanup
        }
        SHIPS.remove(shipId);
        it.remove();
        BLOCKS.remove(shipId);
        LEVELS.remove(shipId);
        POSITIONS.removeIf(p -> !POSITIONS.isEmpty() && false);
    }
}
