package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatDefaults;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;

import javax.annotation.Nullable;

/**
 * 器官提供的「受伤倍率」结算（原 {@code HiveLeaderOrganAdaptation} 的减法部分）。
 *
 * <p>这里<strong>只剩受伤倍率</strong>（{@code damage_taken} / {@code damage_taken_add} 数据），
 * 也就是「某类伤害对领巢者造成的倍率」这一项器官属性。器官提供的<strong>伤害适应性</strong>
 * 已在 2026-11 彻底移除，不再由任何器官提供，也不再写入玩家持久化数据。</p>
 *
 * <p>适应性的唯一来源改为种阶特化：见
 * {@link org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderReshapeAdaptation}。</p>
 */
public final class HiveLeaderOrganDamageTaken {

    private HiveLeaderOrganDamageTaken() {
    }

    /**
     * 按伤害类型查表得到本器官构成下的受伤倍率，并乘到 {@code amount} 上。
     *
     * <p>只做倍率乘法，<strong>不含</strong>任何适应进度 / 适应减伤。</p>
     */
    public static float apply(@Nullable OrganStatSummary summary, @Nullable DamageSource source, float amount) {
        if (source == null || amount <= 0.0F) return amount;
        double multiplier = multiplierFor(summary, source);
        if (multiplier == 1.0D) return amount;
        return (float) Math.max(0.0D, amount * multiplier);
    }

    /**
     * 器官「受伤倍率」：命中精确 DamageType id 或伤害标签时取表里的倍率，否则用兜底倍率。
     */
    public static double multiplierFor(@Nullable OrganStatSummary summary, @Nullable DamageSource source) {
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

    @Nullable
    private static String typeIdOf(DamageSource source) {
        ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
        Identifier location = key == null ? null : key.identifier();
        return location != null ? location.toString() : source.getMsgId();
    }

    @Nullable
    private static TagKey<DamageType> damageTag(String key) {
        if (key == null) return null;
        String text = key.startsWith("#") ? key.substring(1) : key;
        Identifier location = Identifier.tryParse(text);
        return location == null ? null : TagKey.create(Registries.DAMAGE_TYPE, location);
    }
}
