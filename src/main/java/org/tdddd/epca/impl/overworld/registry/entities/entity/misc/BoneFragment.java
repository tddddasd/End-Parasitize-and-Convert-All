package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModItems;

public class BoneFragment extends ThrowableProjectile {

    public BoneFragment(EntityType<? extends BoneFragment> type, Level level) {
        super(type, level);
    }

    public BoneFragment(Level level, LivingEntity shooter) {
        // 26.1.2: ThrowableProjectile has no (type, LivingEntity, Level) constructor any more.
        super(ModEntities.BONE_FRAGMENT.get(), shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ(), level);
        this.setOwner(shooter);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        // 26.1.2: Entity#defineSynchedData is abstract and neither Projectile nor
        // ThrowableProjectile implements it, so super.defineSynchedData(...) cannot be called.
        // There is nothing to define: throwable projectiles keep no synched data of their own.
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public void tick() {
        super.tick();

        
        if (level().isClientSide()) {
            ItemStack infestedBone = new ItemStack(ModItems.INFESTED_BONE.get());
            level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(infestedBone)),
                    getX(), getY(), getZ(),
                    random.nextDouble() - 0.5D,
                    random.nextDouble() - 0.5D,
                    random.nextDouble() - 0.5D
            );
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide()) {
            var target = result.getEntity();
            
            if (IParasite.isParasiteNoLivingByTagOrInterface(target)) {
                
                return;
            }
            
            // 26.1.2: Entity#hurt is final void; hurtOrSimulate is the boolean entry point.
            boolean damageApplied = target.hurtOrSimulate(this.damageSources().generic(), 1.0F);
            if (damageApplied && target instanceof LivingEntity livingTarget) {
                applyCothEffect(livingTarget);

                
                if (this.random.nextFloat() < 0.1f) {
                    livingTarget.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 5 * 20, 0));
                }

                
                if (target instanceof LivingEntity living && this.getPersistentData().getBoolean("InfestedFireArrow").orElse(false)) {
                    living.setRemainingFireTicks(160);
                }
            }
        }
        discardAndEffect();
    }

    
    private void applyCothEffect(LivingEntity target) {
        var existingEffect = target.getEffect(ModEffects.COTH);

        if (existingEffect != null) {
            
            target.addEffect(new MobEffectInstance(ModEffects.COTH, 30 * 20, 0));
        }
    }


    @Override
    protected void onHitBlock(BlockHitResult result) {
        discardAndEffect();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
    }

    private void discardAndEffect() {
        if (!level().isClientSide()) {
            level().playSound(null, blockPosition(), SoundEvents.POINTED_DRIPSTONE_BREAK,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else {
            ItemStack infestedBone = new ItemStack(ModItems.INFESTED_BONE.get());
            for (int i = 0; i < 8; i++) {
                double dx = (random.nextDouble() - 0.5D) * 0.3D;
                double dy = (random.nextDouble() - 0.5D) * 0.3D;
                double dz = (random.nextDouble() - 0.5D) * 0.3D;
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(infestedBone)),
                        getX(), getY(), getZ(), dx, dy, dz);
            }
        }
        this.discard();
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return super.canHitEntity(entity) && !entity.isSpectator();
    }
}