package org.tdddd.epca.impl.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModTags;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptationManager;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ParasiteEvents {

    public static final String LAST_RAGE_TRIGGER_KEY = "lastRageTrigger";
    public static final String FOLLOW_TARGET_KEY = "FollowTarget";
    public static final String BIOMASS_SPAWNED_KEY = "BiomassSpawned";
    public static final String FORCED_TARGET_UUID_KEY = "forcedTargetUuid";
    public static final String LAST_FORCED_SWITCH_TICK_KEY = "lastForcedSwitchTick";
    public static final int FORCED_TARGET_COOLDOWN_SECONDS = 12;

    private ParasiteEvents() { }

    public static boolean isParasiteEntity(Entity entity) {
        if (entity == null) return false;
        if (entity.getType().is(ModTags.PARASITE)) return true;
        return entity.getPersistentData().getBoolean("Parasite");
    }

    public static boolean isParasiteByTagOrInterface(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof Player) return HiveLeaderManager.isHiveLeader(entity.getUUID());
        return isParasiteEntity(entity);
    }

    public static boolean isParasiteNoLivingByTagOrInterface(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof Player) return HiveLeaderManager.isHiveLeader(entity.getUUID());
        return isParasiteEntity(entity);
    }

    public static boolean isFriendlyParasite(LivingEntity entity) {
        return isParasiteByTagOrInterface(entity);
    }

    public static boolean shouldIgnoreDamageFrom(LivingEntity self, LivingEntity attacker) {
        if (attacker instanceof Player && HiveLeaderManager.isHiveLeader(attacker.getUUID())) return false;
        return isFriendlyParasite(attacker);
    }

    public static boolean shouldIgnoreTarget(LivingEntity target) {
        return isFriendlyParasite(target);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isParasiteEntity(entity)) return;

        DamageSource source = event.getSource();
        if (source.getEntity() instanceof LivingEntity attacker) {
            if (shouldIgnoreDamageFrom(entity, attacker)) {
                event.setCanceled(true);
                return;
            }
            if (!entity.level().isClientSide) {
                onAttacked(entity, attacker);
            }
        }
        event.setAmount(handleParasiteDamage(entity, source, event.getAmount()));
    }

    public static float handleParasiteDamage(LivingEntity parasite, DamageSource source, float amount) {
        if (isDamageAdaptationInvulnerable(parasite)) return 0.0f;

        if (source.is(DamageTypeTags.IS_FIRE)) {
            int currentTick = (int) parasite.level().getGameTime();
            if (currentTick - getLastRageTriggerTick(parasite) >= 10 && parasite.level().random.nextFloat() < 0.1f) {
                MobEffectInstance currentRage = parasite.getEffect(ModEffects.RAGE.get());
                int amplifier = currentRage != null ? Math.min(currentRage.getAmplifier() + 1, 49) : 0;
                parasite.addEffect(new MobEffectInstance(ModEffects.RAGE.get(), 300, amplifier, false, true, true));
                setLastRageTriggerTick(parasite, currentTick);
            }
        }
        return amount;
    }

    private static int getLastRageTriggerTick(LivingEntity parasite) {
        return parasite.getPersistentData().getInt(LAST_RAGE_TRIGGER_KEY);
    }

    private static void setLastRageTriggerTick(LivingEntity parasite, int tick) {
        parasite.getPersistentData().putInt(LAST_RAGE_TRIGGER_KEY, tick);
    }

    public static boolean hasDamageAdaptationConfig(LivingEntity entity) {
        return getDamageAdaptationConfig(entity) != null;
    }

    public static DamageAdaptationConfig getDamageAdaptationConfig(LivingEntity entity) {
        return org.tdddd.yawning_neko_api.data.AdaptationConfigResolver.getConfig(entity);
    }

    public static boolean isDamageAdaptationInvulnerable(LivingEntity entity) {
        return DamageAdaptation.isInvulnerable(entity);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();
        if (killed == null || killed.level().isClientSide) return;

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity killer)) return;
        if (killer == killed) return;
        if (!isParasiteEntity(killer)) return;

        onKillEntity(killer, killed);
    }

    public static void onKillEntity(LivingEntity killer, LivingEntity killedEntity) {
        if (killer.level().isClientSide) return;
        if (killer.level() instanceof ServerLevel serverLevel) {
            EvolutionManager.forDimension(serverLevel).addPoints(1);
        }
        killer.addEffect(new MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 60, 0, false, false, true));
    }

    public static UUID getFollowTarget(LivingEntity entity) {
        String s = entity.getPersistentData().getString(FOLLOW_TARGET_KEY);
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setFollowTarget(LivingEntity entity, UUID targetUuid) {
        UUID oldTarget = getFollowTarget(entity);
        if (oldTarget == targetUuid || (oldTarget != null && oldTarget.equals(targetUuid))) return;

        if (!entity.level().isClientSide) {
            org.tdddd.epca.impl.events.ParasiteFollowEvent event =
                    new org.tdddd.epca.impl.events.ParasiteFollowEvent(entity, oldTarget, targetUuid);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) return;
        }

        CompoundTag tag = entity.getPersistentData();
        if (targetUuid == null) {
            tag.remove(FOLLOW_TARGET_KEY);
        } else {
            tag.putString(FOLLOW_TARGET_KEY, targetUuid.toString());
        }
    }

    public static void markBiomassSpawned(LivingEntity entity) {
        if (entity == null) return;
        entity.getPersistentData().putBoolean(BIOMASS_SPAWNED_KEY, true);
    }

    public static boolean isBiomassSpawned(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(BIOMASS_SPAWNED_KEY);
    }

    public static UUID getForcedTargetUuid(LivingEntity entity) {
        String uuidStr = entity.getPersistentData().getString(FORCED_TARGET_UUID_KEY);
        if (uuidStr.isEmpty()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setForcedTargetUuid(LivingEntity entity, UUID uuid) {
        CompoundTag tag = entity.getPersistentData();
        if (uuid == null) {
            tag.remove(FORCED_TARGET_UUID_KEY);
        } else {
            tag.putString(FORCED_TARGET_UUID_KEY, uuid.toString());
        }
    }

    public static LivingEntity getForcedTarget(LivingEntity entity, ServerLevel level) {
        UUID uuid = getForcedTargetUuid(entity);
        if (uuid == null) return null;
        Entity found = level.getEntity(uuid);
        if (found instanceof LivingEntity living && living.isAlive() && !isFriendlyParasite(living)) {
            return living;
        }
        setForcedTargetUuid(entity, null);
        return null;
    }

    public static void setForcedTarget(LivingEntity entity, LivingEntity target, ServerLevel level) {
        if (target == null || !target.isAlive()) {
            setForcedTargetUuid(entity, null);
            return;
        }
        setForcedTargetUuid(entity, target.getUUID());
    }

    public static long getLastForcedSwitchTick(LivingEntity entity) {
        return entity.getPersistentData().getLong(LAST_FORCED_SWITCH_TICK_KEY);
    }

    public static void setLastForcedSwitchTick(LivingEntity entity, long tick) {
        entity.getPersistentData().putLong(LAST_FORCED_SWITCH_TICK_KEY, tick);
    }

    public static boolean isForcedTargetSwitchOnCooldown(LivingEntity entity) {
        return (entity.level().getGameTime() - getLastForcedSwitchTick(entity))
                < (FORCED_TARGET_COOLDOWN_SECONDS * 20L);
    }

    public static void onAttacked(LivingEntity self, LivingEntity attacker) {
        if (isFriendlyParasite(attacker)) return;
        trySwitchForcedTargetOnAttacked(self, attacker);
    }

    public static boolean trySwitchForcedTargetOnAttacked(LivingEntity self, LivingEntity attacker) {
        var level = self.level();
        if (level.isClientSide) return false;
        if (isForcedTargetSwitchOnCooldown(self)) return false;
        if (self.distanceToSqr(attacker) > 16.0 * 16.0) return false;
        if (level instanceof ServerLevel serverLevel) {
            setForcedTarget(self, attacker, serverLevel);
            setLastForcedSwitchTick(self, level.getGameTime());
            return true;
        }
        return false;
    }

    public static CompoundTag getVariantData(LivingEntity entity) {
        return new CompoundTag();
    }

    public static void setVariantData(LivingEntity entity, CompoundTag data) {

    }

    public static void applyDifficultyStatModifiers(LivingEntity entity) {
        float multiplier = DifficultyEffects.getParasiteStatMultiplier(entity.level());
        float newMaxHealth = entity.getMaxHealth() * multiplier;
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        entity.setHealth(newMaxHealth);
        if (!(entity instanceof Player)) {
            float newArmor = entity.getArmorValue() * multiplier;
            entity.getAttribute(Attributes.ARMOR).setBaseValue(newArmor);
        }
    }
}
