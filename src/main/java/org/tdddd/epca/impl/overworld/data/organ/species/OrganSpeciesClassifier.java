package org.tdddd.epca.impl.overworld.data.organ.species;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 器官物品 → 种阶 的分类器。
 *
 * <p>判定顺序：物品标签（{@link OrganSpecies#itemTag()}，数据包可用）→ 内置映射表 → {@link OrganSpecies#OTHER}。</p>
 *
 * <p><b>内置映射表的依据</b>：{@code data/epca/loot_tables/entities/*.json} 里各器官的实际掉落来源。
 * 例如 {@code infested_flesh/diseased_heart} 来自虫染动物、{@code infested_bone} 来自虫染骷髅、
 * {@code infested_slime_ball} 来自虫染史莱姆、{@code infested_ender_pearl/ender_blade_scrap} 来自虫染末影人，
 * 因此都算虫染种；{@code reshape_*} / {@code twisted_bone} / {@code tight_tendons} / {@code gasbag_debris}
 * 只由重塑体掉落，算重塑种；{@code fins_fin} 只由鳍兽掉落，算初探种；{@code weird_minced_flesh}
 * 只由残缺体 / 活肉掉落，算粗制种；{@code beckon_membrane} 只由召唤柱掉落，算连结种；
 * {@code parasite_viscera} 是多种寄生体的通用掉落，故意留作未分类，避免污染特化判定。</p>
 */
public final class OrganSpeciesClassifier {

    private static final Map<String, OrganSpecies> BUILTIN = buildBuiltin();

    private OrganSpeciesClassifier() {
    }

    private static Map<String, OrganSpecies> buildBuiltin() {
        Map<String, OrganSpecies> map = new LinkedHashMap<>();

        put(map, OrganSpecies.INFESTED,
                "infested_flesh",
                "infested_bone",
                "diseased_heart",
                "infested_slime_ball",
                "infested_ender_pearl",
                "ender_blade_scrap");

        put(map, OrganSpecies.ONESENT,
                "fins_fin",
                "parasite_viscera");

        put(map, OrganSpecies.POVERTY,
                "weird_minced_flesh");

        put(map, OrganSpecies.RESHAPE,
                "reshape_flesh",
                "reshape_shell",
                "twisted_bone",
                "tight_tendons",
                "gasbag_debris");

        put(map, OrganSpecies.LINK,
                "beckon_membrane");

        return Map.copyOf(map);
    }

    private static void put(Map<String, OrganSpecies> map, OrganSpecies species, String... itemPaths) {
        for (String path : itemPaths) {
            map.put("epca:" + path, species);
        }
    }

    public static OrganSpecies classify(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return OrganSpecies.OTHER;

        for (OrganSpecies species : OrganSpecies.values()) {
            if (!species.isSpecializationCandidate() && species != OrganSpecies.LINK) continue;
            if (species.itemTag() != null && stack.is(species.itemTag())) {
                return species;
            }
        }

        Item item = stack.getItem();
        if (item == null) return OrganSpecies.OTHER;
        return classify(ForgeRegistries.ITEMS.getKey(item));
    }

    public static OrganSpecies classify(@Nullable ResourceLocation itemId) {
        if (itemId == null) return OrganSpecies.OTHER;
        OrganSpecies species = BUILTIN.get(itemId.toString());
        return species == null ? OrganSpecies.OTHER : species;
    }

    /** 供调试/汇报使用：内置映射表内容。 */
    public static Map<String, OrganSpecies> builtinMapping() {
        return BUILTIN;
    }
}
