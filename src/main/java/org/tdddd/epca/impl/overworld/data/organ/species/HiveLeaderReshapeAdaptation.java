package org.tdddd.epca.impl.overworld.data.organ.species;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 领巢者「重塑种」专属的伤害适应性（种阶特化唯一保留的适应性来源）。
 *
 * <h2>语义（参照 YNA {@code DamageAdaptation}）</h2>
 * <ul>
 *   <li>按 <strong>DamageType</strong>（{@code minecraft:player_attack} / {@code mob_attack} /
 *       {@code arrow} / {@code explosion} / {@code fall} …）分别累计「适应层数」；</li>
 *   <li>每次实际受到某类型伤害时，以 {@link #ADAPT_CHANCE} 的概率给该类型 +1 层；</li>
 *   <li>每层提供 {@link #REDUCTION_PER_STACK} 的减伤，单类型封顶 {@link #MAX_REDUCTION}；</li>
 *   <li>最多同时适应 {@link #MAX_ADAPTED_TYPES} 种伤害类型（已适应的类型可以继续叠层，不占新名额）；</li>
 *   <li>触发适应的那一次伤害<strong>立即</strong>享受新层数的减伤（与 YNA 一致）。</li>
 * </ul>
 *
 * <h2>黑名单（不累计、也不减伤）</h2>
 * 见 {@link #BLOCKED_TAGS} / {@link #BLOCKED_TYPES}：火焰类（{@code #minecraft:is_fire}）、
 * {@code minecraft:generic_kill}（/kill）、虚空（{@code minecraft:out_of_world}，即
 * {@code DamageTypes.FELL_OUT_OF_WORLD}）。这些伤害仍按原版与其它减伤规则正常结算。
 *
 * <h2>数据</h2>
 * 层数存在 {@link HiveLeaderSpeciesState}（玩家 {@code persistentData} 的
 * {@code HiveLeaderSpecies} 复合标签）里：换维度 / 重登都不丢；种阶离开重塑种时数据保留但失效，
 * 回到重塑种自动恢复。客户端通过 {@code SyncHiveLeaderSpeciesPacket} 拿到只读副本用于读数。
 *
 * <h2>结算顺序</h2>
 * 在 {@code LivingHurtEvent}（{@code HiveLeaderOrganDamageHandler#onHiveLeaderHurt}）里乘算：
 * {@code amount * organMultiplier * (1 - reduction)}。Forge 1.20.1 的护甲吸收
 * （{@code getDamageAfterArmorAbsorb}）、附魔/抗性（{@code getDamageAfterMagicAbsorb}）都在
 * {@code LivingHurtEvent} <strong>之后</strong>结算，所以本减伤与护甲、其它减伤天然<strong>叠乘</strong>，
 * 不会与任何器官适应重复（器官适应已删除）。
 */
public final class HiveLeaderReshapeAdaptation {

    // ===================== 可调参数（集中在此处修改） =====================

    /** 每次受到某类型伤害时，累计 1 层适应的概率。 */
    public static final float ADAPT_CHANCE = 0.70F;

    /** 每层适应提供的减伤比例（5%）。 */
    public static final double REDUCTION_PER_STACK = 0.05D;

    /** 单类型减伤上限（80%）。 */
    public static final double MAX_REDUCTION = 0.60D;

    /** 单类型最多可累计的适应层数（= 80% / 5% = 16 层）。 */
    public static final int MAX_STACKS_PER_TYPE = 12;

    /** 最多可同时适应的伤害类型数量。 */
    public static final int MAX_ADAPTED_TYPES = 5;

    /**
     * 适应性黑名单（标签）：火焰类伤害（{@code #minecraft:is_fire}，涵盖 in_fire / on_fire /
     * lava / hot_floor / campfire / fireball …）。
     */
    private static final List<TagKey<DamageType>> BLOCKED_TAGS = List.of(DamageTypeTags.IS_FIRE);

    /**
     * 适应性黑名单（精确 DamageType）：
     * {@code minecraft:generic_kill}（/kill，{@link DamageTypes#GENERIC_KILL}）与
     * {@code minecraft:out_of_world}（虚空，1.20.1 里该 DamageType 的常量名是
     * {@link DamageTypes#FELL_OUT_OF_WORLD}）。
     */
    private static final Set<ResourceKey<DamageType>> BLOCKED_TYPES = Set.of(
            DamageTypes.GENERIC_KILL,
            DamageTypes.FELL_OUT_OF_WORLD);

    private HiveLeaderReshapeAdaptation() {
    }

    // ===================== 黑名单判定 =====================

    /**
     * 这次伤害是否可以参与适应（黑名单集中在这里，便于以后扩展）。
     *
     * <p>返回 false 时：不累计层数、也不产生任何适应减伤，但伤害本身照常结算。</p>
     */
    public static boolean isAdaptable(@Nullable DamageSource source) {
        if (source == null) return false;
        for (TagKey<DamageType> tag : BLOCKED_TAGS) {
            if (tag != null && source.is(tag)) return false;
        }
        for (ResourceKey<DamageType> key : BLOCKED_TYPES) {
            if (key != null && source.is(key)) return false;
        }
        return true;
    }

    /** 已持久化的 key（DamageType id）是否仍在黑名单里（读取/显示时过滤掉历史脏数据）。 */
    public static boolean isAdaptableKey(@Nullable Player player, @Nullable String key) {
        if (key == null || key.isBlank()) return false;
        ResourceLocation location = ResourceLocation.tryParse(key);
        if (location == null) return false;
        ResourceKey<DamageType> typeKey = ResourceKey.create(Registries.DAMAGE_TYPE, location);
        if (BLOCKED_TYPES.contains(typeKey)) return false;
        if (player == null) return true;
        Registry<DamageType> registry = player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE);
        if (registry == null) return true;
        Holder<DamageType> holder = registry.getHolder(typeKey).orElse(null);
        if (holder == null) return true;
        for (TagKey<DamageType> tag : BLOCKED_TAGS) {
            if (tag != null && holder.is(tag)) return false;
        }
        return true;
    }

    // ===================== 数值 =====================

    /** 层数 → 该类型的减伤比例（0..{@link #MAX_REDUCTION}）。 */
    public static double reductionFor(int stacks) {
        if (stacks <= 0) return 0.0D;
        return Math.min(MAX_REDUCTION, stacks * REDUCTION_PER_STACK);
    }

    /** 某类型当前的减伤比例；黑名单类型恒为 0。 */
    public static double reductionFor(@Nullable Player player, @Nullable HiveLeaderSpeciesState state,
                                      @Nullable String key) {
        if (state == null || key == null) return 0.0D;
        if (!isAdaptableKey(player, key)) return 0.0D;
        return reductionFor(state.adaptationLevel(key));
    }

    /** 当前已适应的类型数量（只统计仍在黑名单之外的条目）。 */
    public static int adaptedTypeCount(@Nullable Player player, @Nullable HiveLeaderSpeciesState state) {
        return adaptableEntries(player, state).size();
    }

    /**
     * 适合同步/显示的快照：只保留层数 &gt; 0 且不在黑名单里的条目，按层数从高到低排序。
     */
    public static Map<String, Integer> adaptableEntries(@Nullable Player player,
                                                        @Nullable HiveLeaderSpeciesState state) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (state == null) return result;
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : state.adaptationLevels().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) continue;
            if (!isAdaptableKey(player, entry.getKey())) continue;
            entries.add(Map.entry(entry.getKey(), entry.getValue()));
        }
        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));
        for (Map.Entry<String, Integer> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    // ===================== 结算 =====================

    /**
     * 受击结算：先按概率累计适应层数，再把减伤乘到 {@code amount} 上。
     *
     * <p>只应由「当前是重塑种的领巢者」调用（由 {@code HiveLeaderSpeciesManager#modifyIncomingDamage}
     * 把关）。</p>
     */
    public static float adaptAndReduce(@Nullable Player player, @Nullable HiveLeaderSpeciesState state,
                                       @Nullable DamageSource source, float amount) {
        if (player == null || state == null || source == null || amount <= 0.0F) return amount;
        if (!isAdaptable(source)) return amount;

        String key = keyOf(source);
        if (key == null) return amount;

        int stacks = Math.max(0, state.adaptationLevel(key));
        if (stacks < MAX_STACKS_PER_TYPE && tryAdapt(player, state, key, stacks)) {
            stacks++;
            state.setAdaptationLevel(key, stacks);
        }

        double reduction = reductionFor(stacks);
        if (reduction <= 0.0D) return amount;
        return (float) Math.max(0.0D, amount * (1.0D - reduction));
    }

    private static boolean tryAdapt(Player player, HiveLeaderSpeciesState state, String key, int currentStacks) {
        // 类型数量上限只约束「新类型」；已适应的类型可以继续叠层。
        if (currentStacks <= 0 && adaptedTypeCount(player, state) >= MAX_ADAPTED_TYPES) return false;
        return player.getRandom().nextFloat() < ADAPT_CHANCE;
    }

    /** 伤害来源 → 持久化 key（伤害类型注册表 id；没有注册表项时退回 msgId）。 */
    @Nullable
    public static String keyOf(@Nullable DamageSource source) {
        if (source == null) return null;
        ResourceLocation location = source.typeHolder().unwrapKey()
                .map(ResourceKey::location).orElse(null);
        if (location != null) return location.toString();
        String msgId = source.getMsgId();
        return msgId == null || msgId.isBlank() ? null : msgId;
    }

    // ===================== 数据清理 =====================

    /**
     * 登录 / 换维度 / 成为领巢者时调用：把黑名单类型或非法 key 的历史脏数据从持久化里删掉
     * （即使旧版本因为 bug 记录过，也不会出现在已适应列表里）。
     */
    public static void sanitize(@Nullable Player player, @Nullable HiveLeaderSpeciesState state) {
        if (state == null) return;
        List<String> stale = new ArrayList<>();
        for (String key : state.adaptationLevels().keySet()) {
            if (!isAdaptableKey(player, key)) stale.add(key);
        }
        for (String key : stale) {
            state.removeAdaptation(key);
        }
    }

    /** 调试/读数用的可读描述。 */
    public static String describe(@Nullable Player player, @Nullable HiveLeaderSpeciesState state) {
        Map<String, Integer> entries = adaptableEntries(player, state);
        StringBuilder builder = new StringBuilder();
        builder.append(entries.size()).append('/').append(MAX_ADAPTED_TYPES).append(' ');
        entries.forEach((key, stacks) -> builder.append(key).append('=')
                .append(Math.round(reductionFor(stacks) * 100.0D)).append("% "));
        return builder.toString().trim();
    }
}
