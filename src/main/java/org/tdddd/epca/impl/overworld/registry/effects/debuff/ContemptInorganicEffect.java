package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.overworld.data.EntityConversionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;

import java.util.Random;
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

    // 26.1.2: LivingEntity#getEffect/addEffect and MobEffectInstance(Holder<MobEffect>, ...) take a Holder.
    // Resolving this instance through the registry yields the canonical holder it was registered with.
    private Holder<MobEffect> holder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this);
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // 1.20.1 returned true unconditionally and the body below enforces its own cadence.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }

    // 26.1.2: applyEffectTick(LivingEntity,int) -> applyEffectTick(ServerLevel,LivingEntity,int):boolean.
    // The body is unchanged; the isClientSide() early-out is now implied by the ServerLevel parameter.
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        long gameTime = serverLevel.getGameTime();

        MobEffectInstance currentEffect = entity.getEffect(holder());
        if (currentEffect == null) return true;
        int duration = currentEffect.getDuration();

        
        if (duration <= 1 && !(entity instanceof Player)) {
            entity.addEffect(new MobEffectInstance(holder(), BASE_DURATION, 0, false, false, true));
            return true;
        }

        
        long lastCothTime = entity.getPersistentData().getLong(TAG_LAST_COTH_TIME).orElse(0L);
        if (lastCothTime == 0) {
            lastCothTime = gameTime - COTH_INTERVAL;
            entity.getPersistentData().putLong(TAG_LAST_COTH_TIME, lastCothTime);
        }
        if (gameTime - lastCothTime >= COTH_INTERVAL) {
            entity.addEffect(new MobEffectInstance(ModEffects.COTH, COTH_DURATION, COTH_LEVEL_III, false, false, true));
            entity.getPersistentData().putLong(TAG_LAST_COTH_TIME, gameTime);
        }

        
        if (entity instanceof Player player) {
            long nextSoundTime = entity.getPersistentData().getLong(TAG_NEXT_SOUND_TIME).orElse(0L);
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

        
        if (!entity.getPersistentData().getBoolean(TAG_CONVERTED_BY_CONTEMPT).orElse(false)) {
            MobEffectInstance coth = entity.getEffect(ModEffects.COTH);
            if (coth != null && entity.getHealth() <= entity.getMaxHealth() * 0.5f) {
                
                tryPerformConversion(entity);
            }
        }
        return true;
    }

    // 26.1.2: Entity#saveWithoutId now writes into a ValueOutput. EntityConversionManager still matches
    // its datapack rules against a CompoundTag, so the entity is serialised through TagValueOutput with
    // the entity's registry context -- the same field set (including "Age") that 1.20.1 produced.
    private static CompoundTag saveEntityTag(LivingEntity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    
    private void tryPerformConversion(LivingEntity entity) {
        
        if (entity instanceof Player || IParasite.isParasiteByTagOrInterface(entity)) {
            return;
        }
        if (ModConfig.isInConversionModImmunityWhitelist(entity)) {
            return;
        }

        CompoundTag nbt = saveEntityTag(entity);
        EntityType<?> entityType = entity.getType();
        EntityConversionManager.EntityConversionRule rule = EntityConversionManager.getConversionRule(entityType, nbt);

        if (rule != null && rule.mozzie_to != null && !rule.mozzie_to.isEmpty()) {
            String targetEntityId = rule.mozzie_to;
            Identifier targetLocation = Identifier.parse(targetEntityId);
            EntityType<?> targetType = BuiltInRegistries.ENTITY_TYPE.getValue(targetLocation);

            if (targetType != null && entity.level() instanceof ServerLevel serverLevel) {
                try {
                    Entity newEntity = targetType.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
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
            // 26.1.2: the conversion burst is EPCA meat particles (3~5) instead of explosions.
            net.minecraft.util.RandomSource random = entity.getRandom();
            int count = 3 + random.nextInt(3);
            serverLevel.sendParticles(ModParticles.LIVING_FLESH.get(),
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    count, 0.5, 0.5, 0.5, 0.05);
        }
    }
}