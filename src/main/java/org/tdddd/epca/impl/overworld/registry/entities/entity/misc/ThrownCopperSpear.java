package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModItems;

public class ThrownCopperSpear extends AbstractArrow {
    private static final float CUSTOM_GRAVITY = 0.04f; 
    private ItemStack spearItem;
    private boolean dealtDamage = false;
    private int returnTimer = 0;
    private final boolean wasCreativeThrower; 

    public ThrownCopperSpear(EntityType<? extends ThrownCopperSpear> type, Level level) {
        super(type, level);
        this.spearItem = new ItemStack(ModItems.COPPER_SPEAR.get());
        this.setNoGravity(true); 
        this.wasCreativeThrower = false;   
    }

    public ThrownCopperSpear(Level level, LivingEntity shooter, ItemStack stack, boolean isCreative) {
        super(ModEntities.THROWN_COPPER_SPEAR.get(), shooter, level);
        this.spearItem = stack.copy();
        this.setNoGravity(true);
        this.wasCreativeThrower = isCreative;
        
        int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, spearItem);
        if (fireAspect > 0) {
            this.setSecondsOnFire(100);
        }
    }

    
    @Override
    public void tick() {
        
        if (this.returnTimer > 0) {
            Entity owner = this.getOwner();
            if (owner instanceof Player player && !this.level().isClientSide) {
                    
                    if (!this.wasCreativeThrower) {
                        if (!player.getInventory().add(this.spearItem)) {
                            player.drop(this.spearItem, false);
                        } else {
                            
                            this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
                        }
                    }
                    this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 1.0F);
                    this.discard();
                    return;
            } else {
                this.discard();
            }
            return; 
        }
        super.tick();
        
        if (!this.isNoPhysics() && !this.inGround && this.returnTimer == 0) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, motion.y - CUSTOM_GRAVITY, motion.z);
        }

        
        Vec3 motion = this.getDeltaMovement();
        if (!(motion.x == 0 && motion.z == 0 && motion.y == 0) && !this.inGround && this.returnTimer == 0) {
            float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0 / Math.PI);
            float pitch = (float) (Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * 180.0 / Math.PI);
            this.setYRot(yaw);
            this.setXRot(pitch);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.returnTimer > 0) return; 
        Entity target = result.getEntity();
        Entity shooter = this.getOwner();
        float damage = 6.0f;

        
        int sharpness = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, spearItem);
        int smite = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, spearItem);
        int bane = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, spearItem);
        if (sharpness > 0) damage += sharpness * 0.5f;
        if (smite > 0 && (target instanceof Phantom || target instanceof Skeleton || target instanceof Zombie))
            damage += smite * 2.5f;
        if (bane > 0 && (target instanceof Animal))
            damage += bane * 2.5f;

        DamageSource damagesource = shooter == null ? damageSources().thrown(this, this) : damageSources().thrown(this, shooter);
        if (target.hurt(damagesource, damage)) {
            if (target instanceof LivingEntity livingTarget) {
                int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, spearItem);
                if (fireAspect > 0) livingTarget.setSecondsOnFire(100);
                int knockback = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, spearItem);
                if (knockback > 0) {
                    Vec3 vec3 = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6);
                    if (vec3.lengthSqr() > 0.0) {
                        livingTarget.push(vec3.x, 0.1, vec3.z);
                    }
                }
            }
            this.dealtDamage = true;

            
            int loyalty = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, spearItem);
            if (loyalty > 0) {
                
                this.returnTimer = 1;
                this.setNoPhysics(true);  
                this.setNoGravity(true);  
                this.setDeltaMovement(Vec3.ZERO);
                
                this.inGround = false;
                
            } else {
                
                this.setDeltaMovement(Vec3.ZERO);
                this.setNoPhysics(false);
                this.inGround = false;
                this.pickup = Pickup.ALLOWED;
                this.setNoGravity(false);
                this.setPierceLevel((byte)0);
            }
        }
        
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.returnTimer > 0) return; 
        super.onHitBlock(result); 
        
        if (!wasCreativeThrower && !spearItem.isEmpty()) {
            Entity shooter = this.getOwner();
            if (shooter instanceof Player player) {
                spearItem.hurtAndBreak(2, player, (p) -> p.broadcastBreakEvent(p.getUsedItemHand()));
            } else {
                spearItem.setDamageValue(spearItem.getDamageValue() + 2);
            }
            if (spearItem.getDamageValue() >= spearItem.getMaxDamage()) {
                this.discard();
                return;
            }
        }
        
        int loyalty = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, spearItem);
        if (loyalty > 0 && !this.dealtDamage) {
            this.returnTimer = 1;
            
            this.setNoPhysics(true);  
            this.setNoGravity(true);  
            this.setDeltaMovement(Vec3.ZERO);
            this.inGround = false;
        } else {
            
            
            this.pickup = Pickup.ALLOWED;
        }
    }

    @Override
    public ItemStack getPickupItem() {
        return this.spearItem.copy();
    }

    @Override
    public void playerTouch(Player player) {
        if (this.returnTimer > 0) return;
        
        if (!this.level().isClientSide && this.pickup == Pickup.ALLOWED) {
            if (this.wasCreativeThrower) return;
            if (player.getInventory().add(this.spearItem)) {
                this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
                this.discard();
            }
        }
    }

    
    @Override
    public boolean isNoPhysics() {
        return this.returnTimer > 0;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return this.returnTimer == 0 && super.canHitEntity(entity);
    }
}