package org.tdddd.epca.impl.overworld.data.organ.species;

import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;

import java.util.Map;

/**
 * 按种阶统计玩家当前身上的器官数量。
 *
 * <p>数据来源是 {@link OrganStatSummary#slotItemCounts()}：它已经按现有规则统计了
 * 「内部槽全部参与 + 外层紫槽必须已解锁」的所有非空槽位，所以这里不需要再遍历槽位，
 * 只把物品 id 映射到种阶后累加即可（与器官属性读数使用同一份统计口径）。</p>
 */
public record OrganSpeciesCounts(int infested, int onesent, int poverty, int reshape, int link, int other) {

    /** 判定特化所需的最少同种阶器官数量。 */
    public static final int REQUIRED_ORGAN_COUNT = 18;

    /** 粗制种判定时，「其它种阶」允许的最大器官数量（< 5）。 */
    public static final int POVERTY_MAX_OTHER_ORGANS = 5;

    public static final OrganSpeciesCounts EMPTY = new OrganSpeciesCounts(0, 0, 0, 0, 0, 0);

    public static OrganSpeciesCounts of(OrganStatSummary summary) {
        if (summary == null) return EMPTY;
        int infested = 0;
        int onesent = 0;
        int poverty = 0;
        int reshape = 0;
        int link = 0;
        int other = 0;
        for (Map.Entry<net.minecraft.resources.ResourceLocation, Integer> entry
                : summary.slotItemCounts().entrySet()) {
            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (count <= 0) continue;
            switch (OrganSpeciesClassifier.classify(entry.getKey())) {
                case INFESTED -> infested += count;
                case ONESENT -> onesent += count;
                case POVERTY -> poverty += count;
                case RESHAPE -> reshape += count;
                case LINK -> link += count;
                default -> other += count;
            }
        }
        return new OrganSpeciesCounts(infested, onesent, poverty, reshape, link, other);
    }

    public int count(OrganSpecies species) {
        if (species == null) return 0;
        return switch (species) {
            case INFESTED -> infested;
            case ONESENT -> onesent;
            case POVERTY -> poverty;
            case RESHAPE -> reshape;
            case LINK -> link;
            case OTHER -> other;
        };
    }

    public int total() {
        return infested + onesent + poverty + reshape + link + other;
    }

    /** 「其它种阶」数量：四种特化种阶 + 连结种，未分类不计。 */
    public int otherTierCount() {
        return infested + onesent + reshape + link;
    }

    public int povertyOrReshape() {
        return poverty + reshape;
    }

    public boolean infestedCondition() {
        return infested >= REQUIRED_ORGAN_COUNT;
    }

    public boolean onesentCondition() {
        return onesent >= REQUIRED_ORGAN_COUNT;
    }

    public boolean povertyCondition() {
        return poverty >= REQUIRED_ORGAN_COUNT && otherTierCount() < POVERTY_MAX_OTHER_ORGANS;
    }

    /**
     * 自动判定（不含「重塑种」，重塑种只能靠 25 击杀 + 50 生物质进化）。
     *
     * <p>优先级：粗制种 → 初探种 → 虫染种 → 默认虫染种。</p>
     * <p>由于单槽位最多 35 个、同种阶需要 ≥18，虫染/初探不可能同时成立；
     * 粗制种自带「其它种阶 &lt; 5」的限制，因此三者实际上互斥。</p>
     */
    public HiveLeaderSpecies autoSpecies() {
        if (povertyCondition()) return HiveLeaderSpecies.POVERTY;
        if (onesentCondition()) return HiveLeaderSpecies.ONESENT;
        if (infestedCondition()) return HiveLeaderSpecies.INFESTED;
        // 未满足任何条件：回退到默认虫染种（已持有的特化由调用方决定是否保留）。
        return HiveLeaderSpecies.INFESTED;
    }

    public String describe() {
        return "虫染=" + infested + " 初探=" + onesent + " 粗制=" + poverty
                + " 重塑=" + reshape + " 连结=" + link + " 未分类=" + other;
    }
}
