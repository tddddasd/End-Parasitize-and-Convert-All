package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InfestedPlayer extends Monster implements IParasite, IInfested, GeoEntity, RangedAttackMob , IHeadRotatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private static final EntityDataAccessor<String> SOURCE_PLAYER_NAME =
            SynchedEntityData.defineId(InfestedPlayer.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> SOURCE_PLAYER_UUID =
            SynchedEntityData.defineId(InfestedPlayer.class, EntityDataSerializers.OPTIONAL_UUID);

    
    private final ItemStack[] inventory = new ItemStack[38];

    
    private int attackStrengthTicker;
    
    private int bowCharge;

    public InfestedPlayer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        for (int i = 0; i < inventory.length; i++) inventory[i] = ItemStack.EMPTY;
        this.xpReward = 5;
    }

    
    public ItemStack[] getInventory() { return inventory; }

    @Override
    public ItemStack getMainHandItem() { return inventory[0]; }

    @Override
    public ItemStack getOffhandItem() { return inventory[1]; }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) inventory[0] = stack;
        else if (slot == EquipmentSlot.OFFHAND) inventory[1] = stack;
    }

    
    @Override
    public Iterable<ItemStack> getArmorSlots() { return List.of(); }  

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) return inventory[0];
        if (slot == EquipmentSlot.OFFHAND) return inventory[1];
        return ItemStack.EMPTY;
    }

    
    public void setSourcePlayer(String name, UUID uuid) {
        this.entityData.set(SOURCE_PLAYER_NAME, name != null ? name : "");
        this.entityData.set(SOURCE_PLAYER_UUID, uuid != null ? Optional.of(uuid) : Optional.empty());
        if (name != null) {
            this.setCustomName(Component.literal(name));
            this.setCustomNameVisible(true);
        }
    }

    public String getSourcePlayerName() { return this.entityData.get(SOURCE_PLAYER_NAME); }
    public UUID getSourcePlayerUUID() { return this.entityData.get(SOURCE_PLAYER_UUID).orElse(null); }

    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SOURCE_PLAYER_NAME, "");
        this.entityData.define(SOURCE_PLAYER_UUID, Optional.empty());
    }

    
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new RangedBowAttackGoal<>(this, 1.0D, 20, 15.0F));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 32.0D));
    }

    
    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) { return weapon instanceof BowItem; }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack bowStack = this.getMainHandItem();
        if (!(bowStack.getItem() instanceof BowItem)) return;
        ItemStack projectileStack = getProjectile();
        if (projectileStack.isEmpty()) return;
        Item arrowItem = projectileStack.getItem();
        if (!(arrowItem instanceof ArrowItem)) arrowItem = Items.ARROW;
        AbstractArrow arrow = ((ArrowItem) arrowItem).createArrow(this.level(), bowStack, this);
        arrow.setEnchantmentEffectsFromEntity(this, distanceFactor);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float speed = 1.6F * (0.5F + distanceFactor * 0.5F);
        float spread = (float) (14 - this.level().getDifficulty().getId() * 4) * (1 - distanceFactor * 0.5F);
        arrow.shoot(dx, dy + dist * 0.2D, dz, speed, spread);
        this.level().addFreshEntity(arrow);
        if (!projectileStack.isEmpty() && !projectileStack.is(Items.ARROW)) projectileStack.shrink(1);
    }

    private ItemStack getProjectile() {
        ItemStack offhand = this.getOffhandItem();
        if (offhand.getItem() instanceof ArrowItem) return offhand;
        for (int i = 2; i < inventory.length; i++) {
            if (inventory[i].getItem() instanceof ArrowItem) return inventory[i];
        }
        return ItemStack.EMPTY;
    }

    
    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.attackStrengthTicker > 0) return false;
        ItemStack stack = this.getMainHandItem();
        float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (stack.getItem() instanceof TieredItem tiered) baseDamage += tiered.getTier().getAttackDamageBonus();
        else if (stack.getItem() instanceof SwordItem sword) baseDamage += sword.getDamage();
        boolean isCrit = !this.onGround() && this.getDeltaMovement().y() < 0 && !this.isInWater() && !this.hasEffect(MobEffects.SLOW_FALLING);
        float enchantDamage = EnchantmentHelper.getDamageBonus(stack, ((LivingEntity) target).getMobType());
        float totalDamage = baseDamage + enchantDamage;
        if (isCrit) totalDamage *= 1.5F;
        float knockback = (float) EnchantmentHelper.getKnockbackBonus(this);
        if (this.isSprinting()) knockback += 1.0F;
        boolean hurt = target.hurt(this.damageSources().mobAttack(this), totalDamage);
        if (hurt) {
            if (knockback > 0 && target instanceof LivingEntity livingTarget) {
                livingTarget.knockback(knockback * 0.5F, Math.sin(this.getYRot() * Math.PI / 180.0D), -Math.cos(this.getYRot() * Math.PI / 180.0D));
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }
            if (stack.getItem() instanceof SwordItem && !this.isCrouching()) sweepAttack(target);
            this.swing(InteractionHand.MAIN_HAND);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, this, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            double attackSpeed = this.getAttributeValue(Attributes.ATTACK_SPEED);
            this.attackStrengthTicker = (int) (20.0 / attackSpeed);
        }
        return hurt;
    }

    private void sweepAttack(Entity target) {
        double range = 3.0D;
        AABB aabb = this.getBoundingBox().inflate(range, 0.25D, range);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, aabb, e -> e != this && e != target && !this.isAlliedTo(e) && e.isAlive());
        float sweepDamage = 1.0F + EnchantmentHelper.getSweepingDamageRatio(this) * (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity entity : nearby) {
            entity.hurt(this.damageSources().mobAttack(this), sweepDamage);
            Vec3 vec = entity.position().subtract(this.position()).normalize();
            entity.setDeltaMovement(vec.multiply(0.5D, 0.5D, 0.5D));
        }
        double x = -Math.sin(this.getYRot() * Math.PI / 180.0F);
        double z = Math.cos(this.getYRot() * Math.PI / 180.0F);
        this.level().addParticle(ParticleTypes.SWEEP_ATTACK, this.getX() + x, this.getY() + this.getBbHeight() * 0.5, this.getZ() + z, 0, 0, 0);
    }

    
    public void copyFromPlayer(Player player) {
        this.setSourcePlayer(player.getName().getString(), player.getUUID());
        
        inventory[0] = player.getMainHandItem().copy();
        inventory[1] = player.getOffhandItem().copy();
        for (int i = 0; i < 9; i++) inventory[2 + i] = player.getInventory().getItem(i).copy();
        for (int i = 9; i < 36; i++) inventory[11 + (i - 9)] = player.getInventory().getItem(i).copy();
        
        player.getActiveEffects().forEach(effect -> this.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect)));
    }

    
    @Override
    public void tick() {
        super.tick();
        if (this.attackStrengthTicker > 0) this.attackStrengthTicker--;
        if (this.isUsingItem() && this.getUseItem().getItem() instanceof BowItem) this.bowCharge++;
        else this.bowCharge = 0;
    }

    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SourcePlayerName", getSourcePlayerName());
        UUID uuid = getSourcePlayerUUID();
        if (uuid != null) tag.putUUID("SourcePlayerUUID", uuid);
        else tag.putUUID("SourcePlayerUUID", new UUID(0,0));
        ListTag invTag = new ListTag();
        for (int i = 0; i < inventory.length; i++) {
            if (!inventory[i].isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putByte("Slot", (byte) i);
                inventory[i].save(slotTag);
                invTag.add(slotTag);
            }
        }
        tag.put("Inventory", invTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String name = tag.getString("SourcePlayerName");
        UUID uuid = tag.getUUID("SourcePlayerUUID");
        if (uuid != null && !uuid.equals(new UUID(0,0))) setSourcePlayer(name.isEmpty() ? null : name, uuid);
        else setSourcePlayer(name.isEmpty() ? null : name, null);
        ListTag invTag = tag.getList("Inventory", 10);
        for (int i = 0; i < invTag.size(); i++) {
            CompoundTag slotTag = invTag.getCompound(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < inventory.length) inventory[slot] = ItemStack.of(slotTag);
        }
    }

    
    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .build();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<InfestedPlayer> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
            return PlayState.CONTINUE;
        }
        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}