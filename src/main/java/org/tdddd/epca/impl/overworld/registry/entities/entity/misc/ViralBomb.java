package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.utils.ParasiteHelper;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.lang.reflect.Field;
import java.util.List;

public class ViralBomb extends Entity implements GeoEntity, IParasite {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> DUMMY = SynchedEntityData.defineId(ViralBomb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TICK_COUNT = SynchedEntityData.defineId(ViralBomb.class, EntityDataSerializers.INT);

    
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation BOOM_ANIMATION = RawAnimation.begin().thenPlay("boom");

    
    private static final int SPAWN_ANIMATION_DURATION = 10; 
    private static final int BOOM_ANIMATION_DURATION = 20; 
    private static final int TOTAL_LIFETIME = 30; 

    
    private static final int EFFECT_UPGRADE_INTERVAL = 40; 

    
    private static final float EXPLOSION_DAMAGE = 2.5f;
    private static final float EXPLOSION_RADIUS = 1.5f; 

    public ViralBomb(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DUMMY, false);
        this.entityData.define(TICK_COUNT, 0);
    }

    @Override
    public void tick() {
        super.tick();

        
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.2, 0.0));
        }

        
        this.move(MoverType.SELF, this.getDeltaMovement());

        
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO); 
        }

        
        int currentTick = this.entityData.get(TICK_COUNT);
        this.entityData.set(TICK_COUNT, currentTick + 1);

        
        if (!this.level().isClientSide && currentTick >= TOTAL_LIFETIME) {
            
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F);

            
            this.explode();

            this.createEffectCloud();
            this.discard();
        }
    }

    
    private void explode() {
        
        AABB explosionArea = new AABB(
                this.getX() - EXPLOSION_RADIUS,
                this.getY() - EXPLOSION_RADIUS,
                this.getZ() - EXPLOSION_RADIUS,
                this.getX() + EXPLOSION_RADIUS,
                this.getY() + EXPLOSION_RADIUS,
                this.getZ() + EXPLOSION_RADIUS
        );

        
        List<LivingEntity> entities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                explosionArea
        );

        
        for (LivingEntity entity : entities) {
            
            if (shouldSkipDamage(entity)) {
                continue;
            }

            
            DamageSource damageSource = this.damageSources().explosion(this, this);

            
            entity.hurt(damageSource, EXPLOSION_DAMAGE);
        }
    }

    
    private boolean shouldSkipDamage(LivingEntity entity) {
        
        if (ParasiteHelper.isParasite(entity)) {
            return true;
        }

        
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            
            if (player.isCreative() || player.isSpectator()) {
                return true;
            }
        }

        return false;
    }

    private void createEffectCloud() {
        
        CustomEffectCloud customCloud = new CustomEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        customCloud.setRadius(EXPLOSION_RADIUS); 
        customCloud.setRadiusOnUse(-0.5f); 
        customCloud.setWaitTime(0); 
        customCloud.setDuration(100); 

        
        customCloud.setRadiusPerTick(-customCloud.getRadius() / (float)customCloud.getDuration()); 

        
        customCloud.addEffect(new MobEffectInstance(
                net.minecraft.world.effect.MobEffects.POISON, 
                300, 
                0, 
                false, 
                true 
        ));

        customCloud.addEffect(new MobEffectInstance(
                ModEffects.VIRAL.get(), 
                3600, 
                0, 
                false,
                true
        ));

        customCloud.addEffect(new MobEffectInstance(
                ModEffects.COTH.get(), 
                3600, 
                0, 
                false,
                true
        ));

        this.level().addFreshEntity(customCloud);
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ViralBomb>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<ViralBomb> event) {
        AnimationController<ViralBomb> controller = event.getController();
        int currentTick = this.entityData.get(TICK_COUNT);

        if (currentTick < SPAWN_ANIMATION_DURATION) {
            controller.setAnimation(SPAWN_ANIMATION);
        } else if (currentTick < SPAWN_ANIMATION_DURATION + BOOM_ANIMATION_DURATION) {
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
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains("TickCount")) {
            this.entityData.set(TICK_COUNT, compoundTag.getInt("TickCount"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("TickCount", this.entityData.get(TICK_COUNT));
    }

    
    @Override
    public boolean isNoGravity() {
        return false; 
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    
    public static class CustomEffectCloud extends AreaEffectCloud {
        
        private static Field effectsField;
        private static boolean reflectionInitialized = false;

        static {
            try {
                effectsField = AreaEffectCloud.class.getDeclaredField("effects");
                effectsField.setAccessible(true);
                reflectionInitialized = true;
            } catch (NoSuchFieldException e) {
                System.err.println("Failed to find 'effects' field in AreaEffectCloud: " + e.getMessage());
                reflectionInitialized = false;
            }
        }

        public CustomEffectCloud(Level level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @Override
        public void tick() {
            super.tick();

            
            if (!reflectionInitialized) {
                return;
            }

            
            if (this.tickCount % EFFECT_UPGRADE_INTERVAL == 0 && this.tickCount > 0) {
                try {
                    @SuppressWarnings("unchecked")
                    List<MobEffectInstance> effects = (List<MobEffectInstance>) effectsField.get(this);

                    
                    for (int i = 0; i < effects.size(); i++) {
                        MobEffectInstance effect = effects.get(i);

                        
                        if (effect.getEffect() == ModEffects.COTH.get() && effect.getAmplifier() < 2) {
                            
                            effects.set(i, new MobEffectInstance(
                                    effect.getEffect(),
                                    effect.getDuration(),
                                    effect.getAmplifier() + 1,
                                    effect.isAmbient(),
                                    effect.isVisible(),
                                    effect.showIcon()
                            ));
                        }
                        
                        else if (effect.getEffect() != ModEffects.COTH.get()) {
                            effects.set(i, new MobEffectInstance(
                                    effect.getEffect(),
                                    effect.getDuration(),
                                    effect.getAmplifier() + 1,
                                    effect.isAmbient(),
                                    effect.isVisible(),
                                    effect.showIcon()
                            ));
                        }
                    }
                } catch (IllegalAccessException e) {
                    System.err.println("Failed to access 'effects' field in AreaEffectCloud: " + e.getMessage());
                    reflectionInitialized = false; 
                }
            }
        }
    }
}