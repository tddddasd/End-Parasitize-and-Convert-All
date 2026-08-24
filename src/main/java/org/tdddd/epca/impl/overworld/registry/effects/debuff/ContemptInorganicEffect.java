package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.overworld.data.EntityConversionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;

import java.util.concurrent.ThreadLocalRandom;

public class ContemptInorganicEffect extends MobEffect {

    private static final int BASE_DURATION = 1200;
    private static final int COTH_LEVEL_III = 2;
    private static final int COTH_DURATION = 1200;
    private static final int COTH_INTERVAL = 100;
    private static final int MIN_SOUND_INTERVAL = 100;
    private static final int MAX_SOUND_INTERVAL = 140;

    private static final String TAG_LAST_COTH_TIME = "LastCothTime";
    private static final String TAG_NEXT_SOUND_TIME = "NextSoundTime";
    private static final String TAG_CONVERTED_BY_CONTEMPT = "ConvertedByContemptInorganic";

    public ContemptInorganicEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x6B3E2A);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        long gameTime = serverLevel.getGameTime();

        MobEffectInstance currentEffect = entity.getEffect(this);
        if (currentEffect == null) return;
        int duration = currentEffect.getDuration();

        
        if (duration <= 1 && !(entity instanceof Player)) {
            entity.addEffect(new MobEffectInstance(this, BASE_DURATION, 0, false, false, true));
            return;
        }

        
        long lastCothTime = entity.getPersistentData().getLong(TAG_LAST_COTH_TIME);
        if (lastCothTime == 0) {
            lastCothTime = gameTime - COTH_INTERVAL;
            entity.getPersistentData().putLong(TAG_LAST_COTH_TIME, lastCothTime);
        }
        if (gameTime - lastCothTime >= COTH_INTERVAL) {
            entity.addEffect(new MobEffectInstance(ModEffects.COTH.get(), COTH_DURATION, COTH_LEVEL_III, false, false, true));
            entity.getPersistentData().putLong(TAG_LAST_COTH_TIME, gameTime);
        }

        
        if (entity instanceof Player player) {
            long nextSoundTime = entity.getPersistentData().getLong(TAG_NEXT_SOUND_TIME);
            if (nextSoundTime == 0) {
                int initialDelay = ThreadLocalRandom.current().nextInt(MIN_SOUND_INTERVAL, MAX_SOUND_INTERVAL + 1);
                nextSoundTime = gameTime + initialDelay;
                entity.getPersistentData().putLong(TAG_NEXT_SOUND_TIME, nextSoundTime);
            }
            if (gameTime >= nextSoundTime) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSoundEvents.MOZZIE_IDLE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                int newDelay = ThreadLocalRandom.current().nextInt(MIN_SOUND_INTERVAL, MAX_SOUND_INTERVAL + 1);
                entity.getPersistentData().putLong(TAG_NEXT_SOUND_TIME, gameTime + newDelay);
            }
        }

        
        if (!entity.getPersistentData().getBoolean(TAG_CONVERTED_BY_CONTEMPT)) {
            MobEffectInstance coth = entity.getEffect(ModEffects.COTH.get());
            if (coth != null && entity.getHealth() <= entity.getMaxHealth() * 0.5f) {
                
                tryPerformConversion(entity);
            }
        }
    }

    
    private void tryPerformConversion(LivingEntity entity) {
        
        if (entity instanceof Player || IParasite.isParasiteByTagOrInterface(entity)) {
            return;
        }
        if (ModConfig.isInConversionModImmunityWhitelist(entity)) {
            return;
        }

        CompoundTag nbt = entity.saveWithoutId(new CompoundTag());
        EntityType<?> entityType = entity.getType();
        EntityConversionManager.EntityConversionRule rule = EntityConversionManager.getConversionRule(entityType, nbt);

        if (rule != null && rule.mozzie_to != null && !rule.mozzie_to.isEmpty()) {
            String targetEntityId = rule.mozzie_to;
            ResourceLocation targetLocation = new ResourceLocation(targetEntityId);
            EntityType<?> targetType = ForgeRegistries.ENTITY_TYPES.getValue(targetLocation);

            if (targetType != null && entity.level() instanceof ServerLevel serverLevel) {
                try {
                    Entity newEntity = targetType.create(serverLevel);
                    if (newEntity != null) {
                        
                        newEntity.setPos(entity.getX(), entity.getY(), entity.getZ());
                        newEntity.setYRot(entity.getYRot());
                        newEntity.setXRot(entity.getXRot());

                        
                        playConversionEffects(entity);

                        
                        entity.remove(Entity.RemovalReason.KILLED);
                        entity.teleportTo(1000000, -4000, 1000000);

                        
                        serverLevel.addFreshEntity(newEntity);

                        
                        entity.getPersistentData().putBoolean(TAG_CONVERTED_BY_CONTEMPT, true);
                    }
                } catch (Exception e) {
                    
                }
            }
        }
    }

    
    private void playConversionEffects(LivingEntity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 0.8F, 1.2F);

        spawnConversionParticles(entity);
    }

    private static void spawnConversionParticles(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    entity.getX(), entity.getY(), entity.getZ(),
                    5,
                    0.5, 0.5, 0.5,
                    0.1);
        }
    }
}