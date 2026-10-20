package org.tdddd.epca.impl.overworld.registry.entities.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The black hole summoned by the Kill Stick ("The In").
 *
 * <p>It lives for one minute, pulls every non-player entity within 128 blocks towards its centre and
 * consumes whatever touches it with the same removal the wand uses (teleport far below the world, kill,
 * disable AI). In its last seconds it shrinks and then disappears.</p>
 */
public class BlackHole extends Entity {

    /** How long the hole exists, in ticks. */
    public static final int LIFETIME_TICKS = 60 * 20;
    /** Pull radius. */
    private static final double PULL_RADIUS = 128.0D;
    /** Anything this close is consumed. */
    private static final double CONSUME_RADIUS = 2.0D;
    /** Entity radius of the visible cube. */
    private static final double VISUAL_RADIUS = 2.5D;

    private int age = 0;

    public BlackHole(EntityType<? extends BlackHole> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", this.age);
    }

    /** 0..1, shrinks to zero in the last ten seconds. */
    public float getVisualScale() {
        int left = LIFETIME_TICKS - this.age;
        if (left >= 200) {
            return 1.0F;
        }
        return Math.max(0.0F, left / 200.0F);
    }

    public double getVisualRadius() {
        return VISUAL_RADIUS * this.getVisualScale();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        this.age++;
        if (this.age >= LIFETIME_TICKS) {
            this.discard();
            return;
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 centre = this.position();
        for (Entity entity : server.getEntities(this, this.getBoundingBox().inflate(PULL_RADIUS))) {
            if (entity instanceof Player || entity == this || !entity.isAlive()) {
                continue;
            }
            double distance = entity.position().distanceTo(centre);
            if (distance <= CONSUME_RADIUS * Math.max(0.35F, this.getVisualScale())) {
                consume(entity);
                continue;
            }
            // gradual attraction: the closer it is, the harder it is pulled
            Vec3 pull = centre.subtract(entity.position()).normalize()
                    .scale(Math.min(0.9D, 24.0D / Math.max(1.0D, distance)));
            entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
            entity.hurtMarked = true;
        }
    }

    /** The same removal the wand performs for the entities it clears. */
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
}
