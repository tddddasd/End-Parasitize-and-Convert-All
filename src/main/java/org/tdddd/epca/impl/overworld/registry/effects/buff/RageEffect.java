package org.tdddd.epca.impl.overworld.registry.effects.buff;

import org.tdddd.epca.impl.epca;
import net.minecraft.resources.Identifier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RageEffect extends MobEffect implements RemovableEffect {
    private static final Identifier ATTACK_DAMAGE_ID = Identifier.fromNamespaceAndPath(epca.MODID, "rage_attack_damage");
    private static final Identifier MOVEMENT_SPEED_ID = Identifier.fromNamespaceAndPath(epca.MODID, "rage_movement_speed");

    public RageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x000000);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_ID, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_ID, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        return super.applyEffectTick(serverLevel, entity, amplifier);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true; 
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}