package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;
import org.tdddd.epca.impl.overworld.registry.ModParticles;

import java.util.Random;
import java.util.WeakHashMap;

// 26.1.2: removed @EventBusSubscriber -- this class declares no @SubscribeEvent methods,
// and the loader now throws IllegalArgumentException when such a class is registered.
public class BleedingEffect extends MobEffect implements RemovableEffect {
    private static final WeakHashMap<LivingEntity, Long> LAST_DAMAGE_TIME = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Vec3> LAST_POSITIONS = new WeakHashMap<>();
    private static final float DAMAGE_PERCENT = 0.02F;
    private static final float MAX_DAMAGE = 500.0F;
    private static final float BASE_INTERVAL = 25; 
    private static final float PER_LEVEL_REDUCTION = 4; 
    private static final float MIN_INTERVAL = 1; 
    private static final double MOVEMENT_THRESHOLD = 0.001;
    private static final Random RANDOM = new Random();
    private static final WeakHashMap<LivingEntity, Integer> PARTICLE_TICK_COUNTER = new WeakHashMap<>();

    public BleedingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF0000);}

    // 26.1.2: applyEffectTick(ServerLevel, LivingEntity, int) returns boolean and only runs server side
    // (MobEffectInstance#tickServer); the old "isClientSide()" early-out was already dead on the server.
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        long currentTime = entity.level().getGameTime();
        long lastTime = LAST_DAMAGE_TIME.getOrDefault(entity, 0L);

        
        int interval = (int) Math.max(MIN_INTERVAL,
                BASE_INTERVAL - (amplifier * PER_LEVEL_REDUCTION));

        if (currentTime - lastTime >= interval) {
            
            boolean isMoving = isEntityMoving(entity);

            float maxHealth = entity.getMaxHealth();
            
            float baseDamage = maxHealth * DAMAGE_PERCENT;
            
            float damage = isMoving ? baseDamage * 1.5f : baseDamage;
            
            float multiplier = DifficultyEffects.getBleedingDamageMultiplier(entity.level());
            damage *= multiplier;
            
            damage = Math.min(MAX_DAMAGE, damage);

            entity.hurtServer(serverLevel, entity.damageSources().magic(), damage);
            LAST_DAMAGE_TIME.put(entity, currentTime);
        }

        
        if (entity.level() instanceof ServerLevel particleLevel) {
            
            int tickCounter = PARTICLE_TICK_COUNTER.getOrDefault(entity, 0);
            tickCounter++;

            if (tickCounter >= 5) {
                tickCounter = 0;

                
                int particleCount = 1 + RANDOM.nextInt(3);

                
                AABB boundingBox = entity.getBoundingBox();

                for (int i = 0; i < particleCount; i++) {
                    
                    double x, y, z;

                    
                    int face = RANDOM.nextInt(6);
                    switch (face) {
                        case 0: 
                            x = boundingBox.minX + RANDOM.nextDouble() * (boundingBox.maxX - boundingBox.minX);
                            y = boundingBox.minY;
                            z = boundingBox.minZ + RANDOM.nextDouble() * (boundingBox.maxZ - boundingBox.minZ);
                            break;
                        case 1: 
                            x = boundingBox.minX + RANDOM.nextDouble() * (boundingBox.maxX - boundingBox.minX);
                            y = boundingBox.maxY;
                            z = boundingBox.minZ + RANDOM.nextDouble() * (boundingBox.maxZ - boundingBox.minZ);
                            break;
                        case 2: 
                            x = boundingBox.minX + RANDOM.nextDouble() * (boundingBox.maxX - boundingBox.minX);
                            y = boundingBox.minY + RANDOM.nextDouble() * (boundingBox.maxY - boundingBox.minY);
                            z = boundingBox.minZ;
                            break;
                        case 3: 
                            x = boundingBox.minX + RANDOM.nextDouble() * (boundingBox.maxX - boundingBox.minX);
                            y = boundingBox.minY + RANDOM.nextDouble() * (boundingBox.maxY - boundingBox.minY);
                            z = boundingBox.maxZ;
                            break;
                        case 4: 
                            x = boundingBox.minX;
                            y = boundingBox.minY + RANDOM.nextDouble() * (boundingBox.maxY - boundingBox.minY);
                            z = boundingBox.minZ + RANDOM.nextDouble() * (boundingBox.maxZ - boundingBox.minZ);
                            break;
                        case 5: 
                        default:
                            x = boundingBox.maxX;
                            y = boundingBox.minY + RANDOM.nextDouble() * (boundingBox.maxY - boundingBox.minY);
                            z = boundingBox.minZ + RANDOM.nextDouble() * (boundingBox.maxZ - boundingBox.minZ);
                            break;
                    }

                    
                    particleLevel.sendParticles(ModParticles.BLEEDING.get(),
                            x, y, z,
                            1, 
                            0.0, 0.0, 0.0, 
                            0.0 
                    );
                }
            }

            PARTICLE_TICK_COUNTER.put(entity, tickCounter);
        }

        return true;
    }

    private boolean isEntityMoving(LivingEntity entity) {
        
        Vec3 currentPos = entity.position();
        
        Vec3 lastPos = LAST_POSITIONS.get(entity);

        
        LAST_POSITIONS.put(entity, currentPos);

        
        if (lastPos == null) {
            return false;
        }

        
        double distanceMoved = currentPos.distanceToSqr(lastPos);
        
        return distanceMoved > MOVEMENT_THRESHOLD;
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // 1.20.1 returned true unconditionally (the real 25/21/... tick cadence is enforced by LAST_DAMAGE_TIME
    // above), so the effect must still be ticked every tick.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        
        return true;
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }
}
