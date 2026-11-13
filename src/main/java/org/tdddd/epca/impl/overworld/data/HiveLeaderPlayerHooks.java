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

public final class HiveLeaderPlayerHooks {

    private static final String LAST_RAGE_TRIGGER_KEY = "lastRageTrigger";

    private HiveLeaderPlayerHooks() {
    }

    public static boolean isHiveLeader(Player player) {
        return player != null && HiveLeaderManager.isHiveLeader(player.getUUID());
    }

    public static void onKill(Player player, LivingEntity killed) {
        if (!isHiveLeader(player)) return;
        Level level = player.level();
        if (level instanceof ServerLevel serverLevel) {
            EvolutionManager.forDimension(serverLevel).addPoints(1);
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
    }

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

    public static boolean hasDamageAdaptationConfig(Player player) {
        if (!isHiveLeader(player)) return false;
        return getDamageAdaptationConfig(player) != null;
    }

    public static DamageAdaptationConfig getDamageAdaptationConfig(Player player) {
        if (!isHiveLeader(player)) return null;
        return org.tdddd.yawning_neko_api.data.AdaptationConfigResolver.getConfig(player);
    }

    public static boolean isDamageAdaptationInvulnerable(Player player) {
        return isHiveLeader(player) && DamageAdaptation.isInvulnerable(player);
    }

    public static void applyDifficultyStatModifiers(Player player) {
        if (!isHiveLeader(player)) return;
        float multiplier = org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects
                .getParasiteStatMultiplier(player.level());
        float newMaxHealth = player.getMaxHealth() * multiplier;
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        player.setHealth(newMaxHealth);
    }
}
