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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

public class BoneArrow extends AbstractArrow implements GeoEntity, IMotionAligned {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age = 0;
    private final int MAX_AGE = 30 * 20; 
    private static final Random RANDOM = new Random(); 

    public BoneArrow(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.setBaseDamage(8.0F); 
    }

    public BoneArrow(Level level, LivingEntity shooter) {
        super(ModEntities.BONE_ARROW.get(), shooter, level);
        this.setNoGravity(false);
        this.setBaseDamage(8.0F); 
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
    protected void onHitEntity(EntityHitResult hitResult) {
        
        if (this.level().isClientSide) {
            return;
        }

        
        LivingEntity shooter = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;
        var target = hitResult.getEntity();

        
        if (IParasite.isParasiteByTagOrInterface(shooter) && IParasite.isParasiteNoLivingByTagOrInterface(target)) {
            
            return;
        }

        
        boolean damageApplied = target.hurt(this.damageSources().arrow(this, shooter), 8.0F);

        
        if (damageApplied && target instanceof LivingEntity livingTarget) {
            applyCothEffect(livingTarget);

            if (target instanceof LivingEntity living && this.getPersistentData().getBoolean("InfestedFireArrow")) {
                living.setRemainingFireTicks(160);  
            }
        }

        
        this.discard();
    }

    
    private void applyCothEffect(LivingEntity target) {
        var existingEffect = target.getEffect(ModEffects.COTH.get());

        if (existingEffect != null) {
            
            int currentAmplifier = existingEffect.getAmplifier(); 
            int newAmplifier = currentAmplifier;
            if (RANDOM.nextFloat() < 0.7f) {
                
                newAmplifier = Math.min(currentAmplifier + 1, 2);
            }
            
            target.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 15 * 20, newAmplifier));
        } else {
            
            target.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 15 * 20, 0));
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
        
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
