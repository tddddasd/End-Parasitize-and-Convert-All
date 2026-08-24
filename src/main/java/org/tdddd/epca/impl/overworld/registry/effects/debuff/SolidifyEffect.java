package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class SolidifyEffect extends MobEffect implements RemovableEffect {
    public SolidifyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B795E);
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        
        if (isImmune(entity)) {
            entity.removeEffect(this);
            return;
        }

        
        MobEffectInstance effectInstance = entity.getEffect(this);
        if (effectInstance == null) return;

        int duration = effectInstance.getDuration();

        
        
        if (duration > 0 && duration % 20 == 0) {
            
            if (entity.isAlive()) {
                entity.hurt(entity.damageSources().inWall(), 2.0F);
            }

            
            if (entity.isAlive()) {
                entity.hurt(entity.damageSources().magic(), 2.0F);
            }
        }

        
        if (shouldRemoveEffect(entity)) {
            entity.removeEffect(this);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        
        return true;
    }

    
    private boolean shouldRemoveEffect(LivingEntity entity) {
        
        return entity.isOnFire();
    }

    
    private boolean isImmune(LivingEntity entity) {
               return entity instanceof Slime ||
                entity instanceof MagmaCube ||
                entity instanceof Blaze ||
                entity instanceof WitherBoss ||
                entity instanceof EnderDragon;
    }
}