package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.EpcaAnimations;
import org.tdddd.epca.impl.client.entity.IGlowRenderable;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.FollowTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

public class InfestedEndermite extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy, IGlowRenderable {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 5 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 8 * 20; 

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
            InfestedEndermite.class, EntityDataSerializers.INT
    );
    // UNSTABLE-only preemptive attack dodge + blink barrage tuning.
    private static final int DODGE_COOLDOWN_TICKS = 160;
    private static final int DODGE_ATTEMPTS = 32;
    private static final float BLINK_CHANCE = 0.05F;
    private static final int BLINK_INTERVAL_TICKS = 5;
    private static final int BLINK_MIN_TELEPORTS = 2;
    private static final int BLINK_MAX_TELEPORTS = 3;
    private static final int BLINK_ATTEMPTS = 16;
    private static final double BLINK_RADIUS_FALLBACK = 16.0D;
    private static final double BLINK_RADIUS_MAX = 32.0D;
    private int teleportCooldown = 0;
    private int blinkRemaining = 0;
    private int blinkTicks = 0;
    private LivingEntity blinkTarget;

    public InfestedEndermite(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        if (!level.isClientSide()) {
            int roll = this.random.nextInt(100);
            if (roll < 70) {
                this.setVariant(InfestedEndermite.Variant.DEFAULT);  
            } else {
                this.setVariant(InfestedEndermite.Variant.UNSTABLE);    
            }
        }
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public Identifier getGlowTexture() {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/entity/infested_endermite_unstable_glow.png");
    }

    public enum Variant {
        DEFAULT,
        UNSTABLE
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, EnderMan.class, 10, true, false, null));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.0, 16));
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.teleportCooldown > 0) this.teleportCooldown--;
            tickBlinkBarrage();
            if (this.getTarget() == null) {
                if (--this.ambientSoundTime <= 0) {
                    
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
                    playAmbientSound();
                }
            }
            
            updateFloating();
        } else {
            
            for (int i = 0; i < 2; ++i) {
                this.level().addParticle(
                        ParticleTypes.PORTAL,
                        this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        this.getY() + this.random.nextDouble() * this.getBbHeight() - 0.25,
                        this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        (this.random.nextDouble() - 0.5) * 2.0,
                        -this.random.nextDouble(),
                        (this.random.nextDouble() - 0.5) * 2.0
                );
            }
        }
    }
    public InfestedEndermite.Variant getVariant() {
        
        Integer variantOrdinal = this.entityData.get(DATA_VARIANT);
        if (variantOrdinal == null) {
            
            return InfestedEndermite.Variant.DEFAULT;
        }
        int index = Mth.clamp(variantOrdinal, 0, InfestedEndermite.Variant.values().length - 1);
        return InfestedEndermite.Variant.values()[index];
    }

    public void setVariant(InfestedEndermite.Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
        if (!this.level().isClientSide()) {
            applyVariantAttributes(); 
        }
    }

    private void applyVariantAttributes() {
        if (this.level().isClientSide()) return; 
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            double base = 16.0D;
            if (this.getVariant() == InfestedEndermite.Variant.UNSTABLE) {
                base = 16.0D * 1.25; 
            }
            followRange.setBaseValue(base);
        }
    }
    public void playAmbientSound() {
        if (!this.isSilent() && this.random.nextInt(3) == 0) {
            
            this.playSound(SoundEvents.ENDERMITE_AMBIENT, 0.95F, 0.8F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        
        return SoundEvents.ENDERMITE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        
        return SoundEvents.ENDERMITE_DEATH;
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        
        SoundEvent soundevent = this.getHurtSound(source);
        if (soundevent != null) {
            this.playSound(soundevent, 0.95F, 0.8F); 
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            
            if (shouldIgnoreDamageFrom(attacker)) {
                return false; 
            }
        }

        // UNSTABLE darts away the moment the attack is made, so the swing misses entirely.
        if (getVariant() == InfestedEndermite.Variant.UNSTABLE && !this.level().isClientSide()
                && this.teleportCooldown <= 0) {
            boolean attackerSource = source.getEntity() instanceof LivingEntity
                    || source.getDirectEntity() instanceof Projectile;
            if (attackerSource && tryVanillaStyleDodge()) {
                return false;
            }
        }

        if (!this.level().isClientSide() && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }
        float adjustedAmount = ((IParasite) this).onHurt(source, amount);
        boolean result = super.hurtServer(level, source, adjustedAmount);

        return result;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && !level.isClientSide() && target instanceof LivingEntity livingTarget) {
            tryStartBlinkBarrage(livingTarget);
        }
        return hurt;
    }

    private boolean tryVanillaStyleDodge() {
        for (int attempt = 0; attempt < DODGE_ATTEMPTS; ++attempt) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
            double y = this.getY() + (this.random.nextDouble() - 0.5) * 64.0;
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
            y = Mth.clamp(y, (double) this.level().getMinY(), (double) (this.level().getMaxY() - 1));
            if (playVanillaTeleportSoundAndTeleport(x, y, z)) {
                this.teleportCooldown = DODGE_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }

    // 5% chance, UNSTABLE only: blink around the victim a few times, one hop every few ticks.
    private void tryStartBlinkBarrage(LivingEntity victim) {
        if (victim == null) return;
        if (this.level().isClientSide()) return;
        if (getVariant() != InfestedEndermite.Variant.UNSTABLE) return;
        if (this.random.nextFloat() >= BLINK_CHANCE) return;
        this.blinkTarget = victim;
        this.blinkRemaining = BLINK_MIN_TELEPORTS + this.random.nextInt(BLINK_MAX_TELEPORTS - BLINK_MIN_TELEPORTS + 1);
        this.blinkTicks = BLINK_INTERVAL_TICKS;
        this.blinkTeleportAround(victim);
        this.blinkRemaining--;
        if (this.blinkRemaining <= 0) {
            this.blinkTarget = null;
            this.blinkTicks = 0;
            this.teleportCooldown = DODGE_COOLDOWN_TICKS;
        }
    }

    private void tickBlinkBarrage() {
        if (this.blinkRemaining <= 0) return;
        LivingEntity target = this.blinkTarget;
        if (target == null || target.isRemoved() || target.isDeadOrDying()) {
            this.blinkTarget = null;
            this.blinkRemaining = 0;
            this.blinkTicks = 0;
            this.teleportCooldown = DODGE_COOLDOWN_TICKS;
            return;
        }
        if (--this.blinkTicks > 0) return;
        this.blinkTeleportAround(target);
        this.blinkRemaining--;
        if (this.blinkRemaining <= 0) {
            this.blinkTarget = null;
            this.blinkTicks = 0;
            this.teleportCooldown = DODGE_COOLDOWN_TICKS;
        } else {
            this.blinkTicks = BLINK_INTERVAL_TICKS;
        }
    }

    private boolean blinkTeleportAround(LivingEntity target) {
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        double radius = followRange != null ? (double) (float) followRange.getValue() : BLINK_RADIUS_FALLBACK;
        if (radius > BLINK_RADIUS_MAX) radius = BLINK_RADIUS_MAX;
        for (int attempt = 0; attempt < BLINK_ATTEMPTS; ++attempt) {
            double x = target.getX() + (this.random.nextDouble() - 0.5) * 2.0 * radius;
            double y = target.getY() + (double) (this.random.nextInt(9) - 4);
            double z = target.getZ() + (this.random.nextDouble() - 0.5) * 2.0 * radius;
            y = Mth.clamp(y, (double) this.level().getMinY(), (double) (this.level().getMaxY() - 1));
            if (isBlinkDestinationUsable(x, y, z) && playVanillaTeleportSoundAndTeleport(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    // Every endermite teleport uses the vanilla enderman teleport sound (Feature C).
    private boolean playVanillaTeleportSoundAndTeleport(double x, double y, double z) {
        if (!this.randomTeleport(x, y, z, true)) return false;
        this.level().playSound(null, this.xo, this.yo, this.zo,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        this.getNavigation().stop();
        return true;
    }

    // Blink barrage destinations need a loaded chunk, a solid floor, two free blocks and no liquid.
    private boolean isBlinkDestinationUsable(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!this.level().hasChunkAt(pos) || this.level().isOutsideBuildHeight(pos)) return false;
        if (!this.level().getFluidState(pos).isEmpty()) return false;
        if (!this.level().isEmptyBlock(pos) || !this.level().isEmptyBlock(pos.above())) return false;
        BlockPos below = pos.below();
        BlockState belowState = this.level().getBlockState(below);
        return belowState.isFaceSturdy(this.level(), below, Direction.UP)
                && belowState.getFluidState().isEmpty();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", EpcaAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationTest<InfestedEndermite> event) {

            if (event.isMoving()) {
                
                event.setAnimation(RawAnimation.begin().thenLoop("walk"));
            } else if (getVariant() != InfestedEndermite.Variant.DEFAULT) {
                
                event.setAnimation(RawAnimation.begin().thenLoop("idle2"));
            } else {
                event.setAnimation(RawAnimation.begin().thenLoop("idle1"));
            }

        return PlayState.CONTINUE;
    }

    public static boolean checkInfestedEndermiteSpawnRules(
            EntityType<InfestedEndermite> entityType,
            ServerLevelAccessor level,
            EntitySpawnReason spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
            
            int stage = EvolutionManager.getStageForDimension(level.getLevel());
            if (stage < 2 || stage > 5) {
                return false;
            }
        }
        return level.getMaxLocalRawBrightness(pos) < 0;
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force, boolean sendEventAndTriggers) {
        
        if (vehicle instanceof Entity && (vehicle instanceof Boat || vehicle instanceof Minecart)) {
            return false;
        }
        return super.startRiding(vehicle, force, sendEventAndTriggers);
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
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_VARIANT, InfestedEndermite.Variant.DEFAULT.ordinal());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getString("Variant").isPresent()) {
            String variantName = tag.getStringOr("Variant", "");
            try {
                this.setVariant(InfestedEndermite.Variant.valueOf(variantName));
            } catch (IllegalArgumentException e) {
                
                this.setVariant(InfestedEndermite.Variant.DEFAULT);
            }
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
    public boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }

    public Identifier getTextureResource() {
        switch (getVariant()) {
            case UNSTABLE:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/infested_endermite_unstable.png");
            default:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/infested_endermite.png");
        }
    }
}
