package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSlimeSize3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class SlimeProjectile extends ThrowableProjectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private LivingEntity target;           
    private double correctionStrength = 0.08; 
    private double maxCorrectionAngle = 0.15; 
    private boolean homingEnabled = true;     

    public SlimeProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(false); 
    }

    public SlimeProjectile(EntityType<? extends ThrowableProjectile> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
        this.setNoGravity(false);
    }

    public SlimeProjectile(EntityType<? extends ThrowableProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
        this.setNoGravity(false);
    }

    
    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    private UUID ownerUuid;

    @Override
    protected void defineSynchedData() {
        
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (this.getOwner() != null) {
            this.ownerUuid = this.getOwner().getUUID();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwner() != null) {
            tag.putUUID("OwnerUUID", this.getOwner().getUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUuid = tag.getUUID("OwnerUUID");
            
            if (this.level() instanceof ServerLevel serverLevel) {
                Entity owner = serverLevel.getEntity(this.ownerUuid);
                if (owner instanceof LivingEntity) {
                    this.setOwner(owner);
                }
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        
        this.discard();
        this.playSound(SoundEvents.SLIME_BLOCK_FALL, 0.8F, 1.0F);
        spawnDisappearParticles();
        super.onHitBlock(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        
        if (this.level().isClientSide) {
            return;
        }

        
        var target = result.getEntity();

        
        if (IParasite.isParasiteNoLivingByTagOrInterface(target)) {
            
            return;
        }

        
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            LivingEntity realOwner = null;
            if (this.getOwner() instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
                realOwner = livingOwner;
            } else if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
                Entity e = serverLevel.getEntity(this.ownerUuid);
                if (e instanceof LivingEntity owner) realOwner = owner;
            }
            
            if (realOwner instanceof InfestedSlimeSize3) {
                
                MobEffectInstance solidifyEffect = new MobEffectInstance(
                        ModEffects.SOLIDIFY.get(),
                        30, 
                        0,  
                        false, 
                        true   
                );
                MobEffectInstance slownessEffect = new MobEffectInstance(
                        ModEffects.FEAR.get(),
                        300, 
                        0,  
                        false, 
                        true   
                );
                MobEffectInstance cothEffect = new MobEffectInstance(
                        ModEffects.COTH.get(),
                        600, 
                        0,  
                        false, 
                        true   
                );
                livingEntity.addEffect(solidifyEffect);
                livingEntity.addEffect(slownessEffect);
                livingEntity.addEffect(cothEffect);
            } else if (realOwner == null) {
                
                MobEffectInstance solidifyEffect = new MobEffectInstance(
                        ModEffects.SOLIDIFY.get(),
                        30, 
                        0,  
                        false, 
                        true   
                );
                MobEffectInstance cothEffect = new MobEffectInstance(
                        ModEffects.COTH.get(),
                        600, 
                        0,  
                        false, 
                        true   
                );
                livingEntity.addEffect(solidifyEffect);
                livingEntity.addEffect(cothEffect);
            }
        }

        this.discard();
        this.playSound(SoundEvents.SLIME_BLOCK_FALL, 0.8F, 1.0F);
        spawnDisappearParticles();
        super.onHitEntity(result);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;
        if (!homingEnabled || target == null || !target.isAlive()) return;

        
        Vec3 currentVelocity = this.getDeltaMovement();
        if (currentVelocity.lengthSqr() < 0.001) return;

        
        Vec3 toTarget = target.position()
                .add(0, target.getBbHeight() * 0.5, 0)
                .subtract(this.position());
        double dist = toTarget.length();
        if (dist < 0.5) return; 

        Vec3 targetDirection = toTarget.normalize();

        
        Vec3 currentDirection = currentVelocity.normalize();

        
        double dot = currentDirection.dot(targetDirection);
        dot = Math.max(-1.0, Math.min(1.0, dot)); 
        double angle = Math.acos(dot);

        
        if (angle < 0.01) return;

        Vec3 targetDir = toTarget.normalize();
        Vec3 currentDir = currentVelocity.normalize();

        Vec3 newDir = currentDir.lerp(targetDir, correctionStrength).normalize();
        double speed = currentVelocity.length();
        this.setDeltaMovement(newDir.scale(speed));

        
        if (this.isInWater()) {
            this.discard();
            this.playSound(SoundEvents.SLIME_BLOCK_FALL, 0.8F, 1.0F);
            spawnDisappearParticles();
            return;
        }

        
        if (this.level().isClientSide) {
            this.level().addParticle(
                    new ItemParticleOption(ParticleTypes.ITEM, ModItems.INFESTED_SLIME_BALL.get().getDefaultInstance()),
                    this.getX(), this.getY(), this.getZ(),
                    0.0D, 0.0D, 0.0D
            );
        }

        
        if (this.tickCount > 200) {
            this.discard();
            this.playSound(SoundEvents.SLIME_BLOCK_FALL, 0.8F, 1.0F);
            spawnDisappearParticles();
        }
    }

    private void spawnDisappearParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; ++i) {
                double d0 = this.getX() + (this.random.nextDouble() - 0.5) * 0.5;
                double d1 = this.getY() + this.random.nextDouble() * 0.5;
                double d2 = this.getZ() + (this.random.nextDouble() - 0.5) * 0.5;
                serverLevel.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, ModItems.INFESTED_SLIME_BALL.get().getDefaultInstance()),
                        d0, d1, d2, 1, 0.0D, 0.0D, 0.0D, 0.0D
                );
            }
        }
    }

    @Override
    public boolean isNoGravity() {
        return false; 
    }

    @Override
    public boolean isAttackable() {
        return false; 
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<SlimeProjectile> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}