package org.tdddd.epca.impl.overworld.registry.entities.entity.onesent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.base.AbstractOnesentEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.HashMap;
import java.util.Map;

public class Curbug extends AbstractOnesentEntity {

    private final Map<Integer, Integer> touchingEntities = new HashMap<>();
    private static final int TOUCH_THRESHOLD = 2;

    private int growTimer = -1;
    private int spawnAnimationTimer = 0;
    private static final int SPAWN_ANIMATION_DURATION = 20;

    public Curbug(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 2;
        if (!level.isClientSide) {
            this.growTimer = this.random.nextInt(1200) + 1200;
        }
        this.navigation = new GroundPathNavigation(this, level);
    }

    public void triggerSpawnAnimation() {
        this.spawnAnimationTimer = SPAWN_ANIMATION_DURATION;
    }

    // ────────── Attributes ──────────

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ARMOR, 1.5D)
                .build();
    }

    // ────────── AI Goals ──────────

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<Player>(this, Player.class, 7.0F, 1.0D, 1.2D) {
            @Override
            public boolean canUse() {
                Player nearestPlayer = this.mob.level().getNearestPlayer(this.mob, this.maxDist);
                if (nearestPlayer != null) { this.toAvoid = nearestPlayer; return true; }
                return false;
            }
        });
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0.008F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new RandomSoundGoal(this, ModSoundEvents.CURBUG_SAY.get()));
    }

    // ────────── Tick ──────────

    @Override
    public void tick() {
        super.tick();

        if (spawnAnimationTimer > 0) spawnAnimationTimer--;

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

    // ────────── Hurt (delegated to AbstractEpcaEntity) ──────────

    // ────────── Die ──────────

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.onDeath(source);
    }

    // ────────── Entity contact ──────────

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

    // ────────── Growth ──────────

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

    // ────────── Sounds ──────────

    @Override
    protected SoundEvent getDeathSound() { return ModSoundEvents.RIPPER_DEATH.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSoundEvents.RIPPER_HUNT.get(); }

    // ────────── Animation ──────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, this::predicate));
        controllers.add(new AnimationController<>(this, "spawn_controller", 3, this::spawnPredicate));
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

    // ────────── Spawn Rules ──────────

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
}
