package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.block.*;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.items.item.KillStick;
import org.tdddd.epca.impl.utils.ShieldProtectionHelper;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final ThreadLocal<Boolean> applyingCustomCoth = ThreadLocal.withInitial(() -> false);

    
    
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void epca$untargetableWhileConverting(CallbackInfoReturnable<Boolean> cir) {
        if (org.tdddd.epca.impl.events.PendingConversionManager.isPending((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void onAddEffect(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> cir) {
        
        if (applyingCustomCoth.get()) {
            return;
        }

        
        if (effectInstance == null || effectInstance.getEffect() != ModEffects.COTH.get()) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();

        
        if (level.isClientSide()) return;
        if (self instanceof Player) return;
        if (self instanceof IParasite) return;
        if (!DifficultyEffects.isCothEffectEnabled(level)) return;

        
        
        cir.setReturnValue(false);
        cir.cancel();

        
        MobEffectInstance existing = self.getEffect(ModEffects.COTH.get());
        if (existing != null) {
            self.removeEffect(ModEffects.COTH.get());
        }

        
        applyingCustomCoth.set(true);
        try {
            
            MobEffectInstance customCoth = new MobEffectInstance(
                    ModEffects.COTH.get(),
                    1200,      
                    4,         
                    false, false, true
            );
            self.addEffect(customCoth);
            
            self.getPersistentData().putBoolean("COTH", true);
        } finally {
            applyingCustomCoth.set(false);
        }
    }

    
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void onAddEffectWithSource(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        
        if (applyingCustomCoth.get()) {
            return;
        }

        if (effectInstance == null || effectInstance.getEffect() != ModEffects.COTH.get()) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();

        if (level.isClientSide()) return;
        if (self instanceof Player) return;
        if (self instanceof IParasite) return;
        if (!DifficultyEffects.isCothEffectEnabled(level)) return;

        
        cir.setReturnValue(false);
        cir.cancel();

        MobEffectInstance existing = self.getEffect(ModEffects.COTH.get());
        if (existing != null) {
            self.removeEffect(ModEffects.COTH.get());
        }

        applyingCustomCoth.set(true);
        try {
            MobEffectInstance customCoth = new MobEffectInstance(
                    ModEffects.COTH.get(),
                    1200,
                    4,
                    false, false, true
            );
            self.addEffect(customCoth);
            self.getPersistentData().putBoolean("COTH", true);
        } finally {
            applyingCustomCoth.set(false);
        }
    }

    private static final ThreadLocal<Boolean> processingSetHealth = ThreadLocal.withInitial(() -> false);

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void onSetHealth(float newHealth, CallbackInfo ci) {
        if (processingSetHealth.get()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        float oldHealth = self.getHealth();

        if (newHealth < oldHealth) {
            float damage = oldHealth - newHealth;

            
            if (self instanceof Player) {
                Player player = (Player) self;
                if (KillStick.hasAlayavijnanaItem(player)) {
                    LivingEntity target = KillStick.findLowestHealthEntity(player, 8.0);
                    if (target != null) {
                        
                        target.hurt(player.damageSources().generic(), damage);
                        
                        processingSetHealth.set(true);
                        try {
                            self.setHealth(oldHealth);
                        } finally {
                            processingSetHealth.set(false);
                        }
                        ci.cancel();
                        return;
                    } else {
                        
                        damage = damage * 0.3f;
                        newHealth = oldHealth - damage;
                        
                    }
                }
            }

            
            float remainingDamage = ShieldProtectionHelper.applyShieldProtection(self, damage);
            if (remainingDamage <= 0) {
                
                processingSetHealth.set(true);
                try {
                    self.setHealth(oldHealth);
                } finally {
                    processingSetHealth.set(false);
                }
                ci.cancel();
                return;
            } else if (remainingDamage < damage) {
                
                float actualNewHealth = oldHealth - remainingDamage;
                processingSetHealth.set(true);
                try {
                    self.setHealth(actualNewHealth);
                } finally {
                    processingSetHealth.set(false);
                }
                ci.cancel();
                return;
            } else {
                
                processingSetHealth.set(true);
                try {
                    self.setHealth(newHealth);
                } finally {
                    processingSetHealth.set(false);
                }
                ci.cancel();
                return;
            }
        }
    }

    
    private static boolean isClimbableWall(BlockState state) {
        Block block = state.getBlock();

        
        if (block instanceof InfestedSandstoneWall || block instanceof InfestedStoneWall ||
                block instanceof InfestedStoneBricksWall || block instanceof InfestedCobblestoneWall ||
                block instanceof InfestedPlanksFence) {
            return true;
        }
        
        return false;
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(IParasite.isParasiteByTagOrInterface(self))) return;

        
        AABB box = self.getBoundingBox();
        Level level = self.level();
        BlockPos min = new BlockPos((int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ));
        BlockPos max = new BlockPos((int) Math.floor(box.maxX), (int) Math.floor(box.maxY), (int) Math.floor(box.maxZ));
        boolean touchingClimbable = false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (isClimbableWall(level.getBlockState(pos))) {
                touchingClimbable = true;
                break;
            }
        }
        if (!touchingClimbable) return;

        
        Vec3 delta = self.getDeltaMovement();
        if (delta.y < 0.2) {
            self.setDeltaMovement(delta.x, 0.5, delta.z);
            self.fallDistance = 0;
        }

        
        
        double minHorizontalSpeed = 0.0;
        double horizontalSpeedSqr = delta.x * delta.x + delta.z * delta.z;
        if (horizontalSpeedSqr < minHorizontalSpeed * minHorizontalSpeed) {
            
            float yaw = self.getYRot();
            Vec3 forward = Vec3.directionFromRotation(0, yaw).normalize();
            
            double thrust = 0.15;
            
            self.setDeltaMovement(delta.x + forward.x * thrust, delta.y, delta.z + forward.z * thrust);
        }
    }
}
