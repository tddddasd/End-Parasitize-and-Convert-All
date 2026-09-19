package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import org.tdddd.epca.impl.epca;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 数据生成器：自动为模组中所有方块生成 blockstates / 模型 / <b>物品模型定义</b>。
 *
 * <h2>26.1.2 重写说明</h2>
 * 1.20.1 用的是 Forge 的 {@code BlockStateProvider}（{@code slabBlock/stairsBlock/simpleBlockItem/…}
 * + {@code ExistingFileHelper}）。26.1.2 里 Forge 的整套 model generator 被删除，
 * 改为**原版**的 {@link ModelProvider}：它一次生成三类文件
 * <ol>
 *   <li>{@code assets/epca/blockstates/<block>.json}</li>
 *   <li>{@code assets/epca/models/block/<block>.json}（+ 派生模型，如 {@code <block>_top}）</li>
 *   <li><b>{@code assets/epca/items/<item>.json}</b> —— 26.1.x 新增的“物品模型定义”间接层。
 *       1.20.1 只要有 {@code models/item/*.json} 就够了；26.1.2 缺了这一层物品会没有模型。
 *       因此移植后必须跑一次 {@code runData}（或手工补 {@code assets/epca/items/**}）。</li>
 * </ol>
 *
 * <p>2. {@link ModelProvider} 默认会对<b>所有</b>属于本 mod 命名空间的方块/物品做
 * “必须有定义”的校验。本模组大量方块（多模型变体、雪层、藤蔓、滴水石锥、BBmodel 等）的
 * blockstate/模型是手工维护的，不应该被数据生成器重写，因此覆写
 * {@link #getKnownBlocks()}/{@link #getKnownItems()}，只声明本生成器真正处理的条目。
 *
 * <p>3. 纹理推断与 1.20.1 一致：方块自身 {@code epca:block/<name>}；
 * slab/stairs/wall/fence/fence_gate/door/trapdoor/button/pressure_plate/pane 去掉后缀后
 * 找“父方块”（例如 {@code infested_cobblestone_slab} → {@code infested_cobblestone}），
 * 找不到就退回自身纹理。
 *
 * <p>4. 木类保持 1.20.1 的 handheld 判定：26.1.2 已删除 {@code SwordItem}/{@code PickaxeItem}，
 * 工具类只剩 {@code AxeItem/ShovelItem/HoeItem}，因此额外按名字后缀
 * （{@code _sword}/{@code _pickaxe}/{@code _axe}/{@code _shovel}/{@code _hoe}）判定手持风格。
 */
public class BlockStateData extends ModelProvider {

    /**
     * 需手动维护模型/blockstate 的方块：
     * - 多模型变体（dirt 随机纹理）
     * - 雪层（高度属性）
     * - 藤蔓（多方块面）
     * - 滴水石锥
     * - 方块实体渲染器（swallow_cyst）
     * - 自定义 BBmodel 元素模型
     */
    private static final Set<String> MANUAL_BLOCKS = Set.of(
            "infested_dirt", "infested_log", "infested_stone", "infested_heavy_stone","infested_wood", "infested_stripped_wood",
            "infested_snow", "infested_spider_web", "infested_spider_web_blood", "infested_cave_spider_web",
            "infested_vine", "infested_sweet_berry_bush", "infested_cactus", "infested_sugar_cane",
            "infested_pointed_dripstone", "infested_mangrove_roots", "infested_muddy_mangrove_roots",
            "swallow_cyst",
            "infested_lily_pad", "infested_carved_pumpkin", "infested_pumpkin",
            "infested_remains_small", "infested_remains_medium", "infested_remains_large",
            "infested_residue",
            "infested_nethersea_brand_grown",
            "infested_sandstone", "infested_sandstone_slab", "infested_sandstone_stairs", "infested_chiseled_red_sandstone", "infested_chiseled_sandstone", "infested_cut_sandstone", "infested_cut_sandstone_slab",
            "infested_tall_grass", "infested_tall_fern",
            "acid_solution"
    );

    public BlockStateData(PackOutput output) {
        super(output, epca.MODID);
    }

    // ═══════════════ 需要生成的条目（ModelProvider 的校验集合） ═══════════════

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> isGenerated(holder.value()));
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        // 只声明“被本生成器生成 blockstate 的方块”对应的 BlockItem；
        // 纯物品（非 BlockItem）由 ItemGenData 负责。
        return BuiltInRegistries.ITEM.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> holder.value() instanceof BlockItem blockItem && isGenerated(blockItem.getBlock()));
    }

    private static boolean isGenerated(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null || !key.getNamespace().equals(epca.MODID)) return false;
        if (MANUAL_BLOCKS.contains(key.getPath())) return false;
        return block instanceof SlabBlock
                || block instanceof StairBlock
                || block instanceof WallBlock
                || block instanceof RotatedPillarBlock
                || block instanceof LeavesBlock
                || block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof ButtonBlock
                || block instanceof PressurePlateBlock
                || block instanceof IronBarsBlock
                || block instanceof BushBlock
                || true; // 其余按“普通完整方块”
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        BuiltInRegistries.BLOCK.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> isGenerated(holder.value()))
                .map(Holder::value)
                .forEach(block -> {
                    try {
                        generateBlock(blockModels, block);
                    } catch (Exception e) {
                        epca.LOGGER.warn("Skipping blockstate/model for {}: {}", name(block), e.getMessage());
                    }
                });
    }

    /**
     * 根据方块类型分发到对应的生成方法（最具体的类型优先匹配）。
     */
    private void generateBlock(BlockModelGenerators g, Block block) {
        if (block instanceof SlabBlock slab) {
            slabBlockWithItem(g, slab);
        } else if (block instanceof StairBlock stair) {
            stairsBlockWithItem(g, stair);
        } else if (block instanceof WallBlock wall) {
            wallBlockWithItem(g, wall);
        } else if (block instanceof RotatedPillarBlock) {
            logBlockWithItem(g, block);
        } else if (block instanceof LeavesBlock) {
            leavesBlockWithItem(g, block);
        } else if (block instanceof FenceBlock fence) {
            fenceBlockWithItem(g, fence);
        } else if (block instanceof FenceGateBlock gate) {
            fenceGateBlockWithItem(g, gate);
        } else if (block instanceof DoorBlock door) {
            doorBlockWithItem(g, door);
        } else if (block instanceof TrapDoorBlock trapdoor) {
            trapdoorBlockWithItem(g, trapdoor);
        } else if (block instanceof ButtonBlock button) {
            buttonBlockWithItem(g, button);
        } else if (block instanceof PressurePlateBlock plate) {
            pressurePlateBlockWithItem(g, plate);
        } else if (block instanceof IronBarsBlock pane) {
            paneBlockWithItem(g, pane);
        } else if (block instanceof BushBlock) {
            crossBlockWithItem(g, block);
        } else {
            // 默认：普通完整方块
            simpleBlockWithItem(g, block);
        }
    }

    // ======================== 带默认纹理推断的辅助方法 ========================

    /**
     * 尝试从方块名推断配套的“完整方块”，用于 slab/stairs/wall 等派生方块的纹理。
     * 例如 infested_cobblestone_slab -> infested_cobblestone
     */
    private Block findParentBlock(Block child, String... suffixes) {
        String childName = name(child);
        for (String suffix : suffixes) {
            if (childName.endsWith(suffix)) {
                String parentName = childName.substring(0, childName.length() - suffix.length());
                Identifier rl = Identifier.fromNamespaceAndPath(epca.MODID, parentName);
                if (BuiltInRegistries.BLOCK.containsKey(rl)) {
                    return BuiltInRegistries.BLOCK.getValue(rl);
                }
            }
        }
        // fallback: use the child's own texture
        return child;
    }

    // ======================== 具体类型的生成方法 ========================

    /** 普通完整方块（六面纹理相同）。 */
    protected void simpleBlockWithItem(BlockModelGenerators g, Block block) {
        blockTextureModel(g, block, ModelTemplates.CUBE_ALL);
        g.registerSimpleItemModel(block, modelId(block));
    }

    /** Slab：自动查找配套 fullBlock 获取纹理。 */
    private void slabBlockWithItem(BlockModelGenerators g, SlabBlock slab) {
        Block fullBlock = findParentBlock(slab, "_slab");
        TextureMapping tex = TextureMapping.cube(blockTexture(fullBlock));
        Identifier bottom = ModelTemplates.SLAB_BOTTOM.create(slab, tex, g.modelOutput);
        Identifier top = ModelTemplates.SLAB_TOP.create(slab, tex, g.modelOutput);
        Identifier full = modelId(fullBlock);
        g.blockStateOutput.accept(BlockModelGenerators.createSlab(slab,
                BlockModelGenerators.plainVariant(bottom),
                BlockModelGenerators.plainVariant(top),
                BlockModelGenerators.plainVariant(full)));
        g.registerSimpleItemModel(slab, bottom);
    }

    /** Stairs：自动查找配套 fullBlock 获取纹理。 */
    private void stairsBlockWithItem(BlockModelGenerators g, StairBlock stair) {
        Block fullBlock = findParentBlock(stair, "_stairs");
        TextureMapping tex = TextureMapping.cube(blockTexture(fullBlock));
        Identifier inner = ModelTemplates.STAIRS_INNER.create(stair, tex, g.modelOutput);
        Identifier straight = ModelTemplates.STAIRS_STRAIGHT.create(stair, tex, g.modelOutput);
        Identifier outer = ModelTemplates.STAIRS_OUTER.create(stair, tex, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createStairs(stair,
                BlockModelGenerators.plainVariant(inner),
                BlockModelGenerators.plainVariant(straight),
                BlockModelGenerators.plainVariant(outer)));
        g.registerSimpleItemModel(stair, straight);
    }

    /** Wall：自动查找配套 fullBlock 获取纹理。 */
    private void wallBlockWithItem(BlockModelGenerators g, WallBlock wall) {
        Block fullBlock = findParentBlock(wall, "_wall");
        Material tex = blockTexture(fullBlock);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.WALL, tex);
        Identifier post = ModelTemplates.WALL_POST.create(wall, mapping, g.modelOutput);
        Identifier lowSide = ModelTemplates.WALL_LOW_SIDE.create(wall, mapping, g.modelOutput);
        Identifier tallSide = ModelTemplates.WALL_TALL_SIDE.create(wall, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createWall(wall,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(lowSide),
                BlockModelGenerators.plainVariant(tallSide)));
        Identifier inventory = ModelTemplates.WALL_INVENTORY.create(wall, mapping, g.modelOutput);
        g.registerSimpleItemModel(wall, inventory);
    }

    /** Fence：自动查找配套 plank 获取纹理。 */
    private void fenceBlockWithItem(BlockModelGenerators g, FenceBlock fence) {
        Block plank = findParentBlock(fence, "_fence");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier post = ModelTemplates.FENCE_POST.create(fence, mapping, g.modelOutput);
        Identifier side = ModelTemplates.FENCE_SIDE.create(fence, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createFence(fence,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(side)));
        Identifier inventory = ModelTemplates.FENCE_INVENTORY.create(fence, mapping, g.modelOutput);
        g.registerSimpleItemModel(fence, inventory);
    }

    /** RotatedPillarBlock 原木/柱子类。 */
    protected void logBlockWithItem(BlockModelGenerators g, Block block) {
        Material side = blockTexture(block);
        Material end = blockTexture(block, "_top");
        TextureMapping mapping = TextureMapping.column(side, end);
        Identifier model = ModelTemplates.CUBE_COLUMN.create(block, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(block,
                BlockModelGenerators.plainVariant(model)));
        g.registerSimpleItemModel(block, model);
    }

    /** 树叶方块。 */
    private void leavesBlockWithItem(BlockModelGenerators g, Block block) {
        blockTextureModel(g, block, ModelTemplates.LEAVES);
        g.registerSimpleItemModel(block, modelId(block));
    }

    /**
     * 交叉植物（花草）。
     *
     * <p><b>物品定义必须指向扁平物品模型</b>：1.20.1 的 GUI 图标走
     * {@code models/item/<id>.json}（{@code item/generated} + 方块纹理），26.1.2 原版对
     * {@code short_grass}/{@code fern}/{@code tall_grass}/{@code dead_bush} 也是同一做法
     * （{@code items/<id>.json} → {@code minecraft:item/<id>}，见
     * {@code BlockModelGenerators#createCrossBlockWithDefaultItem}）。若像以前那样把
     * {@code items/<id>.json} 指向 {@code epca:block/<id>}（{@code block/cross}），物品栏里会
     * 显示交叉的立体面片而不是原版那张平面贴图；{@code infested_sugar_cane} 更会显示
     * {@code block/infested_sugar_cane}（整根甘蔗）而不是 1.20.1 的
     * {@code item/infested_sugar_cane_top}。
     */
    protected void crossBlockWithItem(BlockModelGenerators g, Block block) {
        TextureMapping mapping = TextureMapping.cross(blockTexture(block));
        Identifier model = ModelTemplates.CROSS.create(block, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block,
                BlockModelGenerators.plainVariant(model)));
        // 26.1.2: 生成 models/item/<id>.json（item/generated，layer0 = epca:block/<id>）
        // 并把 items/<id>.json 指向 epca:item/<id>，与 1.20.1 的 GUI 外观一致。
        g.registerSimpleFlatItemModel(block);
    }

    /** 栅栏门。 */
    private void fenceGateBlockWithItem(BlockModelGenerators g, FenceGateBlock gate) {
        Block plank = findParentBlock(gate, "_fence_gate");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier closed = ModelTemplates.FENCE_GATE_CLOSED.create(gate, mapping, g.modelOutput);
        Identifier open = ModelTemplates.FENCE_GATE_OPEN.create(gate, mapping, g.modelOutput);
        Identifier wallClosed = ModelTemplates.FENCE_GATE_WALL_CLOSED.create(gate, mapping, g.modelOutput);
        Identifier wallOpen = ModelTemplates.FENCE_GATE_WALL_OPEN.create(gate, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createFenceGate(gate,
                BlockModelGenerators.plainVariant(open),
                BlockModelGenerators.plainVariant(closed),
                BlockModelGenerators.plainVariant(wallOpen),
                BlockModelGenerators.plainVariant(wallClosed),
                true));
        g.registerSimpleItemModel(gate, closed);
    }

    /** 门。26.1.2 的 {@code createDoor(Block)} 会一次性建好 8 个模型、blockstate 与物品定义。 */
    private void doorBlockWithItem(BlockModelGenerators g, DoorBlock door) {
        g.createDoor(door);
    }

    /** 活板门。 */
    private void trapdoorBlockWithItem(BlockModelGenerators g, TrapDoorBlock trapdoor) {
        Block plank = findParentBlock(trapdoor, "_trapdoor");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier top = ModelTemplates.TRAPDOOR_TOP.create(trapdoor, mapping, g.modelOutput);
        Identifier bottom = ModelTemplates.TRAPDOOR_BOTTOM.create(trapdoor, mapping, g.modelOutput);
        Identifier open = ModelTemplates.TRAPDOOR_OPEN.create(trapdoor, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createTrapdoor(trapdoor,
                BlockModelGenerators.plainVariant(top),
                BlockModelGenerators.plainVariant(bottom),
                BlockModelGenerators.plainVariant(open)));
        g.registerSimpleItemModel(trapdoor, bottom);
    }

    /** 按钮。 */
    private void buttonBlockWithItem(BlockModelGenerators g, ButtonBlock button) {
        Block plank = findParentBlock(button, "_button");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier normal = ModelTemplates.BUTTON.create(button, mapping, g.modelOutput);
        Identifier pressed = ModelTemplates.BUTTON_PRESSED.create(button, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createButton(button,
                BlockModelGenerators.plainVariant(normal),
                BlockModelGenerators.plainVariant(pressed)));
        Identifier inventory = ModelTemplates.BUTTON_INVENTORY.create(button, mapping, g.modelOutput);
        g.registerSimpleItemModel(button, inventory);
    }

    /** 压力板。 */
    private void pressurePlateBlockWithItem(BlockModelGenerators g, PressurePlateBlock plate) {
        Block plank = findParentBlock(plate, "_pressure_plate");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier off = ModelTemplates.PRESSURE_PLATE_UP.create(plate, mapping, g.modelOutput);
        Identifier on = ModelTemplates.PRESSURE_PLATE_DOWN.create(plate, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createPressurePlate(plate,
                BlockModelGenerators.plainVariant(off),
                BlockModelGenerators.plainVariant(on)));
        g.registerSimpleItemModel(plate, off);
    }

    /** 玻璃板/铁栏杆。26.1.2 的 {@code createBarsAndItem(Block)} 一次建好全部模型+blockstate+物品定义。 */
    private void paneBlockWithItem(BlockModelGenerators g, IronBarsBlock pane) {
        g.createBarsAndItem(pane);
    }

    // ======================== 工具方法 ========================

    /** 六面同纹理的方块模型。 */
    private void blockTextureModel(BlockModelGenerators g, Block block, ModelTemplate template) {
        TextureMapping mapping = TextureMapping.cube(blockTexture(block));
        template.create(block, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block,
                BlockModelGenerators.plainVariant(modelId(block))));
    }

    protected String name(Block block) {
        return Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block)).getPath();
    }

    /** 等价于 1.20.1 的 {@code blockTexture(Block)}：{@code epca:block/<name>}。 */
    protected Material blockTexture(Block block) {
        return new Material(Identifier.fromNamespaceAndPath(epca.MODID, "block/" + name(block)));
    }

    /** 等价于 1.20.1 的 {@code extend(blockTexture(block), suffix)}。 */
    protected Material blockTexture(Block block, String suffix) {
        return new Material(Identifier.fromNamespaceAndPath(epca.MODID, "block/" + name(block) + suffix));
    }

    protected Identifier modelId(Block block) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "block/" + name(block));
    }

    /** 供 ItemGenData 复用的已生成方块集合（不含手动方块）。 */
    public static List<Block> generatedBlocks() {
        List<Block> result = new ArrayList<>();
        BuiltInRegistries.BLOCK.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> isGenerated(holder.value()))
                .forEach(holder -> result.add(holder.value()));
        return result;
    }
}
