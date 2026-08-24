package org.tdddd.epca.impl.overworld.registry.effects.buff;

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

    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.level().isClientSide && livingEntity.tickCount % 20 == 0) {
            livingEntity.level().getEntitiesOfClass(Mob.class,
                    livingEntity.getBoundingBox().inflate(30),
                    mob -> mob.getTarget() == livingEntity
            ).forEach(mob -> {
                if (shouldClearTarget(livingEntity, mob)) {
                    mob.setTarget(null);
                }
            });
        }
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

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    
    public static boolean tryRemoveOnAttack(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || !attacker.hasEffect(ModEffects.CAMOUFLAGE.get())) {
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
            attacker.removeEffect(ModEffects.CAMOUFLAGE.get());
        }
        return shouldRemove;
    }

    public static boolean hasCamouflageEffect(@Nullable LivingEntity entity) {
        return entity != null && entity.hasEffect(ModEffects.CAMOUFLAGE.get());
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}