package org.tdddd.epca.impl.overworld.registry.entities.entity.special;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.UUID;

public class YawningNya extends PathfinderMob implements IParasite {
    private static final EntityDataAccessor<Boolean> DATA_HAS_REVENGE =
            SynchedEntityData.defineId(YawningNya.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_SHOWN_JOIN_MESSAGE =
            SynchedEntityData.defineId(YawningNya.class, EntityDataSerializers.BOOLEAN);

    private UUID lastAttacker;
    private int revengeCooldown = 0;
    private int attackStrengthTicker;

    public YawningNya(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HAS_REVENGE, false);
        this.entityData.define(DATA_HAS_SHOWN_JOIN_MESSAGE, false);
    }

    
    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_SPEED, 4.0D) 
                .build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        
        if (!this.level().isClientSide && !this.entityData.get(DATA_HAS_SHOWN_JOIN_MESSAGE)) {
            this.showJoinMessage();
            this.entityData.set(DATA_HAS_SHOWN_JOIN_MESSAGE, true);
        }

        
        if (this.attackStrengthTicker > 0) {
            --this.attackStrengthTicker;
        }

        if (revengeCooldown > 0) {
            revengeCooldown--;
            if (revengeCooldown <= 0) {
                this.entityData.set(DATA_HAS_REVENGE, false);
                this.lastAttacker = null;
            }
        }
    }

    private void showJoinMessage() {
        if (this.level() instanceof ServerLevel serverLevel) {
            
            Component joinMessage = Component.translatable("entity.epca.yawning_nya.join");

            
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(joinMessage, false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return false;
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity && attacker != this && revengeCooldown <= 0) {
            this.lastAttacker = attacker.getUUID();
            this.entityData.set(DATA_HAS_REVENGE, true);
            this.revengeCooldown = 100; 

            
            if (attacker instanceof LivingEntity livingAttacker) {
                this.setLastHurtByMob(livingAttacker);
                this.doHurtTarget(livingAttacker);
            }
        }

        return super.hurt(source, amount);
    }

    
    @Override
    public boolean doHurtTarget(Entity target) {
        
        ItemStack mainHandItem = this.getMainHandItem();

        
        float baseDamage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (!mainHandItem.isEmpty()) {
            
            baseDamage += getWeaponAttackDamage(mainHandItem);
        }

        
        float enchantmentDamage = 0.0F;
        if (target instanceof LivingEntity) {
            enchantmentDamage = EnchantmentHelper.getDamageBonus(mainHandItem, ((LivingEntity)target).getMobType());
        }

        float totalDamage = baseDamage + enchantmentDamage;

        
        this.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 1.0F);

        
        boolean attacked = target.hurt(this.damageSources().mobAttack(this), totalDamage);

        if (attacked) {
            
            if (enchantmentDamage > 0.0F && target instanceof LivingEntity) {
                ((LivingEntity)target).knockback((double)((float)enchantmentDamage * 0.5F),
                        this.getX() - target.getX(), this.getZ() - target.getZ());
            }

            
            if (!mainHandItem.isEmpty() && target instanceof LivingEntity) {
                EnchantmentHelper.doPostHurtEffects((LivingEntity)target, this);

                
                if (mainHandItem.isDamageableItem()) {
                    mainHandItem.hurtAndBreak(1, this, (entity) -> {
                        this.broadcastBreakEvent(InteractionHand.MAIN_HAND);
                    });
                }
            }

            
            this.setLastHurtMob(target);

            
            this.attackStrengthTicker = 20; 
        }

        return attacked;
    }

    private float getWeaponAttackDamage(ItemStack stack) {
        
        double attackDamage = stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)
                .stream()
                .mapToDouble(AttributeModifier::getAmount)
                .sum();

        return (float) attackDamage;
    }

    
    public float getAttackDamage() {
        ItemStack mainHandItem = this.getMainHandItem();
        float baseDamage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);

        if (!mainHandItem.isEmpty()) {
            baseDamage += getWeaponAttackDamage(mainHandItem);
        }

        
        float cooldownPercent = this.getAttackStrengthScale(0.0F);
        return baseDamage * (0.2F + cooldownPercent * cooldownPercent * 0.8F);
    }

    
    public float getAttackStrengthScale(float adjustTicks) {
        float attackStrength = (float)this.attackStrengthTicker - adjustTicks;
        if (attackStrength < 0.0F) {
            attackStrength = 0.0F;
        }

        return attackStrength / 20.0F;
    }

    
    @Override
    public ItemStack getMainHandItem() {
        return this.getItemInHand(InteractionHand.MAIN_HAND);
    }

    @Override
    public ItemStack getOffhandItem() {
        return this.getItemInHand(InteractionHand.OFF_HAND);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PLAYER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HasRevenge", this.entityData.get(DATA_HAS_REVENGE));
        compound.putBoolean("HasShownJoinMessage", this.entityData.get(DATA_HAS_SHOWN_JOIN_MESSAGE));
        compound.putInt("RevengeCooldown", this.revengeCooldown);
        compound.putInt("AttackStrengthTicker", this.attackStrengthTicker);
        if (this.lastAttacker != null) {
            compound.putUUID("LastAttacker", this.lastAttacker);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DATA_HAS_REVENGE, compound.getBoolean("HasRevenge"));
        this.entityData.set(DATA_HAS_SHOWN_JOIN_MESSAGE, compound.getBoolean("HasShownJoinMessage"));
        this.revengeCooldown = compound.getInt("RevengeCooldown");
        this.attackStrengthTicker = compound.getInt("AttackStrengthTicker");
        if (compound.hasUUID("LastAttacker")) {
            this.lastAttacker = compound.getUUID("LastAttacker");
        }
    }

    
    @Override
    public boolean canHoldItem(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeItem(ItemStack itemstack) {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        
        if (!this.getMainHandItem().isEmpty()) {
            this.spawnAtLocation(this.getMainHandItem().copy());
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    
    @Override
    public boolean isAffectedByPotions() {
        return true;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return true;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
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

    public static boolean checkNyaSpawnRules(
            EntityType<YawningNya> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        return level.getMaxLocalRawBrightness(pos) < 0;
    }
}