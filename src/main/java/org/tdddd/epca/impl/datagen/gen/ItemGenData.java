package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;

import java.util.Set;

/**
 * 数据生成器：自动为模组中所有物品生成 models/item/*.json。
 * - 手持工具类（Sword/Pickaxe/Axe/Shovel/Hoe）→ item/handheld
 * - 普通物品 → item/generated
 * - BlockItem → 跳过（由 BlockStateData 处理）
 * - 特殊物品（模型复杂，无法自动生成）→ MANUAL_WHITELIST 跳过
 */
public class ItemGenData extends ItemModelProvider {

    public ItemGenData(PackOutput output, ExistingFileHelper efh) {
        super(output, epca.MODID, efh);
    }

    /**
     * 需要手动维护模型的物品（复杂模型、overrides、特殊 transform 等）。
     */
    private static final Set<String> MANUAL_WHITELIST = Set.of(
            // --- 矛（使用 forge:separate_transforms） ---
            "wooden_spear", "stone_spear", "flint_spear", "copper_spear",
            "iron_spear", "golden_spear", "diamond_spear", "netherite_spear",

            // --- 特殊物品（有 overrides / 自定义模型结构） ---
            "erosion_clock",
            "biomass_count_icon",
            "epca_icon",
            "swallow_cyst",

            // --- 模块物品（无独立纹理，使用特殊渲染） ---
            "feeding_module_i", "instinct_module_i", "flesh_armor_module_i",
            "netherite_module_i", "flight_module_i",

            "infested_carved_pumpkin"
    );

    /**
     * 额外应使用 handheld 风格的物品（不继承原版工具类）。
     */
    private static final Set<String> EXTRA_HANDHELD = Set.of(
            "endless_wand"
    );

    @Override
    protected void registerModels() {
        ForgeRegistries.ITEMS.getValues().forEach(item -> {
            ResourceLocation loc = ForgeRegistries.ITEMS.getKey(item);
            if (loc != null && loc.getNamespace().equals(epca.MODID)) {
                String path = loc.getPath();
                // 跳过方块物品（由 BlockStateData 处理）和手动维护物品
                if (item instanceof BlockItem || MANUAL_WHITELIST.contains(path)) return;
                try {
                    // 武器工具类或额外手持风格 → handheld
                    if (isHandheld(item) || EXTRA_HANDHELD.contains(path)) {
                        handheldItem(path);
                    } else {
                        this.basicItem(item);
                    }
                } catch (Exception e) {
                    MANUAL_WHITELIST_DYNAMIC.add(path);
                    epca.LOGGER.warn("Skipping item model for {} (no texture): {}", path, e.getMessage());
                }
            }
        });
    }

    /** 运行时发现的缺失纹理物品，防止重复日志 */
    private static final Set<String> MANUAL_WHITELIST_DYNAMIC = new java.util.HashSet<>();

    private boolean isHandheld(Item item) {
        return item instanceof SwordItem || item instanceof PickaxeItem
                || item instanceof AxeItem || item instanceof ShovelItem || item instanceof HoeItem;
    }

    protected void handheldItem(String path) {
        withExistingParent(path, mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/" + path));
    }
}
