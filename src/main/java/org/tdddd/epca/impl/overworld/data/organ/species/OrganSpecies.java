package org.tdddd.epca.impl.overworld.data.organ.species;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.tdddd.epca.impl.epca;

import javax.annotation.Nullable;

/**
 * 器官的「种阶」分类（注意：这是<b>器官物品</b>的分类，不是玩家当前特化）。
 *
 * <p>分类依据（优先级从高到低）：</p>
 * <ol>
 *   <li>物品标签 {@code epca:organ_species/<id>}（例如 {@code epca:organ_species/infested}）——
 *       数据包可以覆盖/扩展，不需要改代码；</li>
 *   <li>{@link OrganSpeciesClassifier} 里的内置映射表（按物品 id，依据掉落来源划分）；</li>
 *   <li>都没有命中 → {@link #OTHER}（未分类，只用于读数，不参与任何特化判定）。</li>
 * </ol>
 */
public enum OrganSpecies {

    /** 虫染种器官：虫染肉 / 虫染骨 / 患疾心脏 / 虫染粘液球 / 虫染末影珍珠 / 影刃残片。 */
    INFESTED("infested", "虫染"),

    /** 初探种器官：鳍兽的鳍（鳍兽掉落）、寄体内脏。 */
    ONESENT("onesent", "初探"),

    /** 粗制种器官：怪异肉沫（残缺体 / 活肉掉落）。 */
    POVERTY("poverty", "粗制"),

    /** 重塑种器官：重塑体肉排 / 重塑体甲壳 / 扭曲骨头 / 紧绷肌腱 / 气囊残片。 */
    RESHAPE("reshape", "重塑"),

    /** 连结种器官：召唤柱肉膜（召唤柱掉落），不参与四种特化判定。 */
    LINK("link", "连结"),

    /** 未分类器官（通用掉落物），不参与任何特化判定。 */
    OTHER("other", "未分类");

    private final String id;
    private final String displayName;
    private final TagKey<Item> itemTag;

    OrganSpecies(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        // 枚举构造期间不能引用静态常量 OTHER，改用 id 判断。
        this.itemTag = "other".equals(id)
                ? null
                : TagKey.create(Registries.ITEM,
                        Identifier.fromNamespaceAndPath(epca.MODID, "organ_species/" + id));
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** 可选的数据包标签 {@code epca:organ_species/<id>}；{@link #OTHER} 没有标签。 */
    @Nullable
    public TagKey<Item> itemTag() {
        return itemTag;
    }

    public boolean isSpecializationCandidate() {
        return this == INFESTED || this == ONESENT || this == POVERTY || this == RESHAPE;
    }

    @Nullable
    public static OrganSpecies byId(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (OrganSpecies species : values()) {
            if (species.id.equalsIgnoreCase(raw)) return species;
        }
        return null;
    }

    @Override
    public String toString() {
        return "OrganSpecies[" + id + "]";
    }
}
