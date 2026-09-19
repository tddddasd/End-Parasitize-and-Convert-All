package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
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

    // 26.1.2: LivingEntity#getEffect/removeEffect take a Holder<MobEffect>. Resolving this instance through
    // the registry yields the canonical holder it was registered with.
    private Holder<MobEffect> holder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this);
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    // 26.1.2: applyEffectTick(ServerLevel,LivingEntity,int):boolean, run only server side
    // (MobEffectInstance#tickServer). Entity#hurt is final void now, so the two 2.0F hits go through
    // LivingEntity#hurtServer(ServerLevel, DamageSource, float) -- same damage source and amount.
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        
        if (isImmune(entity)) {
            entity.removeEffect(holder());
            return true;
        }

        
        MobEffectInstance effectInstance = entity.getEffect(holder());
        if (effectInstance == null) return true;

        int duration = effectInstance.getDuration();

        
        
        if (duration > 0 && duration % 20 == 0) {
            
            if (entity.isAlive()) {
                entity.hurtServer(serverLevel, entity.damageSources().inWall(), 2.0F);
            }

            
            if (entity.isAlive()) {
                entity.hurtServer(serverLevel, entity.damageSources().magic(), 2.0F);
            }
        }

        
        if (shouldRemoveEffect(entity)) {
            entity.removeEffect(holder());
        }
        return true;
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // 1.20.1 returned true unconditionally (the 20-tick cadence lives in the body), so keep ticking every tick.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        
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
