package org.tdddd.epca.impl.overworld.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptationManager;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Player side behaviour of a hive leader.
 *
 * <p>Players are no longer {@code IParasite} instances. Everything a hive leader gets is decided by
 * {@link HiveLeaderManager} (world saved data) and applied from the event handlers that call these
 * helpers, so nothing about the player has to be an interface implementation any more.</p>
 *
 * <p>The damage adaptation keeps its player specific behaviour: a hive leader still picks up the
 * {@code yawning_neko_api} adaptation config of a player entity type and the fake-death handling that
 * comes with it.</p>
 */
public final class HiveLeaderPlayerHooks {

    private static final String LAST_RAGE_TRIGGER_KEY = "lastRageTrigger";

    private HiveLeaderPlayerHooks() {
    }

    /** True when this player is a hive leader according to the world saved data. */
    public static boolean isHiveLeader(Player player) {
        return player != null && HiveLeaderManager.isHiveLeader(player.getUUID());
    }

    /** Awarded on a kill: one evolution point, plus a short regeneration for the leader. */
    public static void onKill(Player player, LivingEntity killed) {
        if (!isHiveLeader(player)) return;
        Level level = player.level();
        if (level instanceof ServerLevel serverLevel) {
            EvolutionManager.forDimension(serverLevel).addPoints(1);
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
    }

    /** Fire damage can make a leader angry; everything else passes through unchanged. */
    public static float onHurt(Player player, DamageSource source, float amount) {
        if (!isHiveLeader(player)) return amount;
        Level level = player.level();
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && level.random.nextFloat() < 0.1f) {
            int currentTick = (int) level.getGameTime();
            CompoundTag tag = player.getPersistentData();
            if (currentTick - tag.getInt(LAST_RAGE_TRIGGER_KEY) >= 10) {
                MobEffectInstance current = player.getEffect(ModEffects.RAGE.get());
                int amplifier = current != null ? Math.min(current.getAmplifier() + 1, 49) : 0;
                player.addEffect(new MobEffectInstance(ModEffects.RAGE.get(), 300, amplifier, false, true, true));
                tag.putInt(LAST_RAGE_TRIGGER_KEY, currentTick);
            }
        }
        return amount;
    }

    // ---- damage adaptation: player specific logic is kept here ----

    public static boolean hasDamageAdaptationConfig(Player player) {
        if (!isHiveLeader(player)) return false;
        return DamageAdaptationManager.hasConfig(ForgeRegistries.ENTITY_TYPES.getKey(player.getType()));
    }

    public static DamageAdaptationConfig getDamageAdaptationConfig(Player player) {
        if (!isHiveLeader(player)) return null;
        return DamageAdaptationManager.getConfig(ForgeRegistries.ENTITY_TYPES.getKey(player.getType()));
    }

    public static boolean isDamageAdaptationInvulnerable(Player player) {
        return isHiveLeader(player) && DamageAdaptation.isInvulnerable(player);
    }

    /** Applies the difficulty stat multipliers to a leader, once. */
    public static void applyDifficultyStatModifiers(Player player) {
        if (!isHiveLeader(player)) return;
        float multiplier = org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects
                .getParasiteStatMultiplier(player.level());
        float newMaxHealth = player.getMaxHealth() * multiplier;
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        player.setHealth(newMaxHealth);
    }
}
