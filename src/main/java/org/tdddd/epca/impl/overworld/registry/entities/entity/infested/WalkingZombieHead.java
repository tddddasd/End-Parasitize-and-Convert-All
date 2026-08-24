package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.base.AbstractInfestedEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class WalkingZombieHead extends AbstractInfestedEntity {

    public WalkingZombieHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        this.fakeDeathEnabled = false; // walking heads don't fake death
        this.navigation = new GroundPathNavigation(this, level);
    }

    // ────────── Attributes ──────────

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.8D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build();
    }

    // ────────── AI Goals ──────────

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
    }

    // ────────── Sounds ──────────

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.WALKING_HEAD_SAY.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.WALKING_HEAD_SAY.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.WALKING_HEAD_DEATH.get();
    }

    // ────────── No fake death (simple hurt is inherited from AbstractInfestedEntity) ──────────

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.onDeath(source);
    }

    // ────────── Animation ──────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<WalkingZombieHead> event) {
        boolean inWater = this.isInWater();
        if (inWater) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(event.isMoving() ? "walk_water" : "idle_water"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(event.isMoving() ? "walk" : "idle"));
        }
        return PlayState.CONTINUE;
    }

    // ────────── Spawn Rules ──────────

    public static boolean checkWalkingZombieHeadSpawnRules(
            EntityType<WalkingZombieHead> entityType, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(level.getLevel());
            if (stage < 2 || stage > 4) return false;
        }
        return level.getMaxLocalRawBrightness(pos) < 0;
    }
}
