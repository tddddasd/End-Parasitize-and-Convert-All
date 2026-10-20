package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The black hole opened by The In.
 *
 * <p>It is not an entity: the server only keeps its position and age, applies the 128 block attraction
 * and the contact removal, and mirrors the list to clients through {@link BlackHoleSyncPacket} so the
 * client can purely render it.</p>
 */
public final class BlackHoleManager {

    /** How long a hole exists. */
    public static final int LIFETIME_TICKS = 60 * 20;
    public static final double PULL_RADIUS = 128.0D;
    private static final double CONSUME_RADIUS = 2.0D;

    /** One hole: where it is and how old it is. */
    public record Hole(Vec3 pos, int age) {
    }

    private static final List<Hole> HOLES = new ArrayList<>();
    private static final List<Hole> CLIENT_HOLES = new ArrayList<>();

    private BlackHoleManager() {
    }

    /** Opens a hole at {@code pos}. */
    public static void spawn(ServerLevel level, Vec3 pos) {
        HOLES.add(new Hole(pos, 0));
        broadcast(level);
    }

    /** Server tick: ageing, attraction and consumption. */
    public static void tick(ServerLevel level) {
        if (HOLES.isEmpty()) {
            return;
        }
        boolean changed = false;
        List<Hole> next = new ArrayList<>(HOLES.size());
        for (Hole hole : HOLES) {
            int age = hole.age() + 1;
            if (age >= LIFETIME_TICKS) {
                changed = true;
                continue;
            }
            Vec3 centre = hole.pos();
            for (Entity entity : level.getEntities((Entity) null,
                    net.minecraft.world.phys.AABB.ofSize(centre, PULL_RADIUS * 2, PULL_RADIUS * 2, PULL_RADIUS * 2),
                    e -> !(e instanceof Player) && e.isAlive())) {
                double distance = entity.position().distanceTo(centre);
                if (distance <= CONSUME_RADIUS * shrink(age)) {
                    consume(entity);
                    continue;
                }
                Vec3 pull = centre.subtract(entity.position()).normalize()
                        .scale(Math.min(0.9D, 24.0D / Math.max(1.0D, distance)));
                entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
                entity.hurtMarked = true;
            }
            next.add(new Hole(centre, age));
        }
        if (HOLES.size() != next.size()) {
            changed = true;
        }
        HOLES.clear();
        HOLES.addAll(next);
        if (changed) {
            broadcast(level);
        }
    }

    /** 0..1 visual scale, shrinks during the last ten seconds. */
    public static float shrink(int age) {
        int left = LIFETIME_TICKS - age;
        return left >= 200 ? 1.0F : Math.max(0.0F, left / 200.0F);
    }

    private static void consume(Entity entity) {
        entity.remove(Entity.RemovalReason.KILLED);
        entity.teleportTo(1000000.0D, -4800.0D, 1000000.0D);
        entity.setNoGravity(true);
        entity.setDeltaMovement(0.0D, -100.0D, 0.0D);
        entity.setNoGravity(false);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setTarget(null);
        } else if (entity instanceof LivingEntity living) {
            living.setJumping(false);
            living.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void broadcast(ServerLevel level) {
        org.tdddd.epca.impl.network.ModNetwork.sendBlackHoles(level, List.copyOf(HOLES));
    }

    /** Client mirror, updated by the packet. */
    public static void setClientHoles(List<Hole> holes) {
        CLIENT_HOLES.clear();
        CLIENT_HOLES.addAll(holes);
    }

    public static List<Hole> clientHoles() {
        return CLIENT_HOLES;
    }

    /** Client tick: just age them locally so the shrink matches. */
    public static void tickClient() {
        if (CLIENT_HOLES.isEmpty()) {
            return;
        }
        List<Hole> next = new ArrayList<>(CLIENT_HOLES.size());
        for (Hole hole : CLIENT_HOLES) {
            int age = hole.age() + 1;
            if (age < LIFETIME_TICKS) {
                next.add(new Hole(hole.pos(), age));
            }
        }
        CLIENT_HOLES.clear();
        CLIENT_HOLES.addAll(next);
    }
}
