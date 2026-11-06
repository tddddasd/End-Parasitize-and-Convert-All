package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatDefaults;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;

import javax.annotation.Nullable;
import java.util.Map;

public final class HiveLeaderOrganAdaptation {

    public static final String PERSISTENT_KEY = "HiveLeaderOrganAdaptation";
    private static final String TAG_HITS = "hits";

    private HiveLeaderOrganAdaptation() {
    }

    public static float resolve(Player player, DamageSource source, OrganStatSummary summary, float amount) {
        if (player == null || source == null || summary == null || amount <= 0.0F) return amount;

        float result = (float) (amount * multiplierFor(summary, source));

        if (isAdaptable(summary, source)) {
            ResourceKey<DamageType> key = typeKeyOf(source);
            if (key != null) {
                int cap = requiredHits(summary, source);
                if (cap > 0) {
                    String id = key.location().toString();

                    CompoundTag hits = hitsTag(player);
                    int current = hits.getInt(id);
                    double progress = Math.min(1.0D, (double) current / (double) cap);
                    double reduction = clamp01(summary.adaptationReduction()) * progress;
                    result = (float) (result * (1.0D - reduction));

                    if (player.level().random.nextFloat() < clamp01(summary.adaptationChance())) {
                        hits.putInt(id, current + 1);
                        CompoundTag root = player.getPersistentData().getCompound(PERSISTENT_KEY);
                        root.put(TAG_HITS, hits);
                        player.getPersistentData().put(PERSISTENT_KEY, root);
                    }
                }
            }
        }

        return Math.max(0.0F, result);
    }

    public static int progress(Player player, String damageTypeId) {
        if (player == null || damageTypeId == null) return 0;
        return hitsTag(player).getInt(damageTypeId);
    }

    public static void clear(Player player) {
        if (player == null) return;
        CompoundTag root = player.getPersistentData().getCompound(PERSISTENT_KEY);
        root.remove(TAG_HITS);
        player.getPersistentData().put(PERSISTENT_KEY, root);
    }

    public static double multiplierFor(OrganStatSummary summary, DamageSource source) {
        if (summary == null || source == null) return OrganStatDefaults.DEFAULT_OTHER_MULTIPLIER;
        String id = typeIdOf(source);
        String msgId = source.getMsgId();
        for (OrganStatSummary.DamageTakenDisplay entry : summary.damageTakenMultipliers()) {
            if (entry.fallback()) continue;
            String key = entry.key();
            if (key == null) continue;
            if (key.startsWith("#")) {
                TagKey<DamageType> tag = damageTag(key);
                if (tag != null && source.is(tag)) return entry.multiplier();
            } else if ((id != null && key.equals(id))
                    || (msgId != null && key.equals(msgId))) {
                return entry.multiplier();
            }
        }
        return summary.damageTakenFallbackMultiplier();
    }

    public static boolean isAdaptable(OrganStatSummary summary, DamageSource source) {
        if (summary == null || source == null) return false;
        String id = typeIdOf(source);
        for (String entry : summary.nonAdaptableDamageTypes()) {
            if (entry == null || entry.isBlank()) continue;
            if (entry.startsWith("#")) {
                TagKey<DamageType> tag = damageTag(entry);
                if (tag != null && source.is(tag)) return false;
            } else if (id != null && entry.equals(id)) {
                return false;
            }
        }
        return true;
    }

    public static int requiredHits(OrganStatSummary summary, DamageSource source) {
        if (summary == null || source == null) return 0;
        String id = typeIdOf(source);
        int best = 0;
        if (id != null) {
            Integer exact = summary.adaptationRequiredHits().get(id);
            if (exact != null) best = Math.max(best, exact);
        }
        for (Map.Entry<String, Integer> entry : summary.adaptationRequiredHitsTags().entrySet()) {
            TagKey<DamageType> tag = damageTag(entry.getKey());
            if (tag != null && source.is(tag)) {
                best = Math.max(best, entry.getValue());
            }
        }
        return Math.max(0, best);
    }

    private static CompoundTag hitsTag(Player player) {
        return player.getPersistentData().getCompound(PERSISTENT_KEY).getCompound(TAG_HITS);
    }

    @Nullable
    private static String typeIdOf(DamageSource source) {
        ResourceKey<DamageType> key = typeKeyOf(source);
        return key != null ? key.location().toString() : source.getMsgId();
    }

    @Nullable
    private static ResourceKey<DamageType> typeKeyOf(DamageSource source) {
        return source.typeHolder().unwrapKey().orElse(null);
    }

    @Nullable
    private static TagKey<DamageType> damageTag(String key) {
        if (key == null) return null;
        String text = key.startsWith("#") ? key.substring(1) : key;
        ResourceLocation location = ResourceLocation.tryParse(text);
        return location == null ? null : TagKey.create(Registries.DAMAGE_TYPE, location);
    }

    private static double clamp01(double value) {
        if (value <= 0.0D) return 0.0D;
        return Math.min(1.0D, value);
    }
}
