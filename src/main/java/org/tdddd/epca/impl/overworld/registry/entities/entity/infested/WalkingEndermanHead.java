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
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.EpcaAnimations;
import org.tdddd.epca.impl.client.entity.IGlowRenderable;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.CarryConfigManager;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
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
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import com.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

public class WalkingEndermanHead extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy, IGlowRenderable {

    @Override
    public Identifier getGlowTexture() {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/entity/walking_enderman_head_unstable_glow.png");
    }
    
    public enum Variant {
        DEFAULT,
        UNSTABLE
    }
    private static final EntityDataAccessor<Integer> DATA_CARRIED_ENTITY_ID = SynchedEntityData.defineId(WalkingEndermanHead.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(WalkingEndermanHead.class, EntityDataSerializers.INT);
    private static final int TELEPORT_COOLDOWN_MIN = 160;
    private static final int TELEPORT_COOLDOWN_MAX = 200;
    private int teleportCooldown = 0;
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
    // Blink barrage state (UNSTABLE only).
    private LivingEntity blinkTarget;
    private int blinkRemaining = 0;
    private int blinkTicks = 0;
    private Entity carriedEntity; 
    
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 5 * 20;
    private static final int MAX_AMBIENT_SOUND_DELAY = 8 * 20;
    private int floatingTime;
    public WalkingEndermanHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
        this.navigation = new GroundPathNavigation(this, level);

        if (!level.isClientSide()) {
            
            int roll = this.random.nextInt(100);
            if (roll < 70) {
                this.setVariant(Variant.DEFAULT);
            } else {
                this.setVariant(Variant.UNSTABLE);
            }
        }
    }
    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(0, new PickUpParasiteGoal());
        this.goalSelector.addGoal(1, new PlacePassengerGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 32.0D));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            
            if (this.teleportCooldown > 0) this.teleportCooldown--;
            tickBlinkBarrage();
            Entity carried = this.getCarriedEntity();
            if (carried != null) {
                updateCarriedEntityPosition(carried);
                if (this.isOnFire()) {
                    carried.igniteForSeconds(5);
                } else {
                    carried.setRemainingFireTicks(-1);
                }
                if (this.isInWater()) {
                    carried.clearFire();
                }
                if (!this.isAlive()) {
                    releaseCarriedEntity();
                }
            }
            if (this.getTarget() == null) {
                if (--this.ambientSoundTime <= 0) {
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
                    playAmbientSound();
                }
            }
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
        updateFloating();
    }
    private boolean attemptTeleportAndPlace(LivingEntity target) {
        if (target == null || getCarriedEntity() == null) return false;
        
        RandomSource rand = random;
        for (int i = 0; i < 16; i++) {
            double x = target.getX() + (rand.nextDouble() - 0.5) * 8.0;
            double y = target.getY() + rand.nextInt(3) - 1;
            double z = target.getZ() + (rand.nextDouble() - 0.5) * 8.0;
            if (randomTeleport(x, y, z, true)) {
                teleportCooldown = TELEPORT_COOLDOWN_MAX; 
                
                return attemptPlaceCarried();
            }
        }
        return false;
    }
    private boolean attemptPlaceCarried() {
        Entity passenger = getCarriedEntity();
        if (passenger == null) return false;

        BlockPos basePos = blockPosition();
        RandomSource rand = random;
        int placeRange = 6; 

        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = rand.nextInt(placeRange + 1) - placeRange / 2;
            int dy = rand.nextInt(placeRange + 1) - placeRange / 2;
            int dz = rand.nextInt(placeRange + 1) - placeRange / 2;
            BlockPos placePos = basePos.offset(dx, dy, dz);

            if (level().isEmptyBlock(placePos)) {
                BlockPos below = placePos.below();
                BlockState belowState = level().getBlockState(below);
                if (belowState.isFaceSturdy(level(), below, Direction.UP)) {
                    
                    releaseCarriedEntity();
                    passenger.setPos(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5);
                    if (passenger instanceof Mob) {
                        ((Mob) passenger).setNoAi(false);
                    }
                    passenger.noPhysics = false;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            releaseCarriedEntity();
        }
        super.remove(reason);
    }
    private boolean isTeleportFromExternalSource() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            
            if (className.startsWith("net.minecraft.commands.") ||
                    className.startsWith("net.minecraft.server.commands.") ||
                    className.startsWith("net.minecraft.server.players.") ||
                    className.contains("CommandBlock") ||
                    className.contains("PlayerList") ||
                    className.contains("TeleportCommand") ||
                    className.contains("Player")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        
        if (!this.level().isClientSide() && this.getCarriedEntity() != null && isTeleportFromExternalSource()) {
            releaseCarriedEntity();
        }
        super.teleportTo(x, y, z);
    }

    @Override
    public boolean randomTeleport(double x, double y, double z, boolean particleEffects) {
        
        return super.randomTeleport(x, y, z, particleEffects);
    }

    private void applyVariantAttributes() {
        if (this.level().isClientSide()) return; 
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            double base = 32.0D;
            if (this.getVariant() == Variant.UNSTABLE) {
                base = 32.0D * 1.25; 
            }
            followRange.setBaseValue(base);
        }
    }
    public Entity getCarriedEntity() {
        if (this.level().isClientSide()) {
            int id = this.entityData.get(DATA_CARRIED_ENTITY_ID);
            return id == -1 ? null : this.level().getEntity(id);
        } else {
            return carriedEntity;
        }
    }

    private void setCarriedEntity(Entity entity) {
        if (this.level().isClientSide()) return;
        this.carriedEntity = entity;
        this.entityData.set(DATA_CARRIED_ENTITY_ID, entity == null ? -1 : entity.getId());
        if (entity != null) {
            if (entity instanceof Mob) {
                ((Mob) entity).setNoAi(true);
            }
            entity.noPhysics = true;
        }
    }

    private void updateCarriedEntityPosition(Entity passenger) {
        if (passenger == null) return;
        double yOffset = passenger.getBbHeight() < 1.25 ? passenger.getBbHeight() / 2.0 : 1.2;
        Vec3 pos = new Vec3(0, yOffset, 0).yRot(-this.getYRot() * ((float) Math.PI / 180F) - (float) Math.PI / 2);
        passenger.setPos(this.getX() + pos.x, this.getY() + pos.y, this.getZ() + pos.z);
        passenger.xo = passenger.getX();
        passenger.yo = passenger.getY();
        passenger.zo = passenger.getZ();
    }

    private void releaseCarriedEntity() {
        Entity carried = this.getCarriedEntity();
        if (carried != null) {
            if (carried instanceof Mob) {
                ((Mob) carried).setNoAi(false);
            }
            carried.noPhysics = false;
            this.setCarriedEntity(null);
        }
    }
    private boolean tryTeleportRandomly() {
        if (this.level().isClientSide()) return false;
        double x = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
        double y = this.getY() + (double)(this.random.nextInt(64) - 32);
        double z = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
        return this.randomTeleport(x, y, z, true);
    }

    private boolean tryForcedTeleport() {
        for (int attempt = 0; attempt < 16; ++attempt) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 16.0;
            double y = this.getY() + (double)(this.random.nextInt(16) - 8);
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 16.0;
            if (this.randomTeleport(x, y, z, true)) {
                this.level().playSound(null, this.xo, this.yo, this.zo,
                        ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    private void reboundProjectile(Projectile projectile) {
        Vec3 vec3 = projectile.getDeltaMovement();
        projectile.setDeltaMovement(vec3.scale(-1.0));
    }
    public Variant getVariant() {
        Integer variantOrdinal = this.entityData.get(DATA_VARIANT);
        if (variantOrdinal == null) return Variant.DEFAULT;
        int index = Mth.clamp(variantOrdinal, 0, Variant.values().length - 1);
        return Variant.values()[index];
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
        if (!this.level().isClientSide()) {
            applyVariantAttributes(); 
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            if (shouldIgnoreDamageFrom(attacker)) return false;
        }
        if (!this.level().isClientSide() && source.getDirectEntity() instanceof Projectile projectile) {
            // UNSTABLE uses the vanilla-style dart away before the hit lands, so this branch only
            // handles the non-UNSTABLE deflection/rebound behaviour.
            if (getVariant() != Variant.UNSTABLE) {
                if (tryForcedTeleport()) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                    return false;
                } else {
                    reboundProjectile(projectile);
                    return false;
                }
            }
        }

        // UNSTABLE darts away the moment the attack is made, so the swing misses entirely.
        if (getVariant() == Variant.UNSTABLE && !this.level().isClientSide() && this.teleportCooldown <= 0) {
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

        if (!this.level().isClientSide() && result && this.teleportCooldown <= 0
                && getVariant() != Variant.UNSTABLE) {
            if (tryTeleportRandomly()) {
                this.teleportCooldown = this.random.nextInt(TELEPORT_COOLDOWN_MIN, TELEPORT_COOLDOWN_MAX + 1);
                playPostDamageTeleportSound();
            }
        }
        return result;
    }

    // Non-UNSTABLE keeps the mod's custom portal sound. UNSTABLE is normally handled by the
    // dart-away above, but its other teleport paths use the vanilla teleport sound.
    private void playPostDamageTeleportSound() {
        if (getVariant() == Variant.UNSTABLE) {
            this.level().playSound(null, this.xo, this.yo, this.zo,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        } else {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    private boolean tryVanillaStyleDodge() {
        for (int attempt = 0; attempt < DODGE_ATTEMPTS; ++attempt) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
            double y = this.getY() + (this.random.nextDouble() - 0.5) * 64.0;
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
            y = Mth.clamp(y, (double) this.level().getMinY(), (double) (this.level().getMaxY() - 1));
            if (this.randomTeleport(x, y, z, true)) {
                this.level().playSound(null, this.xo, this.yo, this.zo,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                this.getNavigation().stop();
                this.teleportCooldown = DODGE_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && !level.isClientSide() && target instanceof LivingEntity livingTarget) {
            tryStartBlinkBarrage(livingTarget);
        }
        return hurt;
    }

    // 5% chance, UNSTABLE only: blink around the victim a few times, one hop every few ticks.
    private void tryStartBlinkBarrage(LivingEntity victim) {
        if (victim == null) return;
        if (this.level().isClientSide()) return;
        if (getVariant() != Variant.UNSTABLE) return;
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
            if (isBlinkDestinationUsable(x, y, z) && this.randomTeleport(x, y, z, true)) {
                this.level().playSound(null, this.xo, this.yo, this.zo,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                this.getNavigation().stop();
                return true;
            }
        }
        return false;
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
    public void playAmbientSound() {
        if (this.getTarget() != null && !this.isSilent()) {
            this.playSound(ModSoundEvents.INFESTED_ENDERMAN_SCREAM.get());
        } else if (!this.isSilent()) {
            this.playSound(ModSoundEvents.INFESTED_ENDERMAN_IDLE.get());
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INFESTED_ENDERMAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.INFESTED_ENDERMAN_DEATH.get();
    }

    @Override
    public void die(DamageSource source) {
        releaseCarriedEntity(); 
        super.die(source);
        this.onDeath(source); 
    }
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
    public boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", EpcaAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationTest<WalkingEndermanHead> event) {
        boolean isMoving = event.isMoving();

        if (isMoving) {
            event.setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else {
            if (getVariant() != Variant.DEFAULT) {
                event.setAnimation(RawAnimation.begin().thenLoop("idle2"));
            } else {
                event.setAnimation(RawAnimation.begin().thenLoop("idle1"));
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_CARRIED_ENTITY_ID, -1);
        entityData.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", this.getVariant().name());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getString("Variant").isPresent()) {
            try {
                this.setVariant(Variant.valueOf(tag.getStringOr("Variant", "")));
            } catch (IllegalArgumentException e) {
                this.setVariant(Variant.DEFAULT);
            }
        }
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }
    public static boolean checkWalkingEndermanHeadSpawnRules(
            EntityType<WalkingEndermanHead> entityType,
            ServerLevelAccessor level,
            EntitySpawnReason spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(level.getLevel());
            if (stage < 4 || stage > 7) return false;
        }
        return level.getMaxLocalRawBrightness(pos) < 0;
    }
    private boolean tryTeleportToEntity(LivingEntity target) {
        if (target == null) return false;
        RandomSource rand = random;
        for (int attempt = 0; attempt < 16; attempt++) {
            double offsetX = (rand.nextDouble() - 0.5) * 3.0;
            double offsetZ = (rand.nextDouble() - 0.5) * 3.0;
            double x = target.getX() + offsetX;
            double y = target.getY() + rand.nextInt(2) - 1; 
            double z = target.getZ() + offsetZ;
            if (randomTeleport(x, y, z, true)) {
                teleportCooldown = TELEPORT_COOLDOWN_MAX;
                return true;
            }
        }
        return false;
    }
    private class PickUpParasiteGoal extends Goal {
        private static final int SEARCH_RADIUS = 32;
        private LivingEntity carryTarget;
        private int cooldown = 0;

        private boolean isCarriedByOther(LivingEntity target) {
            return !WalkingEndermanHead.this.level().getEntitiesOfClass(WalkingEndermanHead.class,
                            target.getBoundingBox().inflate(32),
                            e -> e != WalkingEndermanHead.this && e.getCarriedEntity() == target)
                    .isEmpty();
        }

        @Override
        public boolean canUse() {
            if (cooldown-- > 0) return false;
            if (WalkingEndermanHead.this.getCarriedEntity() != null) return false;
            LivingEntity attackTarget = WalkingEndermanHead.this.getTarget();
            if (attackTarget == null) return false;

            AABB aabb = WalkingEndermanHead.this.getBoundingBox().inflate(SEARCH_RADIUS);
            List<LivingEntity> list = WalkingEndermanHead.this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                    e -> e != WalkingEndermanHead.this &&
                            e != attackTarget &&
                            isCarryableTarget(e) &&
                            !e.isOnFire() &&
                            !isCarriedByOther(e)
            );
            list.removeIf(candidate -> attackTarget.distanceToSqr(candidate) > 24 * 24);

            if (list.isEmpty()) return false;
            Optional<LivingEntity> nonFire = list.stream().filter(e -> !e.isOnFire()).findFirst();
            carryTarget = nonFire.orElseGet(() -> list.get(WalkingEndermanHead.this.random.nextInt(list.size())));
            return carryTarget != null;
        }

        private boolean isCarryableTarget(LivingEntity target) {
            EntityType<?> carrierType = WalkingEndermanHead.this.getType();
            EntityType<?> targetType = target.getType();
            return CarryConfigManager.INSTANCE.isEntityCarryable(carrierType, targetType);
        }

        @Override
        public void start() {
            if (carryTarget != null) {
                WalkingEndermanHead.this.getNavigation().moveTo(carryTarget, 1.2);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return carryTarget != null && carryTarget.isAlive() &&
                    WalkingEndermanHead.this.getCarriedEntity() == null &&
                    WalkingEndermanHead.this.distanceToSqr(carryTarget) > 1.2 * 1.2;
        }

        @Override
        public void tick() {
            if (carryTarget == null) return;
            double distSq = WalkingEndermanHead.this.distanceToSqr(carryTarget);
            if (distSq <= 24 * 24 && WalkingEndermanHead.this.teleportCooldown <= 0) {
                if (WalkingEndermanHead.this.tryTeleportToEntity(carryTarget)) {
                    WalkingEndermanHead.this.setCarriedEntity(carryTarget);
                    WalkingEndermanHead.this.level().playSound(null, WalkingEndermanHead.this.getX(), WalkingEndermanHead.this.getY(), WalkingEndermanHead.this.getZ(),
                            ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                    WalkingEndermanHead.this.getNavigation().stop();
                    LivingEntity target = WalkingEndermanHead.this.getTarget();
                    if (target != null && WalkingEndermanHead.this.attemptTeleportAndPlace(target)) {
                        
                        carryTarget = null;
                        cooldown = 40;
                        return;
                    }
                    
                    carryTarget = null;
                    cooldown = 40;
                    return;
                }
            }
            if (distSq <= 1.2 * 1.2) {
                
                if (WalkingEndermanHead.this.getCarriedEntity() == null) {
                    WalkingEndermanHead.this.setCarriedEntity(carryTarget);
                    WalkingEndermanHead.this.level().playSound(null, WalkingEndermanHead.this.getX(), WalkingEndermanHead.this.getY(), WalkingEndermanHead.this.getZ(),
                            ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                    WalkingEndermanHead.this.getNavigation().stop();
                    LivingEntity target = WalkingEndermanHead.this.getTarget();
                    if (target != null && WalkingEndermanHead.this.attemptTeleportAndPlace(target)) {
                        
                        carryTarget = null;
                        cooldown = 40;
                        return;
                    }
                }
                carryTarget = null;
                cooldown = 40;
            } else {
                WalkingEndermanHead.this.getNavigation().moveTo(carryTarget, 1.2);
            }
        }

        @Override
        public void stop() {
            carryTarget = null;
            WalkingEndermanHead.this.getNavigation().stop();
        }
    }
    private class PlacePassengerGoal extends Goal {
        private static final int PLACE_RANGE = 6;
        private int placeAttempts = 0;
        private int cooldown = 0;

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            return WalkingEndermanHead.this.getCarriedEntity() != null
                    && WalkingEndermanHead.this.getTarget() != null
                    && WalkingEndermanHead.this.getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return WalkingEndermanHead.this.getCarriedEntity() != null
                    && WalkingEndermanHead.this.getTarget() != null
                    && WalkingEndermanHead.this.getTarget().isAlive()
                    && placeAttempts < 5;
        }

        @Override
        public void start() {
            placeAttempts = 0;
            WalkingEndermanHead.this.getNavigation().moveTo(WalkingEndermanHead.this.getTarget(), 1.2);
        }

        @Override
        public void stop() {
            cooldown = 40;
            placeAttempts = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = WalkingEndermanHead.this.getTarget();
            if (target == null) return;

            if (WalkingEndermanHead.this.teleportCooldown <= 0) {
                if (tryTeleportToTarget(target)) {
                    attemptPlacePassenger(target);
                    return;
                }
            }

            double distSq = WalkingEndermanHead.this.distanceToSqr(target);
            if (distSq <= PLACE_RANGE * PLACE_RANGE) {
                attemptPlacePassenger(target);
            } else {
                WalkingEndermanHead.this.getNavigation().moveTo(target, 1.2);
            }
        }

        private boolean tryTeleportToTarget(LivingEntity target) {
            RandomSource rand = WalkingEndermanHead.this.random;
            for (int i = 0; i < 16; i++) {
                double x = target.getX() + (rand.nextDouble() - 0.5) * 8.0;
                double y = target.getY() + rand.nextInt(3) - 1;
                double z = target.getZ() + (rand.nextDouble() - 0.5) * 8.0;
                if (WalkingEndermanHead.this.randomTeleport(x, y, z, true)) {
                    WalkingEndermanHead.this.teleportCooldown = TELEPORT_COOLDOWN_MAX;
                    return true;
                }
            }
            return false;
        }

        private void attemptPlacePassenger(LivingEntity target) {
            Entity passenger = WalkingEndermanHead.this.getCarriedEntity();
            if (passenger == null) return;

            BlockPos targetPos = target.blockPosition();
            RandomSource rand = WalkingEndermanHead.this.random;

            for (int attempt = 0; attempt < 10; attempt++) {
                int dx = rand.nextInt(PLACE_RANGE + 1) - PLACE_RANGE / 2;
                int dy = rand.nextInt(PLACE_RANGE + 1) - PLACE_RANGE / 2;
                int dz = rand.nextInt(PLACE_RANGE + 1) - PLACE_RANGE / 2;
                BlockPos placePos = targetPos.offset(dx, dy, dz);

                if (WalkingEndermanHead.this.level().isEmptyBlock(placePos)) {
                    BlockPos below = placePos.below();
                    if (WalkingEndermanHead.this.level().getBlockState(below).isFaceSturdy(
                            WalkingEndermanHead.this.level(), below, Direction.UP)) {
                        WalkingEndermanHead.this.releaseCarriedEntity();
                        passenger.setPos(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5);
                        if (passenger instanceof Mob) {
                            ((Mob) passenger).setNoAi(false);
                        }
                        passenger.noPhysics = false;
                        placeAttempts = 5;
                        return;
                    }
                }
            }
            placeAttempts++;
        }
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

    public Identifier getTextureResource() {
        switch (getVariant()) {
            case UNSTABLE:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/walking_enderman_head_unstable.png");
            default:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/walking_enderman_head.png");
        }
    }
}