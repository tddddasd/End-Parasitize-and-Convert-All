package org.tdddd.epca.impl.overworld.registry.effects.buff;

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
    private static final String ATTACK_DAMAGE_UUID = "0a1b2c3d-4e5f-6789-abcd-ef0123456789";
    private static final String MOVEMENT_SPEED_UUID = "12345678-9abc-def0-1234-56789abcdef0";
    private static final Random RANDOM = new Random();

    
    private static final ConcurrentHashMap<UUID, Integer> LAST_COMMAND_TICK = new ConcurrentHashMap<>();

    public RageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x000000);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID, 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_UUID, 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        
        if (!entity.level().isClientSide && entity.level() instanceof ServerLevel serverLevel) {
            AABB boundingBox = entity.getBoundingBox();
            for (int i = 0; i < 5; i++) {
                double x = boundingBox.minX + RANDOM.nextDouble() * (boundingBox.maxX - boundingBox.minX);
                double y = boundingBox.minY + RANDOM.nextDouble() * (boundingBox.maxY - boundingBox.minY);
                double z = boundingBox.minZ + RANDOM.nextDouble() * (boundingBox.maxZ - boundingBox.minZ);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        x, y, z,
                        1,
                        0.0, 0.0, 0.0,
                        0.01
                );
            }

            
            var effectInstance = entity.getEffect(ModEffects.RAGE.get());
            if (effectInstance != null && effectInstance.getAmplifier() >= 29) {
                UUID uuid = entity.getUUID();
                int currentTick = entity.tickCount;
                Integer lastTick = LAST_COMMAND_TICK.get(uuid);

                
                if (lastTick == null || currentTick - lastTick >= 40) {
                    
                    String cmd = "photon fx epca:epca_rage_smoke entity " + entity.getStringUUID() +
                            " 0 0 0 0 0 0 1 1 1 0 false true";
                    serverLevel.getServer().getCommands().performPrefixedCommand(
                            serverLevel.getServer().createCommandSourceStack(),
                            cmd
                    );
                    LAST_COMMAND_TICK.put(uuid, currentTick);
                }
            }
        }
        super.applyEffectTick(entity, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; 
    }

    public static float getAttackDamageMultiplier(int amplifier) {
        return 1.0f + (amplifier + 1) * 0.1f;
    }

    public static float getMovementSpeedMultiplier(int amplifier) {
        return 1.0f + (amplifier + 1) * 0.1f;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}