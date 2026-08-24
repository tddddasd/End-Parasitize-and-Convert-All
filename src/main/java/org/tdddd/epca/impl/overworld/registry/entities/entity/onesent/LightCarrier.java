package org.tdddd.epca.impl.overworld.registry.entities.entity.onesent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IOnesent;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LightCarrier extends PathfinderMob implements GeoEntity, IParasite, IOnesent, Enemy {
    
    public enum Variant {
        DEFAULT,
        BLEED,
        VIRAL
    }

    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private static final EntityDataAccessor<Boolean> DATA_IS_EXPLODING = SynchedEntityData.defineId(LightCarrier.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_BOOMING = SynchedEntityData.defineId(LightCarrier.class, EntityDataSerializers.BOOLEAN);

    
    private int explosionTimer = 20;
    private BlockPos deathPosition;

    
    private int boomAnimationTimer = 0;          
    private boolean isBoomTriggered = false;     

    
    private int clientBoomTimer = 0;

    private boolean variantOverridden = false;

    public LightCarrier(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 15;

        
        if (!level.isClientSide && !variantOverridden) {
            int roll = this.random.nextInt(100);
            if (roll < 66) {
                this.setVariant(LightCarrier.Variant.DEFAULT);
            } else if (roll < 83) {
                this.setVariant(LightCarrier.Variant.BLEED);
            } else {
                this.setVariant(LightCarrier.Variant.VIRAL);
            }
        }
    }

    @Override
    public CompoundTag getVariantData() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Variant", this.getVariant().name());
        return tag;
    }

    @Override
    public void setVariantData(CompoundTag data) {
        if (data.contains("Variant", 8)) {
            String variantName = data.getString("Variant");
            try {
                this.setVariant(LightCarrier.Variant.valueOf(variantName));
                this.variantOverridden = true; 
            } catch (IllegalArgumentException ignored) {}
        }
    }

    
    public LightCarrier.Variant getVariant() {
        
        Integer variantOrdinal = this.entityData.get(DATA_VARIANT);
        if (variantOrdinal == null) {
            
            return LightCarrier.Variant.DEFAULT;
        }

        
        int index = Mth.clamp(variantOrdinal, 0, LightCarrier.Variant.values().length - 1);
        return LightCarrier.Variant.values()[index];
    }

    public void setVariant(LightCarrier.Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant", 8)) {
            String variantName = tag.getString("Variant");
            try {
                this.setVariant(LightCarrier.Variant.valueOf(variantName));
            } catch (IllegalArgumentException e) {
                
                this.setVariant(LightCarrier.Variant.DEFAULT);
            }
        }
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));

        
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0.005F));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 32.0D));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, LightCarrier.Variant.DEFAULT.ordinal());
        this.entityData.define(DATA_IS_EXPLODING, false);
        this.entityData.define(DATA_IS_BOOMING, false);
    }

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
            LightCarrier.class, EntityDataSerializers.INT
    );

    
    public boolean isExploding() {
        return this.entityData.get(DATA_IS_EXPLODING);
    }

    private void setExploding(boolean exploding) {
        this.entityData.set(DATA_IS_EXPLODING, exploding);
    }

    private void triggerExplosionDeath(DamageSource source) {
        setExploding(true);
        explosionTimer = 20;
        deathPosition = this.blockPosition();
        this.setHealth(0.02F);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.entityData.set(DATA_IS_BOOMING, false); 
    }

    private void executeExplosionEffects() {
        if (this.level() instanceof ServerLevel serverLevel) {
            
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.BIG_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);

            
            AABB explosionArea = this.getBoundingBox().inflate(4.0);
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, explosionArea)) {
                if (entity != null && entity.isAlive() && entity != this && !IParasite.isParasiteByTagOrInterface(entity)) {
                    double distance = this.distanceTo(entity);
                    if (distance <= 4.0) {
                        float damage = 28.0F * (float)(1.0 - distance / 4.0);
                        entity.hurt(this.damageSources().explosion(this, null), damage);

                        double dx = entity.getX() - this.getX();
                        double dz = entity.getZ() - this.getZ();
                        double magnitude = Math.sqrt(dx * dx + dz * dz);
                        if (magnitude > 0) {
                            dx /= magnitude;
                            dz /= magnitude;
                            double knockbackStrength = (1.0 - distance / 4.0) * 2.0;
                            entity.setDeltaMovement(
                                    entity.getDeltaMovement().add(dx * knockbackStrength, 0.3, dz * knockbackStrength)
                            );
                        }
                    }
                }
            }

            
            int rupterCount = 2 + this.random.nextInt(2);
            for (int i = 0; i < rupterCount; i++) {
                EntityType<?> rupterType = ModEntities.RIPPER.get();
                Entity rupter = rupterType.create(this.level());
                if (rupter != null) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
                    rupter.setPos(
                            this.getX() + offsetX,
                            this.getY(),
                            this.getZ() + offsetZ
                    );
                    this.level().addFreshEntity(rupter);
                }
            }

            
            int buglinCount = 1 + this.random.nextInt(2);
            for (int i = 0; i < buglinCount; i++) {
                EntityType<?> buglinType = ModEntities.CURBUG.get();
                Entity buglin = buglinType.create(this.level());
                if (buglin != null) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
                    buglin.setPos(
                            this.getX() + offsetX,
                            this.getY(),
                            this.getZ() + offsetZ
                    );
                    this.level().addFreshEntity(buglin);
                }
            }

            
            BlockPos center = this.blockPosition();
            BlockConversionManager manager = BlockConversionManager.getInstance();

            
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        double distSqr = center.distSqr(pos);
                        
                        if (distSqr > 9.0) continue;

                        BlockState state = serverLevel.getBlockState(pos);
                        if (!manager.isExposed(serverLevel, pos)) continue; 

                        
                        if (distSqr <= 4.0) {
                            
                            if (this.random.nextFloat() < 0.8f) {
                                manager.convertBlockUsingGeneralConfig(serverLevel, pos, state);
                                manager.convertPlantsInRangeForGeneral(serverLevel, pos);
                            }
                        } else {
                            
                            if (this.random.nextFloat() < 0.15f) {
                                manager.convertBlockUsingGeneralConfig(serverLevel, pos, state);
                                manager.convertPlantsInRangeForGeneral(serverLevel, pos);
                            }
                        }
                    }
                }
            }

            
            Variant variant = getVariant();
            if (variant == Variant.BLEED) {
                
                int gnatCount = 1 + this.random.nextInt(2); 
                for (int i = 0; i < gnatCount; i++) {
                    EntityType<?> gnatType = ModEntities.MOZZIE.get();
                    if (gnatType != null) {
                        Entity gnat = gnatType.create(this.level());
                        if (gnat != null) {
                            double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                            double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
                            gnat.setPos(
                                    this.getX() + offsetX,
                                    this.getY(),
                                    this.getZ() + offsetZ
                            );
                            this.level().addFreshEntity(gnat);
                        }
                    }
                }
            } else if (variant == Variant.VIRAL) {
                
                AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
                cloud.setOwner(this);
                cloud.setRadius(1.5f);        
                cloud.setRadiusPerTick(0.0f);  
                cloud.setWaitTime(0);           
                cloud.setDuration(120);          
                
                cloud.addEffect(new MobEffectInstance(ModEffects.VIRAL.get(), 300, 1)); 
                cloud.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, 0));   
                this.level().addFreshEntity(cloud);
            }
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.RIPPER_HUNT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.RIPPER_HUNT.get();
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)) {
            return false;
        }

        if (isExploding()) {
            return false;
        }

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (shouldIgnoreDamageFrom(attacker)) {
                return false;
            }
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        float adjustedAmount = ((IParasite) this).onHurt(source, amount);
        return super.hurt(source, adjustedAmount);
    }

    
    @Override
    public void die(DamageSource source) {
        if (this.isOnFire()) {
            
            super.die(source);
            this.onDeath(source);
            return;
        }

        
        if (!this.level().isClientSide && !this.isExploding()) {
            triggerExplosionDeath(source);
            this.onDeath(source);
        } else {
            super.die(source);
            this.onDeath(source);
        }
    }

    
    @Override
    protected boolean isAffectedByFluids() {
        return false;
    }

    
    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide) {
            
            if (this.entityData.get(DATA_IS_BOOMING)) {
                if (clientBoomTimer <= 0) {
                    clientBoomTimer = 20;
                }
                if (clientBoomTimer > 0) {
                    clientBoomTimer--;
                }
            } else {
                clientBoomTimer = 0;
            }
        }

        
        if (isExploding()) {
            explosionTimer--;
            if (explosionTimer <= 0) {
                if (!this.level().isClientSide) {
                    executeExplosionEffects();
                }
                this.discard();
                return;
            }
            return;
        }

        
        if (boomAnimationTimer > 0) {
            boomAnimationTimer--;
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && !this.level().isClientSide) {
                this.getNavigation().moveTo(target, 1.2);
            }
            if (boomAnimationTimer <= 0) {
                this.entityData.set(DATA_IS_BOOMING, false); 
                if (!this.level().isClientSide) {
                    triggerExplosionDeath(null);
                }
            }
        }

        
        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();

            
            if (target == null || !target.isAlive()) {
                if (boomAnimationTimer == 0 && !isExploding()) {
                    isBoomTriggered = false;
                    this.entityData.set(DATA_IS_BOOMING, false);
                }
                
            }

            
            if (target != null && target.isAlive() && !isBoomTriggered && boomAnimationTimer == 0) {
                double distance = this.distanceTo(target);
                boolean healthLow = this.getHealth() / this.getMaxHealth() < 0.25F;
                boolean tooClose = distance <= 5.0;
                if (healthLow || tooClose) {
                    isBoomTriggered = true;
                    boomAnimationTimer = 20; 
                    this.entityData.set(DATA_IS_BOOMING, true);
                    this.getNavigation().moveTo(target, 1.2);
                }
            }
        }
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LightCarrier>(this, "controller", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<LightCarrier> event) {
        AnimationController<LightCarrier> controller = event.getController();
        LightCarrier entity = event.getAnimatable();

        if (entity.isExploding()) {
            controller.setAnimation(RawAnimation.begin().thenLoop("boom"));
        } else if (this.isMoving()) {
            controller.setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else {
            controller.setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
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
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);
        }
    }

    
    public static boolean checkLightCarrierSpawnRules(
            EntityType<LightCarrier> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getMaxLocalRawBrightness(pos) < 0;
    }

    public ResourceLocation getTextureResource() {
        switch (getVariant()) {
            case BLEED:
                return new ResourceLocation("epca", "textures/entity/light_carrier_blood.png");
            case VIRAL:
                return new ResourceLocation("epca", "textures/entity/light_carrier_viral.png");
            default:
                return new ResourceLocation("epca", "textures/entity/light_carrier.png");
        }
    }
}