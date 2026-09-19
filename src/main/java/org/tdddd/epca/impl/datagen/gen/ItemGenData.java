package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Block;
import org.tdddd.epca.impl.epca;

import java.util.Set;
import java.util.stream.Stream;

/**
 * 数据生成器：自动为模组中所有物品生成物品模型 + {@code assets/epca/items/*.json} 定义。
 *
 * <h2>26.1.2 重写说明</h2>
 * 1.20.1 用 Forge 的 {@code ItemModelProvider}（{@code basicItem/handheldItem/withExistingParent}
 * + {@code ExistingFileHelper}）。26.1.2 里该 Provider 已删除，改为原版 {@link ModelProvider}
 * 的 {@code ItemModelGenerators}，且 26.1.x 多了一层
 * <b>{@code assets/epca/items/<item>.json}</b>「物品模型定义」——它才是客户端真正读取的入口。
 *
 * <p>规则与 1.20.1 保持一致：
 * <ul>
 *   <li>工具类（{@code AxeItem/ShovelItem/HoeItem} 等）或名字带工具的 → {@code item/handheld}；
 *       26.1.2 已删除 {@code SwordItem}/{@code PickaxeItem}，因此这两个按名字后缀判定；</li>
 *   <li>普通物品 → {@code item/generated}；</li>
 *   <li>{@code BlockItem} 跳过（由 {@link BlockStateData} 处理）；</li>
 *   <li>特殊物品（复杂模型/overrides/独立渲染）→ {@link #MANUAL_WHITELIST} 跳过。</li>
 * </ul>
 */
public class ItemGenData extends ModelProvider {

    public ItemGenData(PackOutput output) {
        super(output, epca.MODID);
    }

    /**
     * 需要手动维护模型的物品（复杂模型、overrides、特殊 transform 等）。
     *
     * <p>不在本集合里的物品由 {@link #registerModels} 生成
     * {@code assets/epca/models/item/*.json} + {@code assets/epca/items/*.json}；
     * 列在这里的物品必须自带静态定义（{@code src/main/resources}），否则会没有模型。
     *
     * <p><b>26.1.2 复核</b>（每一条都对照过 {@code src/main/resources}）：
     * <ul>
     *   <li><b>矛</b>：静态定义用 {@code epca:item/<name>_item} 作为 GUI 图标纹理。
     *       26.1.2 的 {@code ItemModelGenerators#generateSpear} 用
     *       {@code TextureMapping.getItemTexture(item)}（即 {@code epca:item/<name>}，无后缀）
     *       作 GUI 图标，纹理文件不存在；而且它把 {@code gui/ground/fixed/on_shelf} 四个上下文
     *       都路由到平面图标，与手写定义（只覆盖 {@code gui/fixed}）不一致。
     *       因此矛继续由静态定义负责，不改为 datagen。</li>
     *   <li><b>erosion_clock</b>：{@code minecraft:range_dispatch} on {@code epca:evolution_stage}，
     *       需要按 {@code ClientSetup.EvolutionStageProperty} 的阈值逐条生成，静态定义更清晰。
     *       <b>阈值口径</b>：该 property 返回 {@code getStageForDimension(level) + 2}，而
     *       {@code EvolutionManager#getStage()} 的值域是 {@code [-2, 10]}，所以属性输出是
     *       {@code [0, 12]}；{@code RangeSelectItemModel} 取「不超过属性值的最大 threshold」，
     *       低于最小 threshold 时用 fallback。因此 phase-1..phase10 的 threshold 必须是
     *       {@code 1..12}，fallback 为 {@code phase_unknow}——这正好等价于 1.20.1 的
     *       {@code overrides}：{@code 0→phase_unknow, 1→phase-1, 2→phase0, …, 12→phase10}。</li>
     *   <li><b>swallow_cyst</b>：{@code minecraft:select} on {@code minecraft:component}，
     *       同样需要按数据组件的取值生成。</li>
     *   <li><b>feeding_module_i / flesh_armor_module_i / netherite_module_i / flight_module_i</b>：
     *       全项目<b>没有</b>任何模型或纹理（{@code textures/item/*module*.png} 与
     *       {@code models/item/*module*.json} 都不存在）。它们的静态定义指向
     *       {@code epca:item/null}，即不可生成的空图标；要正常显示必须先有美术资源。
     *       这里保留在 whitelist 中，是<b>有意为空</b>，不是遗漏。</li>
     * </ul>
     *
     * <p>已从这里移除的三项（证明它们在 datagen 覆盖范围内）：
     * {@code biomass_count_icon}、{@code epca_icon}（标准平面物品，纹理与模型都在）、
     * {@code infested_carved_pumpkin}（它是 {@code BlockItem}，本来就被
     * {@link #getKnownItems()} 的 {@code BlockItem} 过滤排除，而 {@code BlockStateData}
     * 把它列在 {@code MANUAL_BLOCKS} 里，所以它的静态定义仍必须保留）。
     */
    private static final Set<String> MANUAL_WHITELIST = Set.of(
            // --- 矛（GUI 用 *_item 纹理 + 只覆盖 gui/fixed 的 display_context 分发） ---
            "wooden_spear", "stone_spear", "flint_spear", "copper_spear",
            "iron_spear", "golden_spear", "diamond_spear", "netherite_spear",

            // --- 特殊物品（数据驱动的模型分发） ---
            "erosion_clock",
            "swallow_cyst",

            // --- 模块物品（没有任何纹理/模型，静态定义指向 epca:item/null） ---
            "feeding_module_i", "flesh_armor_module_i",
            "netherite_module_i", "flight_module_i"
    );

    /**
     * 额外应使用 handheld 风格的物品（不继承原版工具类）。
     */
    private static final Set<String> EXTRA_HANDHELD = Set.of(
            "endless_wand"
    );

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        // 方块状态与方块物品模型由 BlockStateData 负责
        return Stream.empty();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        // ModelProvider 只校验“声明过的条目必须有定义”，因此这里只列出本生成器负责的物品
        return BuiltInRegistries.ITEM.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> !(holder.value() instanceof BlockItem))
                .filter(holder -> !MANUAL_WHITELIST.contains(holder.getKey().identifier().getPath()));
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        BuiltInRegistries.ITEM.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> !(holder.value() instanceof BlockItem))
                .filter(holder -> !MANUAL_WHITELIST.contains(holder.getKey().identifier().getPath()))
                .forEach(holder -> {
                    Item item = holder.value();
                    String path = holder.getKey().identifier().getPath();
                    try {
                        if (isHandheld(item) || EXTRA_HANDHELD.contains(path)) {
                            itemModels.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
                        } else {
                            itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
                        }
                    } catch (Exception e) {
                        epca.LOGGER.warn("Skipping item model for {} (no texture): {}", path, e.getMessage());
                    }
                });
    }

    private boolean isHandheld(Item item) {
        if (item instanceof AxeItem || item instanceof ShovelItem || item instanceof HoeItem) {
            return true;
        }
        // 26.1.2 已删除 SwordItem / PickaxeItem（工具改为数据驱动），按注册名后缀兜底判定
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return false;
        String path = key.getPath();
        return path.endsWith("_sword") || path.endsWith("_pickaxe");
    }

    @Override
    public String getName() {
        return "EPCA Item Models";
    }
}
