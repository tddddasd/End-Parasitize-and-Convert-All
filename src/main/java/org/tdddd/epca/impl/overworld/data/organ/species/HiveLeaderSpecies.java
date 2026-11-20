package org.tdddd.epca.impl.overworld.data.organ.species;

import javax.annotation.Nullable;

/**
 * 领巢者当前特化种阶。
 *
 * <p>持久化在玩家持久化数据（{@code HiveLeaderSpecies} 复合标签）里，
 * 由 {@code SyncHiveLeaderSpeciesPacket} 同步给客户端。</p>
 */
public enum HiveLeaderSpecies {

    /** 虫染种（默认）：攻击附加恐慌/虫咒缚身/流血，最小伤害 0.5。 */
    INFESTED("infested", "虫染种",
            0.5D, 0.0D, 0.0D, 0.0D),

    /** 初探种：器官 ≥18 初探种；最小伤害 0.1，接触施咒、鳍兽之鳍加成。 */
    ONESENT("onesent", "初探种",
            0.1D, 0.0D, 0.0D, 0.0D),

    /** 粗制种：器官 ≥18 粗制种且其它种阶 <5；+4 攻击、+20 生命、死亡爆体，无最小伤害。 */
    POVERTY("poverty", "粗制种",
            0.0D, 4.0D, 20.0D, 0.0D),

    /** 重塑种：进化而来；+5 攻击、+25 生命、+2 护甲，最小伤害 1。 */
    RESHAPE("reshape", "重塑种",
            1.0D, 5.0D, 25.0D, 2.0D);

    private final String id;
    private final String displayName;
    private final double minimumDamage;
    private final double attackDamageBonus;
    private final double maxHealthBonus;
    private final double armorBonus;

    HiveLeaderSpecies(String id, String displayName,
                      double minimumDamage, double attackDamageBonus,
                      double maxHealthBonus, double armorBonus) {
        this.id = id;
        this.displayName = displayName;
        this.minimumDamage = minimumDamage;
        this.attackDamageBonus = attackDamageBonus;
        this.maxHealthBonus = maxHealthBonus;
        this.armorBonus = armorBonus;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** 攻击最小伤害（0 = 该种阶没有最小伤害，例如粗制种）。 */
    public double minimumDamage() {
        return minimumDamage;
    }

    public double attackDamageBonus() {
        return attackDamageBonus;
    }

    public double maxHealthBonus() {
        return maxHealthBonus;
    }

    public double armorBonus() {
        return armorBonus;
    }

    public boolean hasMinimumDamage() {
        return minimumDamage > 0.0D;
    }

    @Nullable
    public static HiveLeaderSpecies byId(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (HiveLeaderSpecies species : values()) {
            if (species.id.equalsIgnoreCase(raw)) return species;
        }
        return null;
    }

    public static HiveLeaderSpecies fromOrganSpecies(OrganSpecies species) {
        if (species == null) return INFESTED;
        return switch (species) {
            case ONESENT -> ONESENT;
            case POVERTY -> POVERTY;
            case RESHAPE -> RESHAPE;
            default -> INFESTED;
        };
    }

    @Override
    public String toString() {
        return "HiveLeaderSpecies[" + id + "]";
    }
}
