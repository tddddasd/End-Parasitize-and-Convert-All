package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;
import org.tdddd.epca.impl.client.entity.IMotionAligned;


import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.tdddd.epca.impl.overworld.registry.capability.LifetimeCapability;
import software.bernie.geckolib.animatable.GeoEntity;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingChickenHead;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BiomassEgg extends AbstractArrow implements GeoEntity, IMotionAligned {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age = 0;
    private final int MAX_AGE = 15 * 20; 

    public BiomassEgg(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(false); 
    }

    public BiomassEgg(Level level, LivingEntity shooter) {
        super(ModEntities.BIOMASS_EGG.get(), shooter, level); 
        this.setNoGravity(false); 
    }

    @Override
    public void tick() {
        super.tick();

        
        age++;
        if (age >= MAX_AGE && !this.level().isClientSide) {
            this.discard();
        }

        
        if (!this.inGround) {
            Vec3 deltaMovement = this.getDeltaMovement();
            double length = deltaMovement.length();
            if (length > 0.0D) {
                double horizontalLength = deltaMovement.horizontalDistance();
                this.setYRot((float)(Mth.atan2(deltaMovement.x, deltaMovement.z) * (180F / Math.PI)));
                this.setXRot((float)(Mth.atan2(deltaMovement.y, horizontalLength) * (180F / Math.PI)));

                
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.random.nextFloat() < 0.125f) { 
            this.spawnWalkingChickenHeads();
        }
        this.discard();
        super.onHitBlock(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) result.getEntity();
            
            if (!IParasite.isParasiteByTagOrInterface(target)) {
                target.hurt(target.damageSources().indirectMagic(this, this.getOwner()), 0.5f);
                target.addEffect(new MobEffectInstance(
                        ModEffects.COTH.get(),
                        300,
                        0,
                        false,
                        true
                ));
            }
        }
        if (this.random.nextFloat() < 0.125f) { 
            this.spawnWalkingChickenHeads();
        }
        this.discard();
    }

    private void setLifeTime(LivingEntity entity, int ticks) {
        entity.getCapability(LifetimeCapability.LIFETIME).ifPresent(cap -> cap.setRemainingTicks(ticks));
    }

    
    private void spawnWalkingChickenHeads() {
        if (!this.level().isClientSide) {
            
            int count = 1 + random.nextInt(2); 

            for (int i = 0; i < count; i++) {
                WalkingChickenHead walkingChickenHead = new WalkingChickenHead(ModEntities.WALKING_CHICKEN_HEAD.get(), this.level());
                walkingChickenHead.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                setLifeTime(walkingChickenHead, 1200);

                
                walkingChickenHead.addEffect(new MobEffectInstance(
                        ModEffects.RAGE.get(), 
                        1200,  
                        1       
                ));

                
                this.level().addFreshEntity(walkingChickenHead);
            }
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        
        return ItemStack.EMPTY;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<BiomassEgg> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
