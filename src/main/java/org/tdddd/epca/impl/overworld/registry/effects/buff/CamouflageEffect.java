package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;


public class CamouflageEffect extends MobEffect implements RemovableEffect {

    public CamouflageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B9A46);
    }

    // 26.1.2: applyEffectTick(ServerLevel, LivingEntity, int) returns boolean and only runs server side
    // (MobEffectInstance#tickServer). The tick body is unchanged; the old "!isClientSide()" guard is now
    // implied by the ServerLevel parameter.
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity livingEntity, int amplifier) {
        if (livingEntity.tickCount % 20 == 0) {
            livingEntity.level().getEntitiesOfClass(Mob.class,
                    livingEntity.getBoundingBox().inflate(30),
                    mob -> mob.getTarget() == livingEntity
            ).forEach(mob -> {
                if (shouldClearTarget(livingEntity, mob)) {
                    mob.setTarget(null);
                }
            });
        }
        return true;
    }

    
    private boolean shouldClearTarget(LivingEntity holder, LivingEntity entity) {
        
        boolean isHolderParasite = isParasite(holder);
        boolean isMobParasite = isParasite(entity);

        if (isHolderParasite) {
            
            return !(entity instanceof Player);
        } else {
            
            return isMobParasite;
        }
    }

    
    private static boolean isParasite(LivingEntity entity) {
        
        return IParasite.isParasiteByTagOrInterface(entity);
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // Same "every 20 ticks" gate as 1.20.1.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return tickCount % 20 == 0;
    }

    
    public static boolean tryRemoveOnAttack(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || !attacker.hasEffect(ModEffects.CAMOUFLAGE)) {
            return false;
        }

        boolean shouldRemove = false;
        boolean isAttackerParasite = isParasite(attacker);
        boolean isTargetParasite = isParasite(target);

        if (isAttackerParasite) {
            
            if (!(target instanceof Player) && !isTargetParasite) {
                shouldRemove = true;
            }
        } else {
            
            if (isTargetParasite) {
                shouldRemove = true;
            }
        }

        if (shouldRemove) {
            attacker.removeEffect(ModEffects.CAMOUFLAGE);
        }
        return shouldRemove;
    }

    public static boolean hasCamouflageEffect(@Nullable LivingEntity entity) {
        return entity != null && entity.hasEffect(ModEffects.CAMOUFLAGE);
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}
