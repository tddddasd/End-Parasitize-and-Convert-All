package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.client.entity.IGlowRenderable;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.data.CarryConfigManager;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class InfestedEnderman extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy , IHeadRotatable, IGlowRenderable {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private int arayaBuffTimer = 0;
    private int damageIncreaseTimer = 0;
    private boolean hasTriggeredFiftyPercent = false;
    private int lastResistanceStage = -1;
    private final Set<UUID> chargedTargets = new HashSet<>();
    private static final EntityDataAccessor<Boolean> DATA_IS_RUNNING = SynchedEntityData.defineId(InfestedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(InfestedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final UUID WANDER_SPEED_ID = UUID.fromString("A2766B59-7066-4402-AD81-0E3B7B6C2B9B");
    private static final AttributeModifier WANDER_SPEED_REDUCTION = new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 12 * 20;
    private static final int MAX_AMBIENT_SOUND_DELAY = 16 * 20;
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(InfestedEnderman.class, EntityDataSerializers.BOOLEAN);
    private int fakeDeathTimer = 27;
    private BlockPos deathPosition;
    private static final int TELEPORT_COOLDOWN_MIN = 160;
    private static final int TELEPORT_COOLDOWN_MAX = 200;
    private int teleportCooldown = 0;
    private static final EntityDataAccessor<Float> DATA_IDLE2_PROBABILITY = SynchedEntityData.defineId(InfestedEnderman.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CARRY_IDLE2_PROBABILITY = SynchedEntityData.defineId(InfestedEnderman.class, EntityDataSerializers.FLOAT);
    private static final float DEFAULT_IDLE2_PROB = 0.3f;
    private static final float DEFAULT_CARRY_IDLE2_PROB = 0.3f;
    private int breakCooldown = 0;
    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(InfestedEnderman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
            InfestedEnderman.class, EntityDataSerializers.INT
    );
    private double baseSpeed = 0.35D;
    private double chaseSpeed = 0.45D;
    private int floatingTime;

    
    private Entity carriedEntity; 
    private static final EntityDataAccessor<Integer> DATA_CARRIED_ENTITY_ID = SynchedEntityData.defineId(InfestedEnderman.class, EntityDataSerializers.INT);

    public boolean isArayaMode = false;
    private int summoningTicks = 0;               
    private int summonsRemaining = 0;              
    private LivingEntity summonTarget;             

    
    public boolean isArayaMinion = false;         
    private int minionLife = 0;                    
    private Vec3 minionVelocity = Vec3.ZERO;       
    public boolean isStationaryMinion = false;   

    
    private boolean isCharging = false;              
    private int chargeRemainingTicks = 0;            
    private Vec3 chargeDirection = Vec3.ZERO;         
    private double chargeDistanceCovered = 0;         
    private int chargeCooldown = 0;                   
    private long lastSuccessfulAttackTime = 0;        
    private static final EntityDataAccessor<Boolean> DATA_IS_STATIONARY_MINION =
            SynchedEntityData.defineId(InfestedEnderman.class, EntityDataSerializers.BOOLEAN);

    
    public InfestedEnderman(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 12;

        
        if (!level.isClientSide) {
            int roll = this.random.nextInt(100);
            if (roll < 70) {
                this.setVariant(InfestedEnderman.Variant.DEFAULT);  
            } else {
                this.setVariant(InfestedEnderman.Variant.UNSTABLE);    
            }
        }

        
        if (!level().isClientSide) {
            lastSuccessfulAttackTime = level().getGameTime();
        }

        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
        this.setMaxUpStep(0.5F);
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public ResourceLocation getGlowTexture() {
        return new ResourceLocation(epca.MODID, "textures/entity/infested_enderman_unstable_glow.png");
    }

    public enum Variant {
        DEFAULT,
        UNSTABLE
    }

    
    public InfestedEnderman.Variant getVariant() {
        
        Integer variantOrdinal = this.entityData.get(DATA_VARIANT);
        if (variantOrdinal == null) {
            
            return InfestedEnderman.Variant.DEFAULT;
        }

        
        int index = Mth.clamp(variantOrdinal, 0, InfestedEnderman.Variant.values().length - 1);
        return InfestedEnderman.Variant.values()[index];
    }

    public void setVariant(InfestedEnderman.Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
        if (!this.level().isClientSide) {
            applyVariantAttributes(); 
        }
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!this.level().isClientSide) {
            breakCooldown = tryBreakLightSources(this, breakCooldown);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
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
        
        if (!this.level().isClientSide && this.getCarriedEntity() != null && isTeleportFromExternalSource()) {
            releaseCarriedEntity();
        }
        super.teleportTo(x, y, z);
    }

    @Override
    public boolean randomTeleport(double x, double y, double z, boolean particleEffects) {
        
        return super.randomTeleport(x, y, z, particleEffects);
    }

    
    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }
    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    
    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ATTACK_SPEED, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ARMOR, 4.0D)
                .build();
    }

    
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new PickUpParasiteGoal());       
        this.goalSelector.addGoal(1, new PlacePassengerGoal());       
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 32.0D));
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
    }

    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide) {
            if (isCharging) {
                handleCharge();      
                return;              
            }

            
            if (chargeCooldown > 0) chargeCooldown--;

            
            if (getVariant() == Variant.UNSTABLE && chargeCooldown <= 0 && getTarget() != null && getTarget().isAlive()) {
                
                boolean yAligned = Math.abs(getY() - getTarget().getY()) < 1.5; 
                double horizDist = distanceTo(getTarget());
                if (yAligned && horizDist < 8.5 && chargeCooldown <= 0) {
                    long now = level().getGameTime();
                    if (now - lastSuccessfulAttackTime > 15 * 20) {
                        startCharge();
                    }
                }
            }

            if (isArayaMode && chargeCooldown <= 0 && getTarget() != null && getTarget().isAlive()) {
                LivingEntity target = getTarget();
                int scar = target.getPersistentData().getInt("SwordScar");
                if (scar >= 99 || this.getHealth() <= this.getMaxHealth() * 0.5f) {
                    startCharge();
                }
            }
        }

        if (isArayaMinion) {
            if (minionLife > 0) {
                minionLife--;
                if (!isStationaryMinion) {
                    
                    this.move(MoverType.SELF, minionVelocity);
                    checkMinionCollision();
                }
                
            } else {
                this.discard();
            }
            return; 
        }

        if (isArayaMode && !level().isClientSide) {
            arayaBuffTimer++;
            if (arayaBuffTimer >= 60 * 20) {
                arayaBuffTimer = 0;
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1, false, false));
                damageIncreaseTimer = 120; // 6秒
            }
            if (damageIncreaseTimer > 0) damageIncreaseTimer--;

            float healthRatio = this.getHealth() / this.getMaxHealth();
            int stage;
            if (healthRatio >= 1.0f) stage = 0;
            else if (healthRatio >= 0.75f) stage = 1;
            else if (healthRatio >= 0.6f) stage = 2;
            else stage = -1;

            if (stage != lastResistanceStage) {
                this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                if (stage != -1) {
                    int amplifier = (stage == 0) ? 3 : (stage == 1 ? 2 : 1); // IV=3, III=2, II=1
                    this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, amplifier, false, false));
                }
                lastResistanceStage = stage;
            }

            if (!hasTriggeredFiftyPercent && healthRatio <= 0.5f) {
                hasTriggeredFiftyPercent = true;
                AABB aabb = this.getBoundingBox().inflate(10.0);
                List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                        e -> e != this && e.isAlive());
                for (LivingEntity entity : list) {
                    float entityRatio = entity.getHealth() / entity.getMaxHealth();
                    int lostPercent = (int) ((1 - entityRatio) * 100);
                    if (lostPercent < 1) lostPercent = 1;
                    int currentScar = entity.getPersistentData().getInt("SwordScar");
                    currentScar = Math.min(99, currentScar + lostPercent);
                    entity.getPersistentData().putInt("SwordScar", currentScar);
                }
            }
        }
        
        if (!this.level().isClientSide) {
            boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            boolean hasTarget = this.getTarget() != null;
            boolean hasPassenger = this.getCarriedEntity() != null;

            if (!hasPassenger) {
                this.setRunning(isMoving && hasTarget);
                this.setWalking(isMoving && !hasTarget);
                if (isMoving && hasTarget) this.setWalking(false);
                else if (isMoving && !hasTarget) this.setRunning(false);
                else { this.setRunning(false); this.setWalking(false); }
            } else {
                this.setRunning(false);
                this.setWalking(false);
            }
        }

        
        if (isFakingDeath()) {
            super.tick();
            fakeDeathTimer--;
            if (fakeDeathTimer <= 0) {
                if (!this.level().isClientSide) {
                    
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ModParticles.COTH.get(),
                                this.getX(), this.getY() + 0.5, this.getZ(),
                                10, 0.3, 0.45, 0.3, 0.0);
                        serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                                this.getX(), this.getY() + 0.5, this.getZ(),
                                7, 0.4, 0.3, 0.4, 0.2);

                        BlockPos deathPos = this.deathPosition;
                        long seed = this.random.nextLong();
                        int delay = this.random.nextInt(30) + 40;
                        serverLevel.getServer().tell(new TickTask(
                                serverLevel.getServer().getTickCount() + delay,
                                () -> {
                                    spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed));
                                    spawnBuglins(serverLevel, deathPos, RandomSource.create(seed));
                                }
                        ));

                        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                                deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
                        cloud.setRadius(1.5F);
                        cloud.setDuration(60);
                        cloud.setRadiusPerTick(0);
                        cloud.setWaitTime(0);
                        cloud.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 1, false, true));
                        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
                        cloud.addEffect(new MobEffectInstance(ModEffects.ENDER_EROSION.get(), 20, 0, false, true));
                        serverLevel.addFreshEntity(cloud);
                    }
                }
                this.discard();
                return;
            }
            return;
        }

        
        AttributeInstance movementAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute != null) {
            boolean hasTarget = this.getTarget() != null;
            movementAttribute.removeModifier(WANDER_SPEED_ID);
            if (hasTarget) {
                movementAttribute.setBaseValue(chaseSpeed);
            } else {
                movementAttribute.setBaseValue(baseSpeed);
                movementAttribute.addTransientModifier(WANDER_SPEED_REDUCTION);
            }
        }

        
        if (!this.level().isClientSide && this.getTarget() == null) {
            if (--this.ambientSoundTime <= 0) {
                this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
                playAmbientSound();
            }
        }

        
        updateFloating();

        
        if (this.teleportCooldown > 0) this.teleportCooldown--;

        
        if (!this.level().isClientSide) {
            Entity carried = this.getCarriedEntity();
            if (carried != null) {
                
                updateCarriedEntityPosition(carried);
                
                if (this.isOnFire()) {
                    carried.setSecondsOnFire(5);
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

            
            boolean namedAraya = hasCustomName() && "Araya".equals(getCustomName().getString());
            boolean isUnstable = getVariant() == Variant.UNSTABLE;
            if (isUnstable && namedAraya && !isArayaMode) {
                enterArayaMode();
            } else if ((!isUnstable || !namedAraya) && isArayaMode) {
                exitArayaMode();
            }

            
            if (isArayaMode && summoningTicks > 0) {
                if (summonTarget == null || !summonTarget.isAlive()) {
                    summoningTicks = 0; 
                } else {
                    summoningTicks--;
                    if (summoningTicks % 10 == 0 && summonsRemaining > 0) {
                        spawnArayaMinion(summonTarget);
                        summonsRemaining--;
                    }
                }
            }
        }

        
        if (this.level().isClientSide) {
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

    private void startCharge() {
        if (level().isClientSide) return;
        chargedTargets.clear();

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        isCharging = true;
        chargeRemainingTicks = 15;
        chargeDistanceCovered = 0;

        Vec3 toTarget = target.position().subtract(position());
        chargeDirection = new Vec3(toTarget.x, 0, toTarget.z).normalize();

        if (chargeDirection.lengthSqr() < 1e-4) {
            float yaw = getYRot();
            float rad = yaw * (float) Math.PI / 180.0F;
            chargeDirection = new Vec3(-Math.sin(rad), 0, Math.cos(rad)).normalize();
        }

        setNoAi(true);
        noPhysics = true;
        chargeCooldown = 90 * 20;

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void handleCharge() {
        if (chargeRemainingTicks <= 0) {
            endCharge();
            return;
        }

        double step = 8.0 / 15.0;
        Vec3 movement = chargeDirection.scale(step);
        Vec3 newPos = position().add(movement);
        if (newPos.y < level().getMinBuildHeight() || newPos.y > level().getMaxBuildHeight()) {
            endCharge();
            return;
        }
        setPos(newPos.x, newPos.y, newPos.z);
        chargeDistanceCovered += step;
        // 原有的碰撞伤害和音效
        AABB aabb = getBoundingBox();
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != this && e.isAlive() && !IParasite.isParasiteByTagOrInterface(e));
        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(ModEffects.ENDER_EROSION.get(), 60, 5, false, false));
            entity.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, 0, false, false));

            if (!chargedTargets.contains(entity.getUUID())) {
                Level level = entity.level();
                chargedTargets.add(entity.getUUID());
                int scar = entity.getPersistentData().getInt("SwordScar");
                if (scar > 0) {
                    entity.getPersistentData().putInt("SwordScar", 0);
                    float maxHealth = entity.getMaxHealth();
                    float slashDamage = maxHealth * scar / 100.0f;
                    Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
                    Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
                    DamageSource minimumSource = new DamageSource(holder);
                    entity.hurt(minimumSource, slashDamage);
                }
            }
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.INFESTED_ENDERMAN_TARGETING.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        chargeRemainingTicks--;
        if (chargeDistanceCovered >= 8.0) {
            endCharge();
        }
    }

    private void endCharge() {
        isCharging = false;
        setNoAi(false);
        noPhysics = false;
        chargeRemainingTicks = 0;
        chargeDirection = Vec3.ZERO;
        chargeDistanceCovered = 0;
        chargedTargets.clear();
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

    private void applyVariantAttributes() {
        if (this.level().isClientSide) return; 
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            double base = 32.0D;
            if (this.getVariant() == Variant.UNSTABLE) {
                base = 32.0D * 1.25; 
            }
            followRange.setBaseValue(base);
        }
    }

    private void enterArayaMode() {
        isArayaMode = true;
        
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(1918.0);
        }
        this.setHealth(1918.0f);
        
        releaseCarriedEntity();
        
        summoningTicks = 0;
        summonsRemaining = 0;
        arayaBuffTimer = 0;
        damageIncreaseTimer = 0;
        hasTriggeredFiftyPercent = false;
        lastResistanceStage = -1;
        chargedTargets.clear();
    }

    private void exitArayaMode() {
        isArayaMode = false;
        
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(32.0);
        }
        if (this.getHealth() > 32.0f) {
            this.setHealth(32.0f);
        }
        
        summoningTicks = 0;
        summonsRemaining = 0;
    }

    private void applyRandomEffect(LivingEntity target) {
        int choice = random.nextInt(4);
        switch (choice) {
            case 0 -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            case 1 -> target.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), 120, 0));
            case 2 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            case 3 -> target.setSecondsOnFire(6);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {

        if (isArayaMode && target instanceof LivingEntity livingTarget) {
            if (summoningTicks > 0) return false;
            startSummoning(livingTarget);
            Level level = this.level();
            int scarLevel = livingTarget.getPersistentData().getInt("SwordScar");
            int bonus = (scarLevel / 10) * 10;
            if (bonus > 50) bonus = 50;
            float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float directDamage = baseDamage * (1 + bonus / 100.0f);

            boolean hurt = livingTarget.hurt(livingTarget.damageSources().mobAttack(this), directDamage);
            if (hurt) {
                Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
                Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
                DamageSource minimumSource = new DamageSource(holder);
                livingTarget.hurt(minimumSource, directDamage * 0.1f);

                applyRandomEffect(livingTarget);

                scarLevel = Math.min(99, scarLevel + 1);
                livingTarget.getPersistentData().putInt("SwordScar", scarLevel);

                lastSuccessfulAttackTime = level().getGameTime();
                return true;
            }
            return false;
        } else {
            boolean hurt = super.doHurtTarget(target);
            if (hurt && !level().isClientSide) {
                lastSuccessfulAttackTime = level().getGameTime();
            }
            return hurt;
        }
    }

    private void startSummoning(LivingEntity target) {
        int count = random.nextInt(2) + 2; 
        spawnArayaMinion(target); 
        if (count > 1) {
            summonsRemaining = count - 1;
            summoningTicks = summonsRemaining * 10; 
        } else {
            summoningTicks = 0;
        }
        summonTarget = target;
    }

    private void spawnArayaMinion(LivingEntity target) {
        if (target == null) return;
        InfestedEnderman minion = new InfestedEnderman(ModEntities.INFESTED_ENDERMAN.get(), this.level());
        minion.setVariant(Variant.UNSTABLE);
        minion.setPos(this.getX(), this.getY(), this.getZ());

        
        minion.isArayaMinion = true;
        minion.minionLife = 10; 
        minion.setInvulnerable(true);  
        minion.noPhysics = true;       
        minion.setNoGravity(true);     
        minion.setNoAi(true);          

        
        Vec3 dir = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ()).normalize();
        minion.minionVelocity = dir.scale(0.25); 

        this.level().addFreshEntity(minion);
    }

    private void checkMinionCollision() {
        AABB aabb = this.getBoundingBox();
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != this && e.isAlive());
        for (LivingEntity target : list) {
            if (!IParasite.isParasiteByTagOrInterface(target)) { 
                target.hurt(this.level().damageSources().magic(), 9.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0)); 
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && isArayaMode && summoningTicks > 0) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0); 
        }
    }

    
    public Entity getCarriedEntity() {
        if (this.level().isClientSide) {
            int id = this.entityData.get(DATA_CARRIED_ENTITY_ID);
            return id == -1 ? null : this.level().getEntity(id);
        } else {
            return carriedEntity;
        }
    }

    private void setCarriedEntity(Entity entity) {
        if (this.level().isClientSide) return;
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
        
        double yOffset;
        if (passenger.getBbHeight() < 1.25) {
            yOffset = passenger.getBbHeight() / 2.0;
        } else {
            yOffset = 1.2;
        }
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

    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        this.entityData.define(DATA_IDLE2_PROBABILITY, DEFAULT_IDLE2_PROB);
        this.entityData.define(DATA_CARRY_IDLE2_PROBABILITY, DEFAULT_CARRY_IDLE2_PROB);
        this.entityData.define(DATA_CARRIED_ENTITY_ID, -1);
        this.entityData.define(DATA_VARIANT, InfestedEnderman.Variant.DEFAULT.ordinal());
        this.entityData.define(DATA_IS_STATIONARY_MINION, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Idle2Probability", this.entityData.get(DATA_IDLE2_PROBABILITY));
        tag.putFloat("CarryIdle2Probability", this.entityData.get(DATA_CARRY_IDLE2_PROBABILITY));
        tag.putString("Variant", this.getVariant().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Idle2Probability", net.minecraft.nbt.Tag.TAG_FLOAT)) {
            this.entityData.set(DATA_IDLE2_PROBABILITY, tag.getFloat("Idle2Probability"));
        } else {
            this.entityData.set(DATA_IDLE2_PROBABILITY, DEFAULT_IDLE2_PROB);
        }
        if (tag.contains("CarryIdle2Probability", net.minecraft.nbt.Tag.TAG_FLOAT)) {
            this.entityData.set(DATA_CARRY_IDLE2_PROBABILITY, tag.getFloat("CarryIdle2Probability"));
        } else {
            this.entityData.set(DATA_CARRY_IDLE2_PROBABILITY, DEFAULT_CARRY_IDLE2_PROB);
        }
        if (tag.contains("Variant", 8)) {
            String variantName = tag.getString("Variant");
            try {
                this.setVariant(InfestedEnderman.Variant.valueOf(variantName));
            } catch (IllegalArgumentException e) {
                
                this.setVariant(InfestedEnderman.Variant.DEFAULT);
            }
        }
    }
    public boolean isStationaryMinion() {
        return this.entityData.get(DATA_IS_STATIONARY_MINION);
    }

    public void setStationaryMinion(boolean stationary) {
        this.entityData.set(DATA_IS_STATIONARY_MINION, stationary);
    }

    
    public boolean isRunning() { return this.entityData.get(DATA_IS_RUNNING); }
    public boolean isWalking() { return this.entityData.get(DATA_IS_WALKING); }
    private void setRunning(boolean running) { this.entityData.set(DATA_IS_RUNNING, running); }
    private void setWalking(boolean walking) { this.entityData.set(DATA_IS_WALKING, walking); }

    
    public boolean isInvulnerable() { return this.entityData.get(DATA_IS_INVULNERABLE); }
    public void setInvulnerable(boolean invulnerable) { this.entityData.set(DATA_IS_INVULNERABLE, invulnerable); }

    
    public void playAmbientSound() {
        if (this.getTarget() != null && !this.isSilent()) {
            this.playSound(ModSoundEvents.INFESTED_ENDERMAN_SCREAM.get());
        } else if (!this.isSilent()) {
            this.playSound(ModSoundEvents.INFESTED_ENDERMAN_IDLE.get());
        }
    }
    @Override protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INFESTED_ENDERMAN_HURT.get();
    }
    @Override protected SoundEvent getDeathSound() { return ModSoundEvents.INFESTED_ENDERMAN_DEATH.get(); }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (isCharging) return false;

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (shouldIgnoreDamageFrom(attacker)) return false;
        }
        if (isFakingDeath() || isInvulnerable()) return false;

        if (!this.level().isClientSide && source.getDirectEntity() instanceof Projectile projectile) {
            if (tryForcedTeleport()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                return false;
            }
            else { reboundProjectile(projectile); return false; }
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        float adjustedAmount = ((IParasite) this).onHurt(source, amount);
        boolean result = super.hurt(source, adjustedAmount);

        if (!this.level().isClientSide && result && this.teleportCooldown <= 0) {
            if (tryTeleportRandomly()) {
                this.teleportCooldown = this.random.nextInt(TELEPORT_COOLDOWN_MIN, TELEPORT_COOLDOWN_MAX + 1);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
        return result;
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

    private boolean tryTeleportRandomly() {
        if (this.level().isClientSide) return false;
        double x = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
        double y = this.getY() + (double)(this.random.nextInt(64) - 32);
        double z = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
        return this.randomTeleport(x, y, z, true);
    }

    private void reboundProjectile(Projectile projectile) {
        Vec3 vec3 = projectile.getDeltaMovement();
        projectile.setDeltaMovement(vec3.scale(-1.0));
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
        }
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        
        AnimationController<InfestedEnderman> normalController = new AnimationController<>(this, "normal_controller", 4, this::normalAnimationPredicate);
        
        AnimationController<InfestedEnderman> stationaryController = new AnimationController<>(this, "stationary_controller", 0, this::stationaryAnimationPredicate);
        controllers.add(normalController, stationaryController);
    }

    private PlayState normalAnimationPredicate(software.bernie.geckolib.core.animation.AnimationState<InfestedEnderman> event) {
        
        if (event.getAnimatable().isStationaryMinion()) {
            return PlayState.STOP;
        }

        
        boolean hasPassenger = this.getCarriedEntity() != null;
        boolean isMoving = event.isMoving();

        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("dead"));
        } else if (hasPassenger) {
            if (this.isRunning() || this.getTarget() != null) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("carry_run"));
            } else if (isMoving) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("carry_walk"));
            } else {
                if (getVariant() != Variant.DEFAULT) {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("carry_idle2"));
                } else {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("carry_idle1"));
                }
            }
        } else {
            if (this.isRunning() || this.getTarget() != null) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
            } else if (isMoving) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
            } else {
                if (getVariant() != Variant.DEFAULT) {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle2"));
                } else {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle1"));
                }
            }
        }
        return PlayState.CONTINUE;
    }

    private PlayState stationaryAnimationPredicate(AnimationState<InfestedEnderman> event) {
        
        if (!event.getAnimatable().isStationaryMinion()) {
            return PlayState.STOP;
        }
        
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle2"));
        return PlayState.CONTINUE;
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
            
            return !InfestedEnderman.this.level().getEntitiesOfClass(InfestedEnderman.class,
                            target.getBoundingBox().inflate(32), 
                            e -> e != InfestedEnderman.this && e.getCarriedEntity() == target)
                    .isEmpty();
        }

        @Override
        public boolean canUse() {
            if (InfestedEnderman.this.isArayaMode) return false;

            if (cooldown-- > 0) return false;
            if (InfestedEnderman.this.getCarriedEntity() != null) return false;
            LivingEntity attackTarget = InfestedEnderman.this.getTarget();
            if (attackTarget == null) return false;

            AABB aabb = InfestedEnderman.this.getBoundingBox().inflate(SEARCH_RADIUS);
            List<LivingEntity> list = InfestedEnderman.this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                    e -> e != InfestedEnderman.this &&
                            e != attackTarget &&
                            isCarryableTarget(e) &&
                            !e.isOnFire() &&
                            !isCarriedByOther(e)
            );

            
            list.removeIf(candidate -> attackTarget.distanceToSqr(candidate) > 24 * 24);

            if (list.isEmpty()) return false;
            Optional<LivingEntity> nonFire = list.stream().filter(e -> !e.isOnFire()).findFirst();
            carryTarget = nonFire.orElseGet(() -> list.get(InfestedEnderman.this.random.nextInt(list.size())));
            return carryTarget != null;
        }

        private boolean isCarryableTarget(LivingEntity target) {
            
            EntityType<?> carrierType = InfestedEnderman.this.getType();
            EntityType<?> targetType = target.getType();

            boolean configAllows = CarryConfigManager.INSTANCE.isEntityCarryable(carrierType, targetType);

            if (configAllows) {
                return true;
            }

            
            
            return configAllows;
        }

        @Override
        public void start() {
            if (carryTarget != null) {
                InfestedEnderman.this.getNavigation().moveTo(carryTarget, 1.2);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return carryTarget != null && carryTarget.isAlive() &&
                    InfestedEnderman.this.getCarriedEntity() == null &&
                    InfestedEnderman.this.distanceToSqr(carryTarget) > 1.2 * 1.2;
        }

        @Override
        public void tick() {
            if (carryTarget == null) return;
            double distSq = InfestedEnderman.this.distanceToSqr(carryTarget);

            
            if (distSq <= 8 * 8 && InfestedEnderman.this.teleportCooldown <= 0) {
                if (InfestedEnderman.this.tryTeleportToEntity(carryTarget)) {
                    
                    
                    InfestedEnderman.this.setCarriedEntity(carryTarget);
                    InfestedEnderman.this.level().playSound(null, InfestedEnderman.this.getX(), InfestedEnderman.this.getY(), InfestedEnderman.this.getZ(),
                            ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                    InfestedEnderman.this.getNavigation().stop();

                    
                    LivingEntity target = InfestedEnderman.this.getTarget();
                    if (target != null && InfestedEnderman.this.attemptTeleportAndPlace(target)) {
                        
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
                
                if (InfestedEnderman.this.getCarriedEntity() == null) {
                    InfestedEnderman.this.setCarriedEntity(carryTarget);
                    InfestedEnderman.this.level().playSound(null, InfestedEnderman.this.getX(), InfestedEnderman.this.getY(), InfestedEnderman.this.getZ(),
                            ModSoundEvents.INFESTED_ENDERMAN_PORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                    InfestedEnderman.this.getNavigation().stop();

                    
                    LivingEntity target = InfestedEnderman.this.getTarget();
                    if (target != null && InfestedEnderman.this.attemptTeleportAndPlace(target)) {
                        
                        carryTarget = null;
                        cooldown = 40;
                        return;
                    }
                }
                carryTarget = null;
                cooldown = 40;
            } else {
                InfestedEnderman.this.getNavigation().moveTo(carryTarget, 1.2);
            }
        }

        @Override
        public void stop() {
            carryTarget = null;
            InfestedEnderman.this.getNavigation().stop();
        }
    }

    
    private class PlacePassengerGoal extends Goal {
        private static final int PLACE_RANGE = 6;
        private int placeAttempts = 0;
        private int cooldown = 0;

        @Override
        public boolean canUse() {
            if (InfestedEnderman.this.isArayaMode) return false;

            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            return InfestedEnderman.this.getCarriedEntity() != null
                    && InfestedEnderman.this.getTarget() != null
                    && InfestedEnderman.this.getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return InfestedEnderman.this.getCarriedEntity() != null
                    && InfestedEnderman.this.getTarget() != null
                    && InfestedEnderman.this.getTarget().isAlive()
                    && placeAttempts < 5;
        }

        @Override
        public void start() {
            placeAttempts = 0;
            InfestedEnderman.this.getNavigation().moveTo(InfestedEnderman.this.getTarget(), 1.2);
        }

        @Override
        public void stop() {
            cooldown = 40;
            placeAttempts = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = InfestedEnderman.this.getTarget();
            if (target == null) return;

            
            if (InfestedEnderman.this.teleportCooldown <= 0) {
                if (tryTeleportToTarget(target)) {
                    
                    attemptPlacePassenger(target);
                    return;
                }
            }

            double distSq = InfestedEnderman.this.distanceToSqr(target);
            if (distSq <= PLACE_RANGE * PLACE_RANGE) {
                attemptPlacePassenger(target);
            } else {
                InfestedEnderman.this.getNavigation().moveTo(target, 1.2);
            }
        }

        private boolean tryTeleportToTarget(LivingEntity target) {
            RandomSource rand = InfestedEnderman.this.random;
            for (int i = 0; i < 16; i++) {
                double x = target.getX() + (rand.nextDouble() - 0.5) * 8.0;
                double y = target.getY() + rand.nextInt(3) - 1;
                double z = target.getZ() + (rand.nextDouble() - 0.5) * 8.0;
                if (InfestedEnderman.this.randomTeleport(x, y, z, true)) {
                    InfestedEnderman.this.teleportCooldown = TELEPORT_COOLDOWN_MAX;
                    return true;
                }
            }
            return false;
        }

        private void attemptPlacePassenger(LivingEntity target) {
            Entity passenger = InfestedEnderman.this.getCarriedEntity();
            if (passenger == null) return;

            BlockPos targetPos = target.blockPosition();
            RandomSource rand = InfestedEnderman.this.random;

            for (int attempt = 0; attempt < 10; attempt++) {
                int dx = rand.nextInt(PLACE_RANGE + 1) - PLACE_RANGE / 2;
                int dy = rand.nextInt(PLACE_RANGE + 1) - PLACE_RANGE / 2;
                int dz = rand.nextInt(PLACE_RANGE + 1) - PLACE_RANGE / 2;
                BlockPos placePos = targetPos.offset(dx, dy, dz);

                if (InfestedEnderman.this.level().isEmptyBlock(placePos)) {
                    BlockPos below = placePos.below();
                    BlockState belowState = InfestedEnderman.this.level().getBlockState(below);
                    if (belowState.isFaceSturdy(InfestedEnderman.this.level(), below, Direction.UP)) {
                        InfestedEnderman.this.releaseCarriedEntity();
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
    public void die(DamageSource source) {
        releaseCarriedEntity(); 
        if (isFakingDeath()) {
            super.die(source);
            return;
        }

        DamageAdaptationConfig config = DamageAdaptation.getEntityConfig(this);
        if (config != null) {
            int minKills = config.getMinimumKillCount();
            if (minKills > 0) {
                int currentDeaths = DamageAdaptation.getDeathCount(this);
                if (currentDeaths < minKills) {
                    DamageAdaptation.recordDeath(this);
                    this.setHealth(this.getMaxHealth());
                    return;
                }
            }
        }

        if (this.isOnFire()) {
            
            super.die(source);
            this.onDeath(source);
            return;
        }

        if (!this.level().isClientSide && this.getHealth() <= 0.0F && this.random.nextFloat() < 0.4f) {
            triggerFakeDeath(source);
            this.onDeath(source);
        } else {
            super.die(source);
            this.onDeath(source);
            if (!this.level().isClientSide) {
                if (this.random.nextFloat() < 0.3f) {
                    WalkingEndermanHead head = ModEntities.WALKING_ENDERMAN_HEAD.get().create(this.level());
                    if (head != null) {
                        head.setPos(this.getX(), this.getY(), this.getZ());
                        head.setYRot(this.random.nextFloat() * 360.0F);

                        InfestedEnderman.Variant foxVariant = this.getVariant();
                        WalkingEndermanHead.Variant headVariant = foxVariant == InfestedEnderman.Variant.UNSTABLE ?
                                WalkingEndermanHead.Variant.UNSTABLE : WalkingEndermanHead.Variant.DEFAULT;
                        head.setVariant(headVariant);
                        this.level().addFreshEntity(head);
                    }
                }
            }
        }
    }

    private void triggerFakeDeath(DamageSource source) {
        releaseCarriedEntity();
        setFakingDeath(true);
        setInvulnerable(true);
        fakeDeathTimer = 27;
        deathPosition = this.blockPosition();
        this.setHealth(0.02F);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
        this.entityData.set(DATA_IS_FAKING_DEATH, true);
    }

    
    private static void spawnBuglins(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            EntityType<?> buglinType = ModEntities.INFESTED_ENDERMITE.get();
            Entity buglin = buglinType.create(level);
            if (buglin != null) {
                double offsetX = random.nextDouble() - 0.5;
                double offsetY = random.nextDouble() * 0.5;
                double offsetZ = random.nextDouble() - 0.5;
                buglin.setPos(pos.getX() + 0.5 + offsetX, pos.getY() + offsetY, pos.getZ() + 0.5 + offsetZ);
                if (buglin instanceof LivingEntity) {
                    ((LivingEntity) buglin).setDeltaMovement(
                            (random.nextDouble() - 0.5) * 0.1,
                            random.nextDouble() * 0.1,
                            (random.nextDouble() - 0.5) * 0.1
                    );
                }
                level.addFreshEntity(buglin);
            }
        }
    }

    private static void spawnRemainsBlocksAt(ServerLevel level, BlockPos deathPos, RandomSource rand) {
        if (level.isClientSide || deathPos == null) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_LARGE.get().defaultBlockState(), 1);
        int mediumCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_MEDIUM.get().defaultBlockState(), mediumCount);
        int smallCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_SMALL.get().defaultBlockState(), smallCount);
    }

    private static void placeRemainsBlock(Level level, BlockPos deathPos, BlockPos.MutableBlockPos pos,
                                          RandomSource rand, BlockState state, int count) {
        for (int i = 0; i < count; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 5;
            double offsetZ = (rand.nextDouble() - 0.5) * 5;
            int offsetY = -rand.nextInt(3);
            pos.set(deathPos.getX() + offsetX, deathPos.getY() + 1 + offsetY, deathPos.getZ() + offsetZ);
            if (level.isEmptyBlock(pos)) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.isFaceSturdy(level, below, Direction.UP)) {
                    level.setBlock(pos, state, 3);
                }
            }
        }
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
    protected boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }

    public boolean isArayaMode() {
        return isArayaMode;
    }

    public boolean isDamageIncreased() {
        return damageIncreaseTimer > 0;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null) return;

        // ----- 剑痕残像增伤（除 Araya 自身外） -----
        int scar = victim.getPersistentData().getInt("SwordScar");
        if (scar > 0) {
            // 若受害者是 Araya 形态则跳过自身增伤（但自身增伤由另一个逻辑处理）
            if (!(victim instanceof InfestedEnderman && ((InfestedEnderman) victim).isArayaMode())) {
                int bonus = (scar / 10) * 10;
                if (bonus > 50) bonus = 50;
                event.setAmount(event.getAmount() * (1 + bonus / 100.0f));
            }
        }

        if (victim instanceof InfestedEnderman) {
            InfestedEnderman ender = (InfestedEnderman) victim;
            if (ender.isArayaMode() && ender.isDamageIncreased()) {
                event.setAmount(event.getAmount() * 1.2f);
            }
        }
    }

    public static boolean checkInfestedEndermanSpawnRules(
            EntityType<InfestedEnderman> entityType,
            ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            if (stage < 4 || stage > 7) return false;
        }
        return levelAccessor.getMaxLocalRawBrightness(pos) < 8;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    public ResourceLocation getTextureResource() {
        switch (getVariant()) {
            case UNSTABLE:
                return new ResourceLocation("epca", "textures/entity/infested_enderman_unstable.png");
            default:
                return new ResourceLocation("epca", "textures/entity/infested_enderman.png");
        }
    }
}