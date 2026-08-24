package org.tdddd.epca.impl.overworld.registry.entities.entity.misc;

import net.minecraft.util.Mth;
import org.tdddd.epca.impl.client.entity.IMotionAligned;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.fluid.ModFluids;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AcidBullet extends ThrowableProjectile implements GeoEntity, IMotionAligned {

    private int ticksInAir = 0;

    public AcidBullet(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (!this.level().isClientSide) {
            
            spawnAcidSolution(result.getLocation());

            if (result.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) result;
                Entity target = entityHit.getEntity();

                if (target instanceof LivingEntity livingTarget && !IParasite.isParasiteByTagOrInterface(livingTarget)) {
                    
                    livingTarget.hurt(livingTarget.damageSources().magic(), 4.0F);

                    
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                            ItemStack armor = livingTarget.getItemBySlot(slot);
                            if (!armor.isEmpty() && armor.isDamageableItem()) {
                                
                                int damageAmount = 16;

                                armor.hurtAndBreak(damageAmount, livingTarget,
                                        (entity) -> entity.broadcastBreakEvent(slot));
                            }
                        }
                    }
                }
            }

            
            for (int i = 0; i < 15; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;

                this.level().addParticle(ParticleTypes.ITEM_SLIME,
                        result.getLocation().x + offsetX,
                        result.getLocation().y + offsetY,
                        result.getLocation().z + offsetZ,
                        0.0, 0.5, 0.0);
            }

            this.discard(); 
        }
    }

    private void spawnAcidSolution(Vec3 location) {
        BlockPos pos = new BlockPos((int)location.x, (int)location.y, (int)location.z);

        
        BlockState currentState = this.level().getBlockState(pos);

        
        if (currentState.isAir() || currentState.canBeReplaced()) {
            
            FluidState acidFluid = ModFluids.FLOWING_ACID_SOLUTION.get().getFlowing(7, false);

            
            BlockState fluidBlockState = acidFluid.createLegacyBlock();
            this.level().setBlock(pos, fluidBlockState, 3);

            
            trySpreadAcid(pos.north());
            trySpreadAcid(pos.south());
            trySpreadAcid(pos.east());
            trySpreadAcid(pos.west());
            trySpreadAcid(pos.below()); 
        } else {
            
            trySpreadAcid(pos.above());
            trySpreadAcid(pos.north());
            trySpreadAcid(pos.south());
            trySpreadAcid(pos.east());
            trySpreadAcid(pos.west());
        }
    }

    private void trySpreadAcid(BlockPos pos) {
        BlockState currentState = this.level().getBlockState(pos);

        
        if (this.random.nextFloat() < 1.0f && (currentState.isAir() || currentState.canBeReplaced())) {
            
            int fluidLevel = 7;
            FluidState acidFluid = ModFluids.FLOWING_ACID_SOLUTION.get().getFlowing(fluidLevel, false);

            BlockState fluidBlockState = acidFluid.createLegacyBlock();
            this.level().setBlock(pos, fluidBlockState, 3);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        
        this.setDeltaMovement(this.getDeltaMovement().x, this.getDeltaMovement().y * 0.97, this.getDeltaMovement().z);

        ticksInAir++;

        if (this.level().isClientSide && ticksInAir > 2) {
            
            ParticleOptions particle = ParticleTypes.ITEM_SLIME;

            
            for (int i = 0; i < 2; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.1;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.1;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.1;

                this.level().addParticle(particle,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        0.0, 0.0, 0.0); 
            }

            
            if (this.random.nextFloat() < 0.1f) {
                for (int i = 0; i < 5; i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 0.3;
                    double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;

                    this.level().addParticle(ParticleTypes.ITEM_SLIME,
                            this.getX() + offsetX,
                            this.getY() + offsetY,
                            this.getZ() + offsetZ,
                            0.0, 0.5, 0.0);
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
