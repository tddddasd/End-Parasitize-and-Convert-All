package org.tdddd.epca.impl.overworld.registry.entities.entity.poverty;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.registry.capability.LifetimeCapability;
import org.tdddd.epca.impl.overworld.data.BiomassSpawnConfig;
import org.tdddd.epca.impl.overworld.data.BiomassSpawnManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class BiomassSmall extends PathfinderMob implements GeoEntity, IParasite, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private static final EntityDataAccessor<Integer> TICK_COUNT = SynchedEntityData.defineId(BiomassSmall.class, EntityDataSerializers.INT);

    
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation BOOM_ANIMATION = RawAnimation.begin().thenPlay("boom");

    
    private static final int SPAWN_ANIMATION_DURATION = 15; 
    private static final int BOOM_ANIMATION_DURATION = 15; 
    private static final int TOTAL_LIFETIME = 30; 

    
    private static final int PARTICLE_INTERVAL = 4; 
    private static final float PARTICLE_SPEED = 0.02F; 
    private static final double MAX_ANGLE_RADIANS = Math.toRadians(35); 
    private static final double SPAWN_HEIGHT_OFFSET = 1.0; 

    public BiomassSmall(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICK_COUNT, 0);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .build();
    }

    @Override
    public void tick() {
        super.tick();

        this.fallDistance = 0.0F;
        
        int currentTick = this.entityData.get(TICK_COUNT);
        this.entityData.set(TICK_COUNT, currentTick + 1);

        
        if (!this.level().isClientSide) {
            
            if (currentTick >= TOTAL_LIFETIME) {
                this.explodeAndTransform();
                return;
            }

            
            
            if (this.tickCount % 20 == 0) {
                this.heal(1.0F);
            }

            
            if (this.tickCount % 10 == 0) {
                
                List<LivingEntity> entities = this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(1.5),
                        e -> e != this
                );

                
                for (LivingEntity entity : entities) {
                    entity.addEffect(new MobEffectInstance(
                            ModEffects.COTH.get(),
                            3600,  
                            1       
                    ));
                }
            }
        } else {
            
            spawnParticles(currentTick);
        }
    }

    
    private void spawnParticles(int currentTick) {
        
        if (currentTick == TOTAL_LIFETIME - 2) {
            
            int particleCount = 3 + this.random.nextInt(3); 

            for (int i = 0; i < particleCount; i++) {
                
                
                AABB boundingBox = this.getBoundingBox();
                double minX = boundingBox.minX;
                double maxX = boundingBox.maxX;
                double minY = boundingBox.minY;
                double maxY = boundingBox.maxY;
                double minZ = boundingBox.minZ;
                double maxZ = boundingBox.maxZ;

                
                double randomX = minX + this.random.nextDouble() * (maxX - minX);
                double randomY = minY + this.random.nextDouble() * (maxY - minY);
                double randomZ = minZ + this.random.nextDouble() * (maxZ - minZ);

                
                double offsetX = (this.random.nextDouble() - 0.5) * 0.12;
                double offsetY = (this.random.nextDouble() - 0.25) * 0.12;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.12;

                this.level().addParticle(
                        ModParticles.BIOMASS_BOOM_SMALL.get(),
                        randomX,
                        randomY,
                        randomZ,
                        offsetX,
                        offsetY,
                        offsetZ
                );
            }
        }

        if (currentTick == TOTAL_LIFETIME - 1) {
            
            int particleCount = 1 + this.random.nextInt(2); 

            for (int i = 0; i < particleCount; i++) {
                
                
                AABB boundingBox = this.getBoundingBox();
                double minX = boundingBox.minX;
                double maxX = boundingBox.maxX;
                double minY = boundingBox.minY;
                double maxY = boundingBox.maxY;
                double minZ = boundingBox.minZ;
                double maxZ = boundingBox.maxZ;

                
                double randomX = minX + this.random.nextDouble() * (maxX - minX);
                double randomY = minY + this.random.nextDouble() * (maxY - minY);
                double randomZ = minZ + this.random.nextDouble() * (maxZ - minZ);

                
                double offsetX = (this.random.nextDouble() - 0.5) * 0.1;
                double offsetY = (this.random.nextDouble() - 0.25) * 0.1;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.1;

                this.level().addParticle(
                        ModParticles.BIOMASS_BOOM_SMALL.get(),
                        randomX,
                        randomY,
                        randomZ,
                        offsetX,
                        offsetY,
                        offsetZ
                );
            }
        }

        
        if (currentTick % PARTICLE_INTERVAL == 0) {
            
            int particleCount = 1 + this.random.nextInt(2);

            for (int i = 0; i < particleCount; i++) {
                
                double angleYaw = this.random.nextDouble() * 2 * Math.PI; 
                double anglePitch = this.random.nextDouble() * MAX_ANGLE_RADIANS; 

                
                double speedX = Math.sin(angleYaw) * Math.cos(anglePitch) * PARTICLE_SPEED;
                double speedY = Math.sin(anglePitch) * PARTICLE_SPEED;
                double speedZ = Math.cos(angleYaw) * Math.cos(anglePitch) * PARTICLE_SPEED;

                
                double spawnX = this.getX();
                double spawnY = this.getY() + this.getBbHeight() * SPAWN_HEIGHT_OFFSET;
                double spawnZ = this.getZ();

                
                double offsetX = (this.random.nextDouble() - 0.5) * this.getBbWidth() * 0.15;
                double offsetZ = (this.random.nextDouble() - 0.5) * this.getBbWidth() * 0.15;

                
                this.level().addParticle(
                        ModParticles.BIOMASS.get(), 
                        spawnX + offsetX,
                        spawnY,
                        spawnZ + offsetZ,
                        speedX,
                        speedY,
                        speedZ
                );
            }
        }
    }

    
    private void setLifeTime(LivingEntity entity, int ticks) {
        entity.getCapability(LifetimeCapability.LIFETIME).ifPresent(cap -> cap.setRemainingTicks(ticks));
    }

    
    private BiomassSpawnConfig.SpawnEntry selectEntryByWeight(List<BiomassSpawnConfig.SpawnEntry> entries) {
        int totalWeight = entries.stream().mapToInt(BiomassSpawnConfig.SpawnEntry::getWeight).sum();
        if (totalWeight <= 0) return null;
        int r = this.random.nextInt(totalWeight);
        int cumulative = 0;
        for (BiomassSpawnConfig.SpawnEntry entry : entries) {
            cumulative += entry.getWeight();
            if (r < cumulative) return entry;
        }
        return entries.get(entries.size() - 1);
    }

    
    private void explodeAndTransform() {
        if (this.level().isClientSide) return;

        BiomassSpawnConfig config = BiomassSpawnManager.getConfig(this.getType());
        if (config == null) {
            this.discard();
            return;
        }

        List<BiomassSpawnConfig.SpawnEntry> entries = this.isInWater() ?
                config.getWaterSpawns() : config.getLandSpawns();

        if (entries == null || entries.isEmpty()) {
            this.discard();
            return;
        }

        BiomassSpawnConfig.SpawnEntry selected = selectEntryByWeight(entries);
        if (selected == null) {
            this.discard();
            return;
        }

        
        int count = this.random.nextInt(selected.getMinCount(), selected.getMaxCount() + 1);
        count = Math.min(count, 6);

        ResourceLocation entityId = new ResourceLocation(selected.getEntity());
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null) {
            this.discard();
            return;
        }

        for (int i = 0; i < count; i++) {
            Entity entity = type.create(this.level());
            if (!(entity instanceof LivingEntity living)) {
                if (entity != null) entity.discard();
                continue;
            }

            living.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());

            
            setLifeTime(living, selected.getLifeTime());

            
            if (selected.getEffects() != null) {
                for (BiomassSpawnConfig.EffectEntry effectEntry : selected.getEffects()) {
                    var effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectEntry.getEffect()));
                    if (effect != null) {
                        living.addEffect(new MobEffectInstance(
                                effect,
                                effectEntry.getDuration(),
                                effectEntry.getAmplifier(),
                                effectEntry.isAmbient(),
                                effectEntry.isVisible(),
                                effectEntry.isIcon()
                        ));
                    }
                }
            }

            this.level().addFreshEntity(living);
        }

        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (source.getEntity() instanceof LivingEntity attacker) {
            
            if (shouldIgnoreDamageFrom(attacker)) {
                return false; 
            }
        }

        
        float adjustedAmount = ((IParasite) this).onHurt(source, amount);

        
        boolean result = super.hurt(source, adjustedAmount);

        return result;
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<BiomassSmall> event) {
        AnimationController<BiomassSmall> controller = event.getController();
        int currentTick = this.entityData.get(TICK_COUNT);

        if (currentTick < SPAWN_ANIMATION_DURATION) {
            controller.setAnimation(SPAWN_ANIMATION);
        } else if (currentTick < TOTAL_LIFETIME) {
            controller.setAnimation(BOOM_ANIMATION);
        } else {
            
            return PlayState.STOP;
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    
    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains("TickCount")) {
            this.entityData.set(TICK_COUNT, compoundTag.getInt("TickCount"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("TickCount", this.entityData.get(TICK_COUNT));
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

    
    public static boolean checkBiomassSmallSpawnRules(
            EntityType<BiomassSmall> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        return level.getMaxLocalRawBrightness(pos) < 0;
    }
}