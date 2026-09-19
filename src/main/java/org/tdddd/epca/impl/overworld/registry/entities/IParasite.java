package org.tdddd.epca.impl.overworld.registry.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import org.tdddd.epca.impl.events.ParasiteFollowEvent;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.ai.ParasiteAttractionManager;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptationManager;

import java.util.UUID;

public interface IParasite {
    String LAST_RAGE_TRIGGER_KEY = "lastRageTrigger";
    String FOLLOW_TARGET_KEY = "FollowTarget";

    default void setFollowTarget(UUID targetUuid) {
        LivingEntity entity = (LivingEntity) this;
        UUID oldTarget = getFollowTarget();
        if (oldTarget == targetUuid || (oldTarget != null && oldTarget.equals(targetUuid))) return;

        if (!entity.level().isClientSide()) {
            ParasiteFollowEvent event = new ParasiteFollowEvent(entity, oldTarget, targetUuid);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            // SCR-3: ParasiteFollowEvent still carries the deleted @Cancelable annotation and does
            // not implement ICancellableEvent yet (it is owned by the events cluster). Reading the
            // flag through the marker interface keeps this file compiling now and behaves exactly
            // like a direct event.isCanceled() call the moment the marker is added. Until then
            // cancellation is inert.
            if (event instanceof net.neoforged.bus.api.ICancellableEvent cancellable
                    && cancellable.isCanceled()) {
                return;
            }
        }

        CompoundTag tag = entity.getPersistentData();
        if (targetUuid == null) {
            tag.remove(FOLLOW_TARGET_KEY);
        } else {
            tag.putString(FOLLOW_TARGET_KEY, targetUuid.toString());
        }
    }

    default UUID getFollowTarget() {
        LivingEntity entity = (LivingEntity) this;
        String s = entity.getPersistentData().getString(FOLLOW_TARGET_KEY).orElse("");
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static boolean isParasiteByTagOrInterface(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof Player) {
            return NestLeaderManager.isNestLeader(entity.getUUID());
        }
        return (entity instanceof IParasite) || entity.getPersistentData().getBoolean("Parasite").orElse(false);
    }

    static boolean isParasiteNoLivingByTagOrInterface(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof Player) {
            return NestLeaderManager.isNestLeader(entity.getUUID());
        }
        return (entity instanceof IParasite) || entity.getPersistentData().getBoolean("Parasite").orElse(false);
    }

    default boolean hasDamageAdaptationConfig() {
        LivingEntity entity = (LivingEntity) this;
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return DamageAdaptationManager.hasConfig(entityId);
    }

    
    default DamageAdaptationConfig getDamageAdaptationConfig() {
        LivingEntity entity = (LivingEntity) this;
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return DamageAdaptationManager.getConfig(entityId);
    }

    
    default boolean isDamageAdaptationInvulnerable() {
        return DamageAdaptation.isInvulnerable((LivingEntity) this);
    }

    
    default float handleParasiteDamage(DamageSource source, float amount) {
        LivingEntity parasite = (LivingEntity) this;

        
        if (isDamageAdaptationInvulnerable()) {
            return 0.0f; 
        }

        
        if (hasDamageAdaptationConfig()) {
            
            
            return handleParasiteSpecificDamage(source, amount);
        } else {
            
            return handleParasiteSpecificDamage(source, amount);
        }
    }

    
    private float handleParasiteSpecificDamage(DamageSource source, float amount) {
        LivingEntity parasite = (LivingEntity) this;
        Level level = parasite.level();

        
        if (source.is(DamageTypeTags.IS_FIRE)) {
            
            int currentTick = (int) level.getGameTime();
            int lastTriggerTick = getLastRageTriggerTick(parasite);

            
            if (currentTick - lastTriggerTick < 10) {
                return amount; 
            }

            
            if (level.getRandom().nextFloat() < 0.1f) {
                
                MobEffectInstance currentRage = parasite.getEffect(ModEffects.RAGE);
                int amplifier = 0;

                if (currentRage != null) {
                    
                    amplifier = Math.min(currentRage.getAmplifier() + 1, 49); 
                }

                
                MobEffectInstance rageEffect = new MobEffectInstance(
                        ModEffects.RAGE,
                        300, 
                        amplifier,
                        false, 
                        true, 
                        true 
                );

                parasite.addEffect(rageEffect);

                
                setLastRageTriggerTick(parasite, currentTick);
            }
        }

        return amount; 
    }

    
    default void onKillEntity(LivingEntity killedEntity) {
        
        Level level = ((LivingEntity) this).level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        
        EvolutionManager evolutionManager = EvolutionManager.forDimension(serverLevel);

        
        evolutionManager.addPoints(1);
    }


    default boolean isFriendlyParasite(LivingEntity entity) {
        return isParasiteByTagOrInterface(entity);
    }

    default boolean shouldIgnoreDamageFrom(LivingEntity attacker) {
        if (attacker instanceof Player) {
            if (NestLeaderManager.isNestLeader(attacker.getUUID())) {
                return false;
            }
        }
        return isFriendlyParasite(attacker);
    }

    default boolean shouldIgnoreTarget(LivingEntity target) {
        return isFriendlyParasite(target);
    }

    default void onDeath(DamageSource source) {
        LivingEntity parasite = (LivingEntity) this;
        Level level = parasite.level();
    }

    
    default void onAttacked(LivingEntity attacker) {
        if (!isFriendlyParasite(attacker)) {
            
            trySwitchForcedTargetOnAttacked(attacker);
            
            ParasiteAttractionManager.onParasiteAttacked((LivingEntity) this, attacker);
        }
    }

    
    default float onHurt(DamageSource source, float amount) {
        return handleParasiteDamage(source, amount);
    }

    
    private int getLastRageTriggerTick(LivingEntity parasite) {
        
        return parasite.getPersistentData().getInt(LAST_RAGE_TRIGGER_KEY).orElse(0);
    }

    
    private void setLastRageTriggerTick(LivingEntity parasite, int tick) {
        parasite.getPersistentData().putInt(LAST_RAGE_TRIGGER_KEY, tick);
    }

    
    default boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    
    default void setParasite(boolean isParasite) {
    }

    default void applyDifficultyStatModifiers() {
        LivingEntity entity = (LivingEntity) this;
        float multiplier = DifficultyEffects.getParasiteStatMultiplier(entity.level());
        
        
        float newMaxHealth = entity.getMaxHealth() * multiplier;
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        entity.setHealth(newMaxHealth);
        if (!(entity instanceof Player)) {
            float newArmor = entity.getArmorValue() * multiplier;
            entity.getAttribute(Attributes.ARMOR).setBaseValue(newArmor);
        }
    }

    

    
    default CompoundTag getVariantData() {
        return new CompoundTag();
    }

    
    default void setVariantData(CompoundTag data) {
        
    }

    
    String FORCED_TARGET_UUID_KEY = "forcedTargetUuid";
    String LAST_FORCED_SWITCH_TICK_KEY = "lastForcedSwitchTick";

    
    int FORCED_TARGET_COOLDOWN_SECONDS = 12;

    
    default UUID getForcedTargetUuid() {
        String uuidStr = ((LivingEntity) this).getPersistentData().getString(FORCED_TARGET_UUID_KEY).orElse("");
        if (uuidStr.isEmpty()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    default void setForcedTargetUuid(UUID uuid) {
        CompoundTag tag = ((LivingEntity) this).getPersistentData();
        if (uuid == null) {
            tag.remove(FORCED_TARGET_UUID_KEY);
        } else {
            tag.putString(FORCED_TARGET_UUID_KEY, uuid.toString());
        }
    }

    default LivingEntity getForcedTarget(ServerLevel level) {
        UUID uuid = getForcedTargetUuid();
        if (uuid == null) return null;
        Entity entity = level.getEntity(uuid);
        if (entity instanceof LivingEntity living && living.isAlive() && !isFriendlyParasite(living)) {
            return living;
        }
        
        setForcedTargetUuid(null);
        return null;
    }

    default void setForcedTarget(LivingEntity target, ServerLevel level) {
        if (target == null || !target.isAlive()) {
            setForcedTargetUuid(null);
            return;
        }
        setForcedTargetUuid(target.getUUID());
    }

    
    default long getLastForcedSwitchTick() {
        return ((LivingEntity) this).getPersistentData().getLong(LAST_FORCED_SWITCH_TICK_KEY).orElse(0L);
    }

    default void setLastForcedSwitchTick(long tick) {
        ((LivingEntity) this).getPersistentData().putLong(LAST_FORCED_SWITCH_TICK_KEY, tick);
    }

    default boolean isForcedTargetSwitchOnCooldown(Level level) {
        long lastTick = getLastForcedSwitchTick();
        long currentTick = level.getGameTime();
        return (currentTick - lastTick) < (FORCED_TARGET_COOLDOWN_SECONDS * 20L);
    }

    
    default boolean trySwitchForcedTargetOnAttacked(LivingEntity attacker) {
        LivingEntity self = (LivingEntity) this;
        Level level = self.level();
        if (level.isClientSide()) return false;

        
        if (isForcedTargetSwitchOnCooldown(level)) return false;

        
        double sqDist = self.distanceToSqr(attacker);
        double closeRangeSq = 16.0 * 16.0;  
        if (sqDist > closeRangeSq) return false;

        
        if (level instanceof ServerLevel serverLevel) {
            setForcedTarget(attacker, serverLevel);
            setLastForcedSwitchTick(level.getGameTime());
            return true;
        }
        return false;
    }
}