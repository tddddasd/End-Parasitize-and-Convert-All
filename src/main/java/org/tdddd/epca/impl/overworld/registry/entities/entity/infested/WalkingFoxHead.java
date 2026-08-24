package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WalkingFoxHead extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 5 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 8 * 20; 
    
    public enum Variant {
        DEFAULT,
        SNOW
    }
    
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(WalkingFoxHead.class, EntityDataSerializers.INT);
    public int attackTicks = 0;  
    private LivingEntity attackTarget;

    public WalkingFoxHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);

        
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.3D) 
                .add(Attributes.MOVEMENT_SPEED, 0.28D) 
                .add(Attributes.ATTACK_DAMAGE, 1.2D) 
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(0, new WalkingFoxHeadAttackGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            
            if (attackTicks > 0) {
                attackTicks--;
                if (attackTicks == 8) {  
                    if (attackTarget != null && attackTarget.isAlive() && distanceToSqr(attackTarget) <= 4.0) {
                        doNormalAttack(attackTarget);
                    }
                }
                if (attackTicks == 0) {
                    attackTarget = null;
                    
                    
                }
            }

            
            if (this.getTarget() == null) {
                if (--this.ambientSoundTime <= 0) {
                    
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);

                    
                    playAmbientSound();
                }
            }
            
            updateFloating();
        }
    }

    
    public void playAmbientSound() {
        if (!this.isSilent()) {
            this.playSound(ModSoundEvents.WALKING_HEAD_SAY.get(), 1.0F, 1.0F);
        }
    }

    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.WALKING_HEAD_SAY.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        
        return ModSoundEvents.WALKING_HEAD_DEATH.get();
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (source.getEntity() instanceof LivingEntity attacker) {
            
            if (shouldIgnoreDamageFrom(attacker)) {
                return false; 
            }
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        
        float adjustedAmount = ((IParasite) this).onHurt(source, amount);

        
        boolean result = super.hurt(source, adjustedAmount);

        return result;
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<WalkingFoxHead> event) {

            if (event.isMoving()) {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
            } else if (attackTicks != 0) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("attack"));
            } else {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
            }

        return PlayState.CONTINUE;
    }

    public static boolean checkWalkingFoxHeadSpawnRules(
            EntityType<WalkingFoxHead> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            
            int stage = EvolutionManager.getStageForDimension(level.getLevel());

            
            if (stage < 2 || stage > 5) {
                return false;
            }
        }

        
        return level.getMaxLocalRawBrightness(pos) < 0;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                        @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        if (level.getBiome(this.blockPosition()).is(Tags.Biomes.IS_SNOWY)) {
            this.setVariant(Variant.SNOW);
        }
        return spawnGroupData;
    }

    
    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        
        if (vehicle instanceof Entity && (vehicle instanceof Boat || vehicle instanceof Minecart)) {
            return false;
        }
        return super.startRiding(vehicle, force);
    }

    
    @Override
    protected boolean canRide(Entity entity) {
        
        if (entity instanceof Entity && (entity instanceof Boat || entity instanceof Minecart)) {
            return false;
        }
        return super.canRide(entity);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.onDeath(source); 
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
    }

    public Variant getVariant() {
        int idx = this.entityData.get(DATA_VARIANT);
        if (idx < 0 || idx >= Variant.values().length) idx = 0;
        return Variant.values()[idx];
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public ResourceLocation getTextureResource() {
        return getVariant() == Variant.SNOW ?
                new ResourceLocation("epca", "textures/entity/walking_snow_fox_head.png") :
                new ResourceLocation("epca", "textures/entity/walking_fox_head.png");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", getVariant().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant", 8)) {
            try {
                setVariant(Variant.valueOf(tag.getString("Variant")));
            } catch (IllegalArgumentException ignored) {
                setVariant(Variant.DEFAULT);
            }
        }
    }

    
    private int floatingTime;

    
    private void updateFloating() {
        if (this.isInWater()) {
            
            Vec3 vec3 = this.getDeltaMovement();
            if (vec3.y < 0.0D) {
                
                this.setDeltaMovement(vec3.x, Math.max(vec3.y * 0.8D, -0.05D), vec3.z);
            }

            
            this.floatingTime++;

            
            if (this.floatingTime > 10) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.4D, 0.0D));
                this.floatingTime = 0;
            }
        } else {
            this.floatingTime = 0;
        }
    }

    
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            
            this.moveRelative(0.01F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    
    @Override
    protected boolean isAffectedByFluids() {
        return true;
    }

    
    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }

    public class WalkingFoxHeadAttackGoal extends MeleeAttackGoal {
        private final WalkingFoxHead mob;
        private LivingEntity target;

        public WalkingFoxHeadAttackGoal(WalkingFoxHead mob) {
            super(WalkingFoxHead.this, 1.0D, true);
            this.mob = mob; }

        @Override
        public boolean canUse() {
            target = mob.getTarget();
            return target != null && target.isAlive() && mob.distanceToSqr(target) <= 64.0 && mob.attackTicks == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && mob.distanceToSqr(target) <= 64.0 && mob.attackTicks == 0;
        }

        @Override
        public void start() {
            
            mob.attackTicks = 10;   
            mob.attackTarget = target;
        }

        @Override
        public void stop() {
            
        }
    }

    public void doNormalAttack(LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        float damage = 1.2f; 
        if (this.getVariant() == Variant.SNOW) {
            target.setTicksFrozen(180); 
        }
        target.hurt(damageSources().mobAttack(this), damage);
        this.playSound(ModSoundEvents.INFESTED_FOX_BITE.get());
    }
}
