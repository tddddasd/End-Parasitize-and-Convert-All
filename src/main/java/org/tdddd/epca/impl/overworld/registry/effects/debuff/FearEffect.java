package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class FearEffect extends MobEffect implements RemovableEffect {
    public static final MutableComponent FEAR_MESSAGE = Component.translatable("effect.epca.fear.message")
            .withStyle(ChatFormatting.RED);

    
    public static final float PANIC_MISS_CHANCE = 0.15F;

    
    private static final double PANIC_SCRAMBLE_BASE = 0.04D;

    
    private static final double PANIC_SCRAMBLE_MAX = 0.30D;

    public FearEffect() {
        super(MobEffectCategory.BENEFICIAL,  0x333333);
    }

    // 26.1.2: applyEffectTick(LivingEntity,int) -> applyEffectTick(ServerLevel,LivingEntity,int):boolean,
    // which only runs server side (MobEffectInstance#tickServer); the 1.20.1 "!level().isClientSide" guard
    // is therefore implied by the ServerLevel parameter, exactly as in CamouflageEffect/CorrosiveEffect.
    // LivingEntity#level() is still available and Entity#getRandom() is unchanged.
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        
        if (entity instanceof Player) return true;

        scramble(entity, amplifier);
        return true;
    }

    
    public static void scramble(LivingEntity entity, int amplifier) {
        
        double strength = Math.min(PANIC_SCRAMBLE_BASE * (amplifier + 1), PANIC_SCRAMBLE_MAX);

        
        int direction = entity.getRandom().nextInt(4);
        double dx = 0.0D;
        double dz = 0.0D;
        switch (direction) {
            case 0 -> dx = -strength; 
            case 1 -> dx = strength;  
            case 2 -> dz = -strength; 
            default -> dz = strength; 
        }

        
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x + dx, motion.y, motion.z + dz);
    }

    
    public static boolean rollPanicMiss(LivingEntity attacker) {
        return attacker.getRandom().nextFloat() < PANIC_MISS_CHANCE;
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // The 1.20.1 body ran on every game tick (the "1 << amplifier" interval is intentionally dropped so the
    // 乱动 cadence is level-independent and only the distance scales with the amplifier), so return true here
    // too -- same choice, and same comment, as CorrosiveEffect/SolidifyEffect in this port.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }


    
    public static boolean shouldPreventBlockPlacement(int amplifier) {
        return amplifier + 1 >= 2 && Math.random() < 0.2;
    }

    
    public static boolean shouldPreventItemUse(int amplifier) {
        return amplifier + 1 >= 3 && Math.random() < 0.2;
    }
}
