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

/**
 * 26.1.2 迁移记录（三处注入点都按 26.1.2 的 {@code minecraft-patched-26.1.2.76} 源码核对过）：
 * <ul>
 *   <li>{@code addEffect}：1.20.1 有两个可注入的重载
 *       （{@code addEffect(MobEffectInstance)} 与
 *       {@code addEffect(MobEffectInstance, Entity)}），26.1.2 合并成一个
 *       {@code boolean addEffect(MobEffectInstance, @Nullable Entity)}，
 *       无源参数的调用点都是 {@code addEffect(effect, null)}。原来的两个 {@code @Inject}
 *       现在注入同一个方法，故合并为一处；<b>取消原始添加、替换成 V 级 60 秒 COTH 的行为完全保留</b>。</li>
 *   <li>{@code getEffect}/{@code removeEffect} 的参数由 {@code MobEffect} 变成
 *       {@code Holder<MobEffect>}；{@code ModEffects.COTH} 本身就是
 *       {@code DeferredHolder<MobEffect, MobEffect>}（即 {@code Holder<MobEffect>}），
 *       所以直接传 holder、不再 {@code .get()}。</li>
 *   <li>{@code setHealth}/{@code aiStep} 的名称与签名未变，注入原样保留。</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final ThreadLocal<Boolean> applyingCustomCoth = ThreadLocal.withInitial(() -> false);

    // ========== addEffect 拦截（26.1.2 只有一个重载） ==========
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void onAddEffect(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        // 如果正在应用自定义COTH，放行（避免递归）
        if (applyingCustomCoth.get()) {
            return;
        }

        // 只处理 COTH 效果
        if (effectInstance == null || !effectInstance.getEffect().is(ModEffects.COTH)) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();

        // 条件：服务器、非玩家、非IParasite、传说难度
        if (level.isClientSide()) return;
        if (self instanceof Player) return;
        if (self instanceof IParasite) return;
        if (!DifficultyEffects.isCothEffectEnabled(level)) return;

        // ---- 传说难度：强制替换为 V级 60秒 COTH ----
        // 1. 取消原始添加
        cir.setReturnValue(false);
        cir.cancel();

        // 2. 移除已有的 COTH（如果有）
        MobEffectInstance existing = self.getEffect(ModEffects.COTH);
        if (existing != null) {
            self.removeEffect(ModEffects.COTH);
        }

        // 3. 标记正在添加自定义效果
        applyingCustomCoth.set(true);
        try {
            // 创建 V级（amplifier=4），1200 ticks（60秒）的效果
            MobEffectInstance customCoth = new MobEffectInstance(
                    ModEffects.COTH,
                    1200,      // 60秒
                    4,         // V级
                    false, false, true
            );
            self.addEffect(customCoth);
            // 附加 COTH 标签（可选）
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
