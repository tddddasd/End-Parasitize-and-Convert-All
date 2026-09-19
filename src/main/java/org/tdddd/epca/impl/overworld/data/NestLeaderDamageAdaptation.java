package org.tdddd.epca.impl.overworld.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NestLeaderDamageAdaptation {
    private static final String ADAPTATION_KEY = "NestLeaderAdaptation";
    private static final String ADAPTATIONS_KEY = "adaptations";
    private static final String DISABLED_UNTIL_KEY = "disabledUntil";
    private static final String LAST_SOUND_TIME_KEY = "AdaptationLastSoundTime";
    private static final long SOUND_COOLDOWN_TICKS = 10;
    private static final int MAX_TYPES = 30;
    private static final long DISABLE_TICKS = 3 * 20;

    private static final Set<ResourceKey<DamageType>> FIRE_TYPES = new HashSet<>();
    static {
        FIRE_TYPES.add(DamageTypes.IN_FIRE);
        FIRE_TYPES.add(DamageTypes.ON_FIRE);
        FIRE_TYPES.add(DamageTypes.LAVA);
        FIRE_TYPES.add(DamageTypes.HOT_FLOOR);
        FIRE_TYPES.add(DamageTypes.FIREBALL);
        FIRE_TYPES.add(DamageTypes.UNATTRIBUTED_FIREBALL);
        FIRE_TYPES.add(DamageTypes.FIREWORKS);
    }

    private static final Set<ResourceKey<DamageType>> UNNADAPTABLE_TYPES = new HashSet<>();
    static {
        UNNADAPTABLE_TYPES.add(DamageTypes.FELL_OUT_OF_WORLD);
        UNNADAPTABLE_TYPES.add(DamageTypes.GENERIC_KILL);
    }

    public static float applyAdaptation(Player player, DamageSource source, float amount) {
        if (amount <= 0) return amount;
        Level level = player.level();
        if (!(level instanceof ServerLevel)) return amount;

        int stage = EvolutionManager.getStageForDimension(level);
        if (stage < 0) return amount;

        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ADAPTATION_KEY);
        long disabledUntil = root.getLong(DISABLED_UNTIL_KEY).orElse(0L);
        long now = level.getGameTime();

        if (now < disabledUntil) return amount;

        ResourceKey<DamageType> typeKey = source.typeHolder().unwrapKey().orElse(null);
        if (typeKey == null) return amount;

        boolean isFire = FIRE_TYPES.contains(typeKey);
        boolean isUnadaptable = UNNADAPTABLE_TYPES.contains(typeKey);

        if (stage < 6 && (isFire || isUnadaptable)) {
            root.putLong(DISABLED_UNTIL_KEY, now + DISABLE_TICKS);
            player.getPersistentData().put(ADAPTATION_KEY, root);
            return amount;
        }

        if (isUnadaptable) {
            return amount;
        }

        CompoundTag adaptMap = root.getCompoundOrEmpty(ADAPTATIONS_KEY);
        String id = typeKey.identifier().toString();
        int currentLevel = adaptMap.getInt(id).orElse(0);
        int newLevel = currentLevel + 1;

        int typeCount = adaptMap.keySet().size();
        if (typeCount >= MAX_TYPES && !adaptMap.contains(id)) {
            List<String> keys = new ArrayList<>(adaptMap.keySet());
            if (!keys.isEmpty()) {
                String removedKey = keys.get(level.getRandom().nextInt(keys.size()));
                adaptMap.remove(removedKey);
            }
        }
        adaptMap.putInt(id, newLevel);
        root.put(ADAPTATIONS_KEY, adaptMap);
        player.getPersistentData().put(ADAPTATION_KEY, root);

        if (!player.level().isClientSide()) {
            long lastSound = player.getPersistentData().getLong(LAST_SOUND_TIME_KEY).orElse(0L);
            if (now - lastSound >= SOUND_COOLDOWN_TICKS) {
                boolean fullAdapted = newLevel >= 7;
                SoundEvent sound = fullAdapted ?
                        ModSoundEvents.FULL_ADAPTATION.get() :
                        ModSoundEvents.PARCIAL_ADAPTATION.get();
                player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.getPersistentData().putLong(LAST_SOUND_TIME_KEY, now);
            }
        }

        double maxReduction = 0.07 + stage * 0.07;
        if (maxReduction > 0.98) maxReduction = 0.98;
        double reduction = (newLevel / 7.0) * maxReduction;
        if (reduction > maxReduction) reduction = maxReduction;

        float reduced = (float) (amount * (1.0 - reduction));
        return Math.max(reduced, 0);
    }
}