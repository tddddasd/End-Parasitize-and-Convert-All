package org.tdddd.epca.impl.overworld.registry.entities.entity.special;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.stream.Collectors;

public class Nullthing extends PathfinderMob implements GeoEntity, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    
    private int runSoundCooldown = 0;
    private int standSoundCooldown = 0;
    private int standSoundDelay = 0;
    private int attackSoundCooldown = 0; 
    private boolean wasMoving = false;
    private boolean isAttacking = false;

    public Nullthing(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .build();
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        if (effectInstance.getEffect() == ModEffects.COTH.get()) {
            return false;
        }
        return super.canBeAffected(effectInstance);
    }

    
    @Override
    public boolean doHurtTarget(Entity target) {
        
        if (!(target instanceof Player)) {
            target.remove(RemovalReason.UNLOADED_TO_CHUNK); 
            return true; 
        }
        
        return super.doHurtTarget(target);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        
        this.goalSelector.addGoal(2, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public void start() {
                super.start();
                
                ((Nullthing) this.mob).setAttacking(true);
            }

            @Override
            public void stop() {
                super.stop();
                
                ((Nullthing) this.mob).setAttacking(false);
            }
        });
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::canAttack));
    }

    
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Nullthing) &&
                target != null &&
                target.isAlive();
    }

    
    @Override
    public void tick() {
        super.tick();
        
        if (this.getHealth() < this.getMaxHealth()) {
            super.setHealth(this.getMaxHealth());
        }

        
        if (this.getAttribute(Attributes.MAX_HEALTH).getBaseValue() != 800.0D) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(800.0D);
        }

        if (!this.level().isClientSide) {
            
            
            this.getActiveEffects().stream()
                    .map(MobEffectInstance::getEffect)
                    .filter(effect -> !isEpcaEffect(effect))
                    .collect(Collectors.toList()) 
                    .forEach(this::removeEffect);

            boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001D;

            
            if (this.isAttacking && this.getTarget() != null && this.distanceToSqr(this.getTarget()) < 8.0D) {
                if (attackSoundCooldown <= 0) {
                    if (this.random.nextBoolean()) {
                        this.playSound(ModSoundEvents.NULLTHING_ATTACK0.get(), 1.0F, 1.0F);
                    }
                    attackSoundCooldown = 10; 
                } else {
                    attackSoundCooldown--; 
                }
            } else {
                
                
            }

            
            if (isMoving) {
                if (runSoundCooldown <= 0) {
                    this.playSound(ModSoundEvents.NULLTHING_RUN.get(), 0.8F, 1.0F);
                    runSoundCooldown = 40; 
                } else {
                    runSoundCooldown--;
                }
                
                if (wasMoving != isMoving) {
                    standSoundCooldown = 0;
                    standSoundDelay = this.random.nextInt(120) + 120; 
                }
            }
            
            else {
                if (standSoundCooldown <= 0) {
                    if (standSoundDelay <= 0) {
                        if (this.random.nextBoolean()) {
                            this.playSound(ModSoundEvents.NULLTHING_STAND1.get(), 0.6F, 1.0F);
                        } else {
                            this.playSound(ModSoundEvents.NULLTHING_STAND2.get(), 0.6F, 1.0F);
                        }
                        standSoundDelay = this.random.nextInt(120); 
                    } else {
                        standSoundDelay--;
                    }
                    standSoundCooldown = 20; 
                } else {
                    standSoundCooldown--;
                }
            }

            wasMoving = isMoving;
        }
    }

    
    private boolean isEpcaEffect(MobEffect effect) {
        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return key != null && "epca".equals(key.getNamespace());
    }

    
    public void setAttacking(boolean attacking) {
        this.isAttacking = attacking;
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        return false;
    }

    
    @Override
    public void setHealth(float health) {
        
        super.setHealth(this.getMaxHealth());
    }

    
    @Override
    public void die(DamageSource damageSource) {
        
        
    }

    @Override
    public boolean isDeadOrDying() {
        
        return false;
    }

    @Override
    public boolean isAlive() {
        
        return true;
    }

    @Override
    public void kill() {
        
        
    }

    
    @Override
    public void remove(Entity.RemovalReason reason) {
        
        
    }

    
    @Override
    public void checkDespawn() {
        
        this.noActionTime = 0;
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<Nullthing> event) {
        boolean isMoving = event.isMoving();

        if (this.isAttacking && this.getTarget() != null && this.distanceToSqr(this.getTarget()) < 8.0D) {
            
            event.getController().setAnimation(RawAnimation.begin().thenLoop("attack"));
        } else if (isMoving) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("trot"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    public static boolean checkNullthingSpawnRules(
            EntityType<Nullthing> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND;
    }
}