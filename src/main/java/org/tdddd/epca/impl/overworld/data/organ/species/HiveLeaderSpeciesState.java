package org.tdddd.epca.impl.overworld.data.organ.species;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 单个领巢者的特化状态（持久化在玩家持久化数据里）：
 *
 * <ul>
 *   <li>{@code Species} —— 当前特化种阶（{@link HiveLeaderSpecies#id()}）；</li>
 *   <li>{@code Kills} —— 朝「重塑种」进化累计的击杀数（0..25）；</li>
 *   <li>{@code NightVisionCooldown} —— 夜视刷新倒计时（每 80 tick 施加 200 tick 夜视）；</li>
 *   <li>{@code MinDamage} —— 攻击最小伤害冷却：目标 UUID → 最近触发 gameTime（1 秒/目标）；</li>
 *   <li>{@code AbilityCooldowns} —— 两个主动按键的冷却结束时间；</li>
 *   <li>{@code Adaptation} —— 重塑种伤害适应性：DamageType id → 已适应层数。</li>
 * </ul>
 *
 * <p>{@code Adaptation} 是持久养成的进度（换维度 / 重登不丢，与击杀数同级）；离开重塑种时数据保留但
 * 不参与结算，回到重塑种后自动恢复。黑名单（火焰 / kill / 虚空）的条目会在登录时由
 * {@link HiveLeaderReshapeAdaptation#sanitize} 清掉。</p>
 *
 * <p>实例由 {@code HiveLeaderSpeciesManager} 按 UUID 缓存，变更后（每 20 tick 或立刻）写回 NBT，
 * 避免每 tick 都写持久化数据。</p>
 */
public final class HiveLeaderSpeciesState {

    public static final String PERSISTENT_KEY = "HiveLeaderSpecies";

    private static final String TAG_SPECIES = "Species";
    private static final String TAG_KILLS = "Kills";
    private static final String TAG_NIGHT_VISION_COOLDOWN = "NightVisionCooldown";
    private static final String TAG_MIN_DAMAGE = "MinDamage";
    private static final String TAG_ABILITY_COOLDOWNS = "AbilityCooldowns";
    private static final String TAG_LAST_KILL_TICK = "LastKillTick";
    private static final String TAG_ADAPTATION = "Adaptation";

    /** 最小伤害冷却记录最多保留多少个目标，避免长期在线无限增长。 */
    private static final int MAX_MIN_DAMAGE_ENTRIES = 32;

    private HiveLeaderSpecies species = HiveLeaderSpecies.INFESTED;
    private int kills;
    private int nightVisionCooldown;
    private long lastKillTick;
    private final Map<UUID, Long> lastMinDamageTick = new LinkedHashMap<>();
    private final Map<String, Long> abilityCooldownEnd = new LinkedHashMap<>();

    /** 重塑种伤害适应性：DamageType id → 已适应层数（只在重塑种时参与结算）。 */
    private final Map<String, Integer> adaptationLevels = new LinkedHashMap<>();

    /** 适应进度版本号：任何层数变化 +1，用于同步签名判断（避免每 tick 拼字符串）。 */
    private int adaptationVersion;

    private boolean dirty;
    private String lastSyncedSignature = "";

    private HiveLeaderSpeciesState() {
    }

    public static HiveLeaderSpeciesState load(@Nullable Player player) {
        HiveLeaderSpeciesState state = new HiveLeaderSpeciesState();
        if (player == null) return state;

        CompoundTag root = player.getPersistentData().getCompound(PERSISTENT_KEY);
        if (!root.isEmpty()) {
            HiveLeaderSpecies stored = HiveLeaderSpecies.byId(root.getString(TAG_SPECIES));
            if (stored != null) state.species = stored;
            state.kills = Math.max(0, root.getInt(TAG_KILLS));
            state.nightVisionCooldown = Math.max(0, root.getInt(TAG_NIGHT_VISION_COOLDOWN));
            state.lastKillTick = root.getLong(TAG_LAST_KILL_TICK);

            CompoundTag minDamage = root.getCompound(TAG_MIN_DAMAGE);
            for (String key : minDamage.getAllKeys()) {
                UUID target = parseUuid(key);
                if (target == null) continue;
                state.lastMinDamageTick.put(target, minDamage.getLong(key));
            }

            CompoundTag cooldowns = root.getCompound(TAG_ABILITY_COOLDOWNS);
            for (String key : cooldowns.getAllKeys()) {
                state.abilityCooldownEnd.put(key, cooldowns.getLong(key));
            }

            CompoundTag adaptation = root.getCompound(TAG_ADAPTATION);
            for (String key : adaptation.getAllKeys()) {
                int stacks = adaptation.getInt(key);
                if (key == null || key.isBlank() || stacks <= 0) continue;
                state.adaptationLevels.put(key, stacks);
            }
        }
        state.dirty = false;
        return state;
    }

    public void save(@Nullable Player player) {
        if (player == null) return;

        CompoundTag root = new CompoundTag();
        root.putString(TAG_SPECIES, species.id());
        root.putInt(TAG_KILLS, kills);
        root.putInt(TAG_NIGHT_VISION_COOLDOWN, nightVisionCooldown);
        root.putLong(TAG_LAST_KILL_TICK, lastKillTick);

        CompoundTag minDamage = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : lastMinDamageTick.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            minDamage.putLong(entry.getKey().toString(), entry.getValue());
        }
        root.put(TAG_MIN_DAMAGE, minDamage);

        CompoundTag cooldowns = new CompoundTag();
        for (Map.Entry<String, Long> entry : abilityCooldownEnd.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            cooldowns.putLong(entry.getKey(), entry.getValue());
        }
        root.put(TAG_ABILITY_COOLDOWNS, cooldowns);

        CompoundTag adaptation = new CompoundTag();
        for (Map.Entry<String, Integer> entry : adaptationLevels.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) continue;
            adaptation.putInt(entry.getKey(), entry.getValue());
        }
        root.put(TAG_ADAPTATION, adaptation);

        player.getPersistentData().put(PERSISTENT_KEY, root);
        dirty = false;
    }

    public void delete(@Nullable Player player) {
        if (player == null) return;
        if (player.getPersistentData().contains(PERSISTENT_KEY, Tag.TAG_COMPOUND)) {
            player.getPersistentData().remove(PERSISTENT_KEY);
        }
        dirty = false;
    }

    @Nullable
    private static UUID parseUuid(@Nullable String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public HiveLeaderSpecies species() {
        return species;
    }

    public void setSpecies(HiveLeaderSpecies species) {
        if (species == null || species == this.species) return;
        this.species = species;
        this.dirty = true;
    }

    public int kills() {
        return kills;
    }

    public void setKills(int kills) {
        int clamped = Math.max(0, kills);
        if (this.kills == clamped) return;
        this.kills = clamped;
        this.dirty = true;
    }

    public int addKill() {
        this.kills = Math.min(999, this.kills + 1);
        this.dirty = true;
        return this.kills;
    }

    public long lastKillTick() {
        return lastKillTick;
    }

    public void setLastKillTick(long tick) {
        this.lastKillTick = tick;
        this.dirty = true;
    }

    public int nightVisionCooldown() {
        return nightVisionCooldown;
    }

    public void setNightVisionCooldown(int value) {
        int clamped = Math.max(0, value);
        if (this.nightVisionCooldown == clamped) return;
        this.nightVisionCooldown = clamped;
        this.dirty = true;
    }

    @Nullable
    public Long lastMinDamageTick(@Nullable UUID target) {
        return target == null ? null : lastMinDamageTick.get(target);
    }

    public void markMinDamage(@Nullable UUID target, long tick) {
        if (target == null) return;
        lastMinDamageTick.put(target, tick);
        while (lastMinDamageTick.size() > MAX_MIN_DAMAGE_ENTRIES) {
            UUID oldest = lastMinDamageTick.keySet().iterator().next();
            lastMinDamageTick.remove(oldest);
        }
        this.dirty = true;
    }

    public long abilityCooldownEnd(String key) {
        Long value = abilityCooldownEnd.get(key);
        return value == null ? 0L : value;
    }

    public void setAbilityCooldownEnd(String key, long tick) {
        if (key == null) return;
        abilityCooldownEnd.put(key, tick);
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    // ===================== 重塑种伤害适应性 =====================

    /**
     * 全部已持久化的适应层数（DamageType id → 层数）。
     *
     * <p>这是<strong>原始数据</strong>（可能含旧版本/黑名单脏数据）：对外显示与结算请用
     * {@link HiveLeaderReshapeAdaptation#adaptableEntries} / {@code isAdaptableKey} 过滤。</p>
     */
    public Map<String, Integer> adaptationLevels() {
        return adaptationLevels;
    }

    /** 某伤害类型当前累计的适应层数（0 表示还没适应过）。 */
    public int adaptationLevel(String key) {
        if (key == null) return 0;
        Integer value = adaptationLevels.get(key);
        return value == null ? 0 : Math.max(0, value);
    }

    /** 写入某伤害类型的适应层数（&lt;= 0 等价于删除）。 */
    public void setAdaptationLevel(String key, int stacks) {
        if (key == null || key.isBlank()) return;
        if (stacks <= 0) {
            removeAdaptation(key);
            return;
        }
        Integer previous = adaptationLevels.put(key, stacks);
        if (previous != null && previous == stacks) return;
        adaptationVersion++;
        this.dirty = true;
    }

    /** 删除某伤害类型的适应记录（黑名单清理用）。 */
    public void removeAdaptation(String key) {
        if (key == null) return;
        if (adaptationLevels.remove(key) == null) return;
        adaptationVersion++;
        this.dirty = true;
    }

    /** 清空全部适应进度（调试 / 需要回滚时用）。 */
    public void clearAdaptations() {
        if (adaptationLevels.isEmpty()) return;
        adaptationLevels.clear();
        adaptationVersion++;
        this.dirty = true;
    }

    /** 适应进度版本号：任何层数变化都会 +1。 */
    public int adaptationVersion() {
        return adaptationVersion;
    }

    public String lastSyncedSignature() {
        return lastSyncedSignature;
    }

    public void setLastSyncedSignature(String signature) {
        this.lastSyncedSignature = signature == null ? "" : signature;
    }
}
