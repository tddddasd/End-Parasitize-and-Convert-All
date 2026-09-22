package org.tdddd.epca.impl.overworld.registry.entities.entity.onesent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.client.entity.EpcaGeoAnimations;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.FollowTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.base.AbstractOnesentEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class Curbug extends AbstractOnesentEntity {

    private final Map<Integer, Integer> touchingEntities = new HashMap<>();
    private static final int TOUCH_THRESHOLD = 2;
    private int growTimer = -1;
    private int spawnAnimationTimer = 0;
    private boolean isHiding = false;
    private BlockPos hidingPos = null;
    private int hideTimer = 0;
    private static final int HIDE_DURATION = 200;
    private int hideSearchCooldown = 0;

    public Curbug(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 2;
        if (!level.isClientSide) {
            this.growTimer = this.random.nextInt(1200) + 1200;
        }
        this.navigation = new GroundPathNavigation(this, level);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ARMOR, 1.5D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FleeFromTargetersGoal(this, 16.0F, 1.2D, 1.4D));
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<Player>(this, Player.class, 7.0F, 1.0D, 1.2D) {
            @Override
            public boolean canUse() {
                Player nearestPlayer = this.mob.level().getNearestPlayer(this.mob, this.maxDist);
                if (nearestPlayer != null) { this.toAvoid = nearestPlayer; return true; }
                return false;
            }
        });
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.0, 16));
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(2, new HideInFoliageGoal(this, 12.0D, 1.0D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0.008F) {
            @Override
            public boolean canUse() {
                if (Curbug.this.isHiding) return false;
                return super.canUse();
            }
            @Override
            public boolean canContinueToUse() {
                if (Curbug.this.isHiding) return false;
                return super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new RandomSoundGoal(this, ModSoundEvents.CURBUG_SAY.get()));
    }

    @Override
    public void tick() {
        super.tick();

        if (spawnAnimationTimer > 0) spawnAnimationTimer--;

        if (hideTimer > 0) {
            hideTimer--;
            if (hideTimer <= 0) {
                isHiding = false;
                hidingPos = null;
            }
        }

        if (!this.level().isClientSide) {
            handleEntityContact();

            if (this.isAlive()) {
                if (growTimer > 0) {
                    growTimer--;
                    if (growTimer <= 0) {
                        if (this.isInWater()) growIntoFins();
                        else growIntoRupter();
                    }
                }
            }
        }
    }

    static class FleeFromTargetersGoal extends AvoidEntityGoal<LivingEntity> {
        private final Curbug curbug;
        private final double radius;
        private int searchCooldown = 0;
        private static final int SEARCH_INTERVAL = 5;

        public FleeFromTargetersGoal(Curbug curbug, float maxDist, double walkSpeed, double sprintSpeed) {
            super(curbug, LivingEntity.class, maxDist, walkSpeed, sprintSpeed, e -> false);
            this.curbug = curbug;
            this.radius = maxDist;
        }

        @Override
        public boolean canUse() {
            if (--searchCooldown > 0) return false;
            searchCooldown = SEARCH_INTERVAL;

            LivingEntity nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Mob mob : curbug.level().getEntitiesOfClass(
                    Mob.class,
                    curbug.getBoundingBox().inflate(radius, 4.0D, radius),
                    m -> m != null && m.isAlive() && m.getTarget() == curbug)) {
                double d = curbug.distanceToSqr(mob);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = mob;
                }
            }
            if (nearest != null) {
                this.toAvoid = nearest;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.toAvoid == null || !this.toAvoid.isAlive()) return false;
            if (this.toAvoid instanceof Mob mob && mob.getTarget() != curbug) {
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void stop() {
            super.stop();
            this.toAvoid = null;
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.onDeath(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && this.isHiding) {
            this.isHiding = false;
            this.hideTimer = 0;
            this.hidingPos = null;
        }
        return result;
    }

    private void handleEntityContact() {
        touchingEntities.keySet().removeIf(id -> {
            Entity entity = this.level().getEntity(id);
            return entity == null || !entity.isAlive() ||
                    !this.getBoundingBox().intersects(entity.getBoundingBox());
        });

        for (Entity entity : this.level().getEntities(this, this.getBoundingBox())) {
            if (entity instanceof LivingEntity living && entity.isAlive() && !IParasite.isParasiteByTagOrInterface(living)) {
                int entityId = entity.getId();
                int touchTime = touchingEntities.getOrDefault(entityId, 0) + 1;
                touchingEntities.put(entityId, touchTime);
                if (touchTime >= TOUCH_THRESHOLD) {
                    applyCothEffect(living);
                    touchingEntities.remove(entityId);
                }
            }
        }
    }

    private void applyCothEffect(LivingEntity target) {
        MobEffectInstance current = target.getEffect(ModEffects.COTH.get());
        int newAmplifier = current != null ? Math.min(current.getAmplifier() + 1, 2) : 0;
        target.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 400, newAmplifier, false, true, true));
    }

    private void growIntoFins() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Fins fins = ModEntities.FINS.get().create(serverLevel);
        if (fins != null) {
            fins.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            serverLevel.addFreshEntity(fins);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.CURBUG_EVOLVE.get(), this.getSoundSource(), 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
            this.discard();
        }
    }

    private void growIntoRupter() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Ripper rupter = ModEntities.RIPPER.get().create(serverLevel);
        if (rupter != null) {
            rupter.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            serverLevel.addFreshEntity(rupter);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.CURBUG_EVOLVE.get(), this.getSoundSource(), 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
            this.discard();
        }
    }

    @Override
    protected SoundEvent getDeathSound() { return ModSoundEvents.RIPPER_DEATH.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSoundEvents.RIPPER_HUNT.get(); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, this::predicate));
        controllers.add(new AnimationController<>(this, "spawn_controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, this::spawnPredicate));
    }

    private PlayState spawnPredicate(AnimationState<Curbug> event) {
        if (this.spawnAnimationTimer > 0) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("spawn"));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private PlayState predicate(AnimationState<Curbug> event) {
        if (this.spawnAnimationTimer > 0) return PlayState.STOP;
        event.getController().setAnimation(RawAnimation.begin().thenLoop(isMoving() ? "walk" : "idle"));
        return PlayState.CONTINUE;
    }

    public static boolean checkBuglinSpawnRules(
            EntityType<Curbug> entityType, ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        int blockLight = levelAccessor.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = levelAccessor.getBrightness(LightLayer.SKY, pos);
        long dayTime = levelAccessor.getLevelData().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime <= 23000;

        if (isNight) { if (blockLight > 8) return false; }
        else { if (skyLight > 8 || blockLight > 8) return false; }

        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            if (stage < 0 || stage > 2) return false;
        }
        return levelAccessor.getMaxLocalRawBrightness(pos) < 8;
    }

    static class HideInFoliageGoal extends Goal {
        private final Curbug curbug;
        private final double searchRadius;
        private final double speed;
        private BlockPos targetPos;
        private int searchCooldown = 0;
        private static final int SEARCH_INTERVAL = 20;

        public HideInFoliageGoal(Curbug curbug, double searchRadius, double speed) {
            this.curbug = curbug;
            this.searchRadius = searchRadius;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (curbug.getTarget() != null) return false;
            if (curbug.isHiding) return false;
            if (curbug.isInWater()) return false;

            if (--searchCooldown > 0) return false;
            searchCooldown = SEARCH_INTERVAL;

            targetPos = findFoliage();
            return targetPos != null;
        }

        private BlockPos findFoliage() {
            BlockPos origin = curbug.blockPosition();
            int r = (int) searchRadius;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        mp.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (!isFoliage(mp)) continue;
                        double d = origin.distSqr(mp);
                        if (d < bestDist) {
                            bestDist = d;
                            best = mp.immutable();
                        }
                    }
                }
            }
            return best;
        }

        private boolean isFoliage(BlockPos pos) {
            BlockState state = curbug.level().getBlockState(pos);
            if (state.isAir()) return false;
            if (!state.getFluidState().isEmpty()) return false;

            VoxelShape shape = state.getCollisionShape(curbug.level(), pos);
            if (!shape.isEmpty()) return false;

            Block block = state.getBlock();
            if (block instanceof BaseRailBlock) return false;
            if (block instanceof RedStoneWireBlock) return false;
            if (block instanceof TripWireBlock) return false;

            if (block instanceof BushBlock
                    || block instanceof TallGrassBlock
                    || block instanceof FlowerBlock
                    || block instanceof DoublePlantBlock) {
                return true;
            }
            if (state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.SMALL_FLOWERS)
                    || state.is(BlockTags.TALL_FLOWERS)) {
                return true;
            }
            return !state.isSolid();
        }

        @Override
        public void start() {
            if (targetPos != null) {
                curbug.getNavigation().moveTo(
                        targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                        speed);
            }
        }

        @Override
        public void tick() {
            if (targetPos == null) return;
            double distSqr = curbug.distanceToSqr(
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5);
            if (distSqr < 1.0) {
                curbug.isHiding = true;
                curbug.hidingPos = targetPos.immutable();
                curbug.hideTimer = HIDE_DURATION;
                curbug.getNavigation().stop();
                curbug.getLookControl().setLookAt(
                        targetPos.getX() + 0.5,
                        targetPos.getY() + 0.5,
                        targetPos.getZ() + 0.5);
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (curbug.isHiding) return false;
            if (targetPos == null) return false;
            return !curbug.getNavigation().isDone();
        }

        @Override
        public void stop() {
            targetPos = null;
            if (!curbug.isHiding) {
                curbug.getNavigation().stop();
            }
        }
    }
}
