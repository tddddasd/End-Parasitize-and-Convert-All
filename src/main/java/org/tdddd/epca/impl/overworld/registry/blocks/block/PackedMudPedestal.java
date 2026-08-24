package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.PackedMudPedestalBlockEntity;
import org.tdddd.epca.impl.overworld.data.AltarPointManager;
import org.tdddd.epca.impl.overworld.registry.items.item.SmallItemFrame;

import java.util.*;
import java.util.stream.Collectors;

public class PackedMudPedestal extends BaseEntityBlock {
    protected static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final int MAX_PEDESTAL_TOTAL = 21;
    private static final int GRID_SIZE = 3;

    public static final TagKey<Block> PEDESTAL_TAG = TagKey.create(Registries.BLOCK,
            new ResourceLocation("epca", "pedestals"));
    public static final TagKey<Block> ALTAR_STONE_TAG = TagKey.create(Registries.BLOCK,
            new ResourceLocation("epca", "altar_stones"));

    public PackedMudPedestal() {
        super(Properties.of()
                .noOcclusion()
                .strength(1.0f, 3.0f)
                .sound(SoundType.PACKED_MUD)
                .isViewBlocking((state, world, pos) -> false)
                .isSuffocating((state, world, pos) -> false)
                .pushReaction(PushReaction.DESTROY)
                .requiresCorrectToolForDrops()
                .mapColor(DyeColor.ORANGE)
                .randomTicks()
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);

        // 处理 SmallItemFrame 右键
        if (heldItem.getItem() instanceof SmallItemFrame) {
            if (level.getBlockEntity(pos) instanceof PackedMudPedestalBlockEntity pedestal) {
                String filterData = "";
                if (heldItem.hasTag() && heldItem.getTag().contains("item_data")) {
                    filterData = heldItem.getTag().getString("item_data");
                }
                pedestal.setFilterData(SmallItemFrame.getItemIds(heldItem));
                if (filterData.isEmpty()) {
                    clearMainPedestalInStructure(level, pos); // 空过滤器 → 清除主祭台标记
                }
                level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // 潜行 + 空手右键 → 取出当前祭台的物品
        if (heldItem.isEmpty() && player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof PackedMudPedestalBlockEntity blockEntity) {
                if (blockEntity.hasItem()) {
                    ItemStack storedItem = blockEntity.getItem();
                    if (player.getInventory().add(storedItem)) {
                        blockEntity.clearItem();
                        player.inventoryMenu.sendAllDataToRemote();
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    } else {
                        ItemEntity itemEntity = new ItemEntity(level,
                                player.getX(), player.getY() + 0.5, player.getZ(),
                                storedItem);
                        itemEntity.setPickUpDelay(0);
                        level.addFreshEntity(itemEntity);
                        blockEntity.clearItem();
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }

        // 烈焰棒查询祭坛信息（此处使用 Kill Stick，可按需改回 BLAZE_ROD）
        if (heldItem.is(ModItems.KILL_STICK.get())) {
            AltarStructureData data = findAltarStructure(level, pos);
            if (data == null || data.allPositions.isEmpty()) {
                player.displayClientMessage(Component.literal("当前方块不属于任何有效祭坛结构"), false);
                return InteractionResult.SUCCESS;
            }
            String status = data.isValid ? "有效" : "无效";
            player.displayClientMessage(Component.literal("总祭台数: " + data.pedestalCount), false);
            player.displayClientMessage(Component.literal("有物品的祭台数: " + data.pedestalsWithItem), false);
            player.displayClientMessage(Component.literal("总点数: " + data.totalPoints), false);
            player.displayClientMessage(Component.literal("状态: " + status), false);
            if (!data.isValid && data.invalidReason != null) {
                player.displayClientMessage(Component.literal("原因: " + data.invalidReason), false);
            }
            return InteractionResult.SUCCESS;
        }

        // 空手右键 → 触发合成（仅当右键的是主祭台且结构有效）
        if (heldItem.isEmpty()) {
            AltarStructureData data = findAltarStructure(level, pos);
            if (data != null && data.isValidForCrafting && data.mainPedestal != null && data.mainPedestal.equals(pos)) {
                // 将该祭台设为主祭台，并清除其他祭台的主祭台标记
                setMainPedestalInStructure(level, pos);

                ItemStack targetStack = getFrameTarget(level, pos);
                if (performCrafting(level, pos, data, targetStack)) {
                    level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        }

        // 有手持物品时的交互（放置物品）
        if (isLocked(level, pos)) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos) instanceof PackedMudPedestalBlockEntity blockEntity) {
            if (blockEntity.hasItem()) {
                return InteractionResult.PASS;
            } else if (!heldItem.isEmpty()) {
                ItemStack copy = heldItem.copy();
                copy.setCount(1);
                blockEntity.setItem(copy);
                heldItem.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    // -------- 合成相关 --------

    @Nullable
    private ItemStack getFrameTarget(Level level, BlockPos corePos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                mutable.set(corePos.getX() + dx, corePos.getY(), corePos.getZ() + dz);
                List<ItemFrame> frames = level.getEntitiesOfClass(
                        ItemFrame.class,
                        new AABB(mutable).inflate(0.1)
                );
                for (ItemFrame frame : frames) {
                    if (frame.blockPosition().equals(mutable)) {
                        ItemStack frameItem = frame.getItem();
                        if (!frameItem.isEmpty()) {
                            return frameItem.copy();
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean performCrafting(Level level, BlockPos corePos, AltarStructureData data,
                                    @Nullable ItemStack targetStack) {
        List<BlockPos> pedestalPositions = data.pedestalPositions;
        boolean isSingle = pedestalPositions.size() == 1;

        // ----- 收集可用物品，并记录对应的基座位置（用于返还） -----
        List<ItemStack> availableItems = new ArrayList<>();
        List<BlockPos> sourcePositions = new ArrayList<>();
        if (isSingle) {
            BlockEntity be = level.getBlockEntity(corePos);
            if (be instanceof PackedMudPedestalBlockEntity pedestal && pedestal.hasItem()) {
                availableItems.add(pedestal.getItem().copy());
                sourcePositions.add(corePos);
            } else {
                return false;
            }
        } else {
            for (BlockPos p : pedestalPositions) {
                if (p.equals(corePos)) continue;
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof PackedMudPedestalBlockEntity pedestal && pedestal.hasItem()) {
                    availableItems.add(pedestal.getItem().copy());
                    sourcePositions.add(p);
                }
            }
            if (availableItems.isEmpty()) return false;
        }

        // ----- 配方匹配 -----
        RecipeManager recipeManager = level.getRecipeManager();
        List<CraftingRecipe> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        CraftingRecipe matchedRecipe = null;
        // 记录匹配到的配方中每个非空槽位对应的原料索引（与ingredients列表对应）
        List<Integer> ingredientSlots = new ArrayList<>(); // 槽位索引（0~8）
        List<Ingredient> requiredIngredients = new ArrayList<>();
        for (CraftingRecipe recipe : recipes) {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            // 收集所有非空槽位及其原料
            List<Integer> slots = new ArrayList<>();
            List<Ingredient> required = new ArrayList<>();
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ing = ingredients.get(i);
                if (!ing.isEmpty()) {
                    slots.add(i);
                    required.add(ing);
                }
            }
            if (required.isEmpty()) continue;
            if (!canCraft(availableItems, required)) continue;
            ItemStack result = recipe.assemble(new EmptyCraftingContainer(), level.registryAccess());
            if (result.isEmpty()) continue;
            if (targetStack != null && !matchesTarget(result, targetStack)) continue;
            matchedRecipe = recipe;
            ingredientSlots = slots;
            requiredIngredients = required;
            break;
        }
        if (matchedRecipe == null) return false;

        // 主祭台过滤
        BlockEntity beCore = level.getBlockEntity(corePos);
        if (!(beCore instanceof PackedMudPedestalBlockEntity corePedestal)) return false;
        List<String> coreFilter = corePedestal.getFilterData();
        ItemStack result = matchedRecipe.assemble(new EmptyCraftingContainer(), level.registryAccess());
        if (!coreFilter.isEmpty() && !PackedMudPedestalBlockEntity.matchesFilter(result, coreFilter)) return false;

        // ----- 匹配原料并记录每个消耗的来源索引和槽位 -----
        List<ItemStack> consumedStacks = new ArrayList<>();      // 每个消耗的物品（数量1）
        List<Integer> slotIndices = new ArrayList<>();           // 对应配方中的槽位索引
        List<Integer> sourceIndices = new ArrayList<>();         // 对应 sourcePositions 的索引
        List<ItemStack> tempItems = availableItems.stream().map(ItemStack::copy).collect(Collectors.toList());

        for (int slotIdx : ingredientSlots) {
            Ingredient ing = matchedRecipe.getIngredients().get(slotIdx);
            if (ing.isEmpty()) continue;
            boolean found = false;
            for (int i = 0; i < tempItems.size(); i++) {
                ItemStack stack = tempItems.get(i);
                if (!stack.isEmpty() && ing.test(stack)) {
                    stack.shrink(1);
                    consumedStacks.add(new ItemStack(stack.getItem(), 1));
                    slotIndices.add(slotIdx);
                    sourceIndices.add(i);
                    if (stack.isEmpty()) tempItems.set(i, ItemStack.EMPTY);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        // ----- 构建虚拟 3×3 容器，按配方槽位填充消耗的物品 -----
        CraftingContainer dummyContainer = new TransientCraftingContainer(
                new AbstractContainerMenu(null, 0) {
                    @Override public boolean stillValid(Player p) { return true; }
                    @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
                }, 3, 3
        );
        for (int j = 0; j < slotIndices.size(); j++) {
            int slot = slotIndices.get(j);
            dummyContainer.setItem(slot, consumedStacks.get(j).copy());
        }

        // 获取返还物品列表（长度9，按槽位）
        NonNullList<ItemStack> remaining = matchedRecipe.getRemainingItems(dummyContainer);

        // ----- 分配返还到基座（单基座：弹出；多基座：设置到对应基座）-----
        if (isSingle) {
            // 单基座：所有返还直接掉落
            for (int j = 0; j < sourceIndices.size(); j++) {
                int slot = slotIndices.get(j);
                ItemStack returnStack = remaining.get(slot);
                if (!returnStack.isEmpty()) {
                    popResource(level, corePos.above(), returnStack);
                }
            }
            corePedestal.clearItem();
            corePedestal.setItem(result);
            corePedestal.setChanged();
        } else {
            // 多基座：将返还物品设置到对应来源基座
            for (int j = 0; j < sourceIndices.size(); j++) {
                int srcIdx = sourceIndices.get(j);
                BlockPos targetPos = sourcePositions.get(srcIdx);
                int slot = slotIndices.get(j);
                ItemStack returnStack = remaining.get(slot);
                epca.LOGGER.info("Returning {} to {} (slot {})", returnStack, targetPos, slot);

                BlockEntity be = level.getBlockEntity(targetPos);
                if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                    pedestal.clearItem(); // 先清空
                    if (!returnStack.isEmpty()) {
                        pedestal.setItem(returnStack.copy());
                    }
                    pedestal.setChanged();
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendBlockUpdated(targetPos, level.getBlockState(targetPos), level.getBlockState(targetPos), 3);
                    }
                } else {
                    epca.LOGGER.warn("Target {} is not a pedestal!", targetPos);
                }
            }

            // 主祭台产物（原有）
            if (corePedestal.hasItem()) {
                popResource(level, corePos, corePedestal.getItem());
                corePedestal.clearItem();
            }
            corePedestal.setItem(result);
            corePedestal.setChanged();
            // 同步所有基座
            if (level instanceof ServerLevel serverLevel) {
                for (BlockPos p : pedestalPositions) {
                    serverLevel.sendBlockUpdated(p, level.getBlockState(p), level.getBlockState(p), 3);
                }
            }
        }
        return true;
    }

    // 辅助：检查物品列表是否满足原料需求（忽略顺序）
    private boolean canCraft(List<ItemStack> available, List<Ingredient> required) {
        // 深拷贝可用物品列表（以便修改）
        List<ItemStack> remaining = available.stream().map(ItemStack::copy).collect(Collectors.toList());
        for (Ingredient ing : required) {
            boolean found = false;
            for (int i = 0; i < remaining.size(); i++) {
                ItemStack stack = remaining.get(i);
                if (ing.test(stack)) {
                    // 消耗一个
                    remaining.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    // 辅助：检查输出是否与目标匹配（精确或标签）
    private boolean matchesTarget(ItemStack result, ItemStack target) {
        if (target.getItem().equals(result.getItem())) {
            return true;
        }
        // 检查共同标签
        Set<TagKey<Item>> targetTags = target.getItem().builtInRegistryHolder().tags().collect(Collectors.toSet());
        Set<TagKey<Item>> resultTags = result.getItem().builtInRegistryHolder().tags().collect(Collectors.toSet());
        targetTags.retainAll(resultTags);
        return !targetTags.isEmpty();
    }

    // 空容器（用于仅获取配方结果，不依赖实际容器）
    private static class EmptyCraftingContainer extends TransientCraftingContainer {
        public EmptyCraftingContainer() {
            super(new AbstractContainerMenu(null, 0) {
                @Override
                public boolean stillValid(Player p) { return true; }
                @Override
                public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
            }, 3, 3);
        }
    }

    // -------- 结构检测（已简化，移除网格映射） --------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PackedMudPedestalBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PackedMudPedestalBlockEntity pedestal && pedestal.hasItem()) {
                popResource(level, pos, pedestal.getItem());
                pedestal.clearItem();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public boolean isLocked(Level level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        FluidState aboveFluid = level.getFluidState(abovePos);
        // 允许空气和流体
        if (aboveState.isAir() || !aboveFluid.isEmpty()) {
            return false;
        }
        // 非完整方块允许（例如台阶、楼梯）
        if (!aboveState.isCollisionShapeFullBlock(level, abovePos)) {
            return false;
        }
        // 只有完整固体方块才锁定
        return true;
    }

    @Nullable
    private AltarStructureData findAltarStructure(Level level, BlockPos startPos) {
        Block startBlock = level.getBlockState(startPos).getBlock();
        if (!isAltarBlock(startBlock)) return null;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        final int maxXOffset = 8, maxYOffset = 16, maxZOffset = 8;
        final BlockPos origin = startPos;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (BlockPos neighbor : getSixNeighbors(current)) {
                if (Math.abs(neighbor.getX() - origin.getX()) > maxXOffset ||
                        Math.abs(neighbor.getY() - origin.getY()) > maxYOffset ||
                        Math.abs(neighbor.getZ() - origin.getZ()) > maxZOffset) {
                    continue;
                }
                if (!visited.contains(neighbor)) {
                    Block neighborBlock = level.getBlockState(neighbor).getBlock();
                    if (isAltarBlock(neighborBlock)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        if (visited.isEmpty()) return null;

        List<BlockPos> pedestalPositions = new ArrayList<>();
        Set<BlockPos> altarStonePositions = new HashSet<>();
        int totalPoints = 0;

        for (BlockPos pos : visited) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            totalPoints += AltarPointManager.getPoints(block);
            if (state.is(PEDESTAL_TAG)) {
                pedestalPositions.add(pos);
            } else if (state.is(ALTAR_STONE_TAG)) {
                altarStonePositions.add(pos);
            }
        }

        int pedestalCount = pedestalPositions.size();
        BlockPos mainPedestal = startPos;
        if (!pedestalPositions.contains(mainPedestal)) {
            return null;
        }

        boolean valid = true;
        String invalidReason = null;

        // 1. 总祭台数量 1~21
        if (pedestalCount < 1 || pedestalCount > MAX_PEDESTAL_TOTAL) {
            valid = false;
            return new AltarStructureData(visited, totalPoints, pedestalCount, false, invalidReason, mainPedestal, null, 0, false);
        }

        // 2. 上方遮挡
        for (BlockPos p : pedestalPositions) {
            BlockPos above = p.above();
            BlockState aboveState = level.getBlockState(above);
            FluidState aboveFluid = level.getFluidState(above);
            if (!aboveState.isAir() && aboveFluid.isEmpty() && aboveState.isCollisionShapeFullBlock(level, above)) {
                valid = false;
                return new AltarStructureData(visited, totalPoints, pedestalCount, false, invalidReason, mainPedestal, null, 0, false);
            }
        }

        // 3. 主祭台与其他祭台间距 ≥ 2
        for (BlockPos p : pedestalPositions) {
            if (p.equals(mainPedestal)) continue;
            int dx = Math.abs(p.getX() - mainPedestal.getX());
            int dy = Math.abs(p.getY() - mainPedestal.getY());
            int dz = Math.abs(p.getZ() - mainPedestal.getZ());
            int maxDist = Math.max(dx, Math.max(dy, dz));
            if (maxDist < 2) {
                valid = false;
                return new AltarStructureData(visited, totalPoints, pedestalCount, false, invalidReason, mainPedestal, null, 0, false);
            }
        }

        // 4. 多个祭台需有祭台石
        if (pedestalCount > 1 && altarStonePositions.isEmpty()) {
            valid = false;
            return new AltarStructureData(visited, totalPoints, pedestalCount, false, invalidReason, mainPedestal, null, 0, false);
        }

        // 统计有物品的祭台数量
        int itemCount = 0;
        for (BlockPos p : pedestalPositions) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof PackedMudPedestalBlockEntity pedestal && pedestal.hasItem()) {
                itemCount++;
            }
        }

        return new AltarStructureData(visited, totalPoints, pedestalCount, true, null,
                mainPedestal, pedestalPositions, itemCount, true);
    }

    private boolean isAltarBlock(Block block) {
        return block.defaultBlockState().is(PEDESTAL_TAG) || block.defaultBlockState().is(ALTAR_STONE_TAG);
    }

    private Collection<BlockPos> getSixNeighbors(BlockPos pos) {
        return Arrays.asList(
                pos.north(), pos.south(), pos.west(), pos.east(),
                pos.above(), pos.below()
        );
    }

    // -------- 内部数据类 --------

    private static class AltarStructureData {
        final Set<BlockPos> allPositions;
        final int totalPoints;
        final int pedestalCount;
        final boolean isValid;
        final String invalidReason;
        final BlockPos mainPedestal;
        final List<BlockPos> pedestalPositions;
        final int pedestalsWithItem;
        final boolean isValidForCrafting;

        AltarStructureData(Set<BlockPos> allPositions, int totalPoints, int pedestalCount,
                           boolean isValid, String invalidReason, BlockPos mainPedestal,
                           List<BlockPos> pedestalPositions, int pedestalsWithItem,
                           boolean isValidForCrafting) {
            this.allPositions = allPositions;
            this.totalPoints = totalPoints;
            this.pedestalCount = pedestalCount;
            this.isValid = isValid;
            this.invalidReason = invalidReason;
            this.mainPedestal = mainPedestal;
            this.pedestalPositions = pedestalPositions != null ? pedestalPositions : Collections.emptyList();
            this.pedestalsWithItem = pedestalsWithItem;
            this.isValidForCrafting = isValidForCrafting;
        }
    }

    // -------- 红石信号 --------

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PackedMudPedestalBlockEntity pedestal) {
            return pedestal.hasItem() ? 15 : 0;
        }
        return 0;
    }

    /**
     * 将结构内所有祭台的主祭台标记清除，并将指定祭台设为主祭台
     */
    private void setMainPedestalInStructure(Level level, BlockPos pos) {
        AltarStructureData data = findAltarStructure(level, pos);
        if (data != null) {
            for (BlockPos p : data.pedestalPositions) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                    pedestal.setMainPedestal(p.equals(pos));
                }
            }
        } else {
            // 独立祭台，只设置自身
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                pedestal.setMainPedestal(true);
            }
        }
    }

    /**
     * 清除结构内所有祭台的主祭台标记
     */
    private void clearMainPedestalInStructure(Level level, BlockPos pos) {
        AltarStructureData data = findAltarStructure(level, pos);
        if (data != null) {
            for (BlockPos p : data.pedestalPositions) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                    pedestal.setMainPedestal(false);
                }
            }
        } else {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                pedestal.setMainPedestal(false);
            }
        }
    }
}