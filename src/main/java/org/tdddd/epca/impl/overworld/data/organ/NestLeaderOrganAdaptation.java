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

/**
 * STAGE 3 <b></b>SPEC  0  C9  D6
 *  {@code NestLeaderDamageAdaptation}  STAGE 1  SPEC A2
 * <b></b>
 *
 * <h2></h2>
 * <ul>
 *   <li>{@link OrganStatSummary#damageTakenMultipliers()} tag id
 *        2.5 1STAGE A
 *       {@code _defaults.json}  +  {@code damage_taken_add}
 *        +0.05 -0.01
 *        {@code LivingHurtEvent}  {@code event.setAmount(...)} </li>
 *   <li>{@link OrganStatSummary#adaptationReduction()}<b></b>
 *       STAGE A
 *       {@code OrganStatSummary#compute}</li>
 *   <li>{@link OrganStatSummary#adaptationRequiredHits()} /
 *       {@link OrganStatSummary#adaptationRequiredHitsTags()}</li>
 *   <li>{@link OrganStatSummary#adaptationChance()} 1.0 = 100%</li>
 *   <li>{@link OrganStatSummary#nonAdaptableDamageTypes()}
 *        {@code minecraft:generic_kill}{@code minecraft:out_of_world}{@code #minecraft:is_fire}</li>
 * </ul>
 *
 * <h2> 0 </h2>
 * <p>SPEC
 * <b></b></p>
 * <pre>
 *   cap(type)        =  requiredHits / requiredHitsTags
 *                      JSON  {@code adaptation_rounds}
 *                      <b></b>
 *                      4+8+12+16=40
 *   progress(type)   = min(1, hits(type) / cap(type))
 *   reduction(type)  = adaptationReduction() * progress(type)
 *             =  * (1 - reduction(type))
 * </pre>
 * <p> cap
 * cap  0 progress  0
 * <b></b>SPEC </p>
 *
 * <h2> max </h2>
 * <p>{@code OrganStatSummary} <b></b>
 *  {@code OrganStatSummary}  231-243  {@code merge(..., Integer::sum)}
 *  {@code roundsForTiers}
 * {@code {"0.25":4,"0.5":8,"0.75":12,"1.0":16}}  1  4
 *  2  8  12 3  12  24 4  16  40
 * 4/8/12/16 16  cap
 *  16  = <b></b></p>
 *
 * <h2></h2>
 * <p> {@code NestLeaderOrganAdaptation}
 * {@code Player#getPersistentData()}Forge  {@code Entity} </p>
 * <pre>
 *   NestLeaderOrganAdaptation
 *     hits : CompoundTag      //  id -> int
 *     tick : long             //
 * </pre>
 * <p><b></b>
 * </p>
 *
 * <p><b></b>
 *  {@code NestLeaderOrganSavedData}{@code nestleader_organs.dat} UUID
 * <b></b>
 *
 *  UUID </p>
 */
public final class NestLeaderOrganAdaptation {

    /**  */
    public static final String PERSISTENT_KEY = "NestLeaderOrganAdaptation";
    private static final String TAG_HITS = "hits";

    private NestLeaderOrganAdaptation() {
    }


    /**
     *  {@code amount}
     *
     * <p> {@code NestLeaderOrganDamageHandler#onNestLeaderHurt}{@code LivingHurtEvent}
     *  {@code LivingHurtEvent#setAmount(float)}</p>
     *
     * @param player
     * @param source
     * @param summary  compute
     * @param amount   {@code LivingHurtEvent}
     * @return  0
     */
    public static float resolve(Player player, DamageSource source, OrganStatSummary summary, float amount) {
        if (player == null || source == null || summary == null || amount <= 0.0F) return amount;

        //   2.5 1
        float result = (float) (amount * multiplierFor(summary, source));

        if (isAdaptable(summary, source)) {
            ResourceKey<DamageType> key = typeKeyOf(source);
            if (key != null) {
                int cap = requiredHits(summary, source);
                if (cap > 0) {
                    String id = key.location().toString();
                    //  NBT cap
                    CompoundTag hits = hitsTag(player);
                    int current = hits.getInt(id);
                    double progress = Math.min(1.0D, (double) current / (double) cap);
                    double reduction = clamp01(summary.adaptationReduction()) * progress;
                    result = (float) (result * (1.0D - reduction));

                    //   adaptation_chance  100%
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

    //  /

    /**  id  */
    public static int progress(Player player, String damageTypeId) {
        if (player == null || damageTypeId == null) return 0;
        return hitsTag(player).getInt(damageTypeId);
    }

    /**  */
    public static void clear(Player player) {
        if (player == null) return;
        CompoundTag root = player.getPersistentData().getCompound(PERSISTENT_KEY);
        root.remove(TAG_HITS);
        player.getPersistentData().put(PERSISTENT_KEY, root);
    }


    /**
     *  {@link OrganStatDefaults#damageTaken()}
     * STAGE A  {@code damage_taken_add}
     * {@link OrganStatSummary#damageTakenMultipliers()} tag  id
     *
     * <p>{@code #}  {@link DamageSource#is(TagKey)}
     * <b> id</b>  id  {@link DamageSource#getMsgId()}
     *  msgId  {@code inFire}  msgId
     *
     * {@link OrganStatSummary#damageTakenFallbackMultiplier()}
     * {@code fallback}  -0.01
     *  1.0</p>
     */
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

    /**
     *
     *
     * <p>{@code minecraft:generic_kill}kill
     * {@code minecraft:out_of_world}{@code #minecraft:is_fire}
     *  tag  {@link DamageSource#is(TagKey)} id
     * {@link DamageSource#getMsgId()} </p>
     */
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

    /**
     *
     * {@link OrganStatSummary#adaptationRequiredHits()} id
     * {@link OrganStatSummary#adaptationRequiredHitsTags()} tag<b></b>
     *  javadoc 0 =
     */
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

    /**  id {@code minecraft:in_fire} {@link DamageSource#getMsgId()} */
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

