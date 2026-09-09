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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
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
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.PackedMudPedestalBlockEntity;
import org.tdddd.epca.impl.overworld.data.AltarPointManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.items.item.SmallItemFrame;

import java.util.*;
import java.util.stream.Collectors;

public class PackedMudPedestal extends BaseEntityBlock {
    protected static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final int MAX_PEDESTAL_TOTAL = 21;

    public static final TagKey<Block> PEDESTAL_TAG = TagKey.create(Registries.BLOCK,
            new ResourceLocation(epca.MODID, "pedestals"));
    public static final TagKey<Block> ALTAR_STONE_TAG = TagKey.create(Registries.BLOCK,
            new ResourceLocation(epca.MODID, "altar_stones"));

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
        if (tryPerformSacrifice(level, pos, player)) {
            return InteractionResult.SUCCESS;
        }
        if (heldItem.getItem() instanceof SmallItemFrame) {
            if (level.getBlockEntity(pos) instanceof PackedMudPedestalBlockEntity pedestal) {
                String filterData = "";
                if (heldItem.hasTag() && heldItem.getTag().contains("item_data")) {
                    filterData = heldItem.getTag().getString("item_data");
                }
                pedestal.setFilterData(SmallItemFrame.getItemIds(heldItem));
                if (filterData.isEmpty()) {
                    clearMainPedestalInStructure(level, pos);
                }
                level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

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

        if (heldItem.isEmpty()) {
            AltarStructureData data = findAltarStructure(level, pos);
            if (data != null && data.isValidForCrafting && data.mainPedestal != null && data.mainPedestal.equals(pos)) {
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

        RecipeManager recipeManager = level.getRecipeManager();
        List<CraftingRecipe> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        CraftingRecipe matchedRecipe = null;
        List<Integer> ingredientSlots = new ArrayList<>();
        List<Ingredient> requiredIngredients = new ArrayList<>();
        for (CraftingRecipe recipe : recipes) {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
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

        List<ItemStack> consumedStacks = new ArrayList<>();
        List<Integer> slotIndices = new ArrayList<>();
        List<Integer> sourceIndices = new ArrayList<>();
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

        NonNullList<ItemStack> remaining = matchedRecipe.getRemainingItems(dummyContainer);

        if (isSingle) {
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
            for (int j = 0; j < sourceIndices.size(); j++) {
                int srcIdx = sourceIndices.get(j);
                BlockPos targetPos = sourcePositions.get(srcIdx);
                int slot = slotIndices.get(j);
                ItemStack returnStack = remaining.get(slot);

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
                }
            }

            if (corePedestal.hasItem()) {
                popResource(level, corePos, corePedestal.getItem());
                corePedestal.clearItem();
            }
            corePedestal.setItem(result);
            corePedestal.setChanged();
            if (level instanceof ServerLevel serverLevel) {
                for (BlockPos p : pedestalPositions) {
                    serverLevel.sendBlockUpdated(p, level.getBlockState(p), level.getBlockState(p), 3);
                }
            }
        }
        return true;
    }

    private boolean canCraft(List<ItemStack> available, List<Ingredient> required) {
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

    private boolean matchesTarget(ItemStack result, ItemStack target) {
        if (target.getItem().equals(result.getItem())) {
            return true;
        }
        Set<TagKey<Item>> targetTags = target.getItem().builtInRegistryHolder().tags().collect(Collectors.toSet());
        Set<TagKey<Item>> resultTags = result.getItem().builtInRegistryHolder().tags().collect(Collectors.toSet());
        targetTags.retainAll(resultTags);
        return !targetTags.isEmpty();
    }

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
        if (aboveState.isAir() || !aboveFluid.isEmpty()) {
            return false;
        }
        if (!aboveState.isCollisionShapeFullBlock(level, abovePos)) {
            return false;
        }
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

        if (pedestalCount < 1 || pedestalCount > MAX_PEDESTAL_TOTAL) {
            valid = false;
            return new AltarStructureData(visited, totalPoints, pedestalCount, false, invalidReason, mainPedestal, null, 0, false);
        }

        for (BlockPos p : pedestalPositions) {
            BlockPos above = p.above();
            BlockState aboveState = level.getBlockState(above);
            FluidState aboveFluid = level.getFluidState(above);
            if (!aboveState.isAir() && aboveFluid.isEmpty() && aboveState.isCollisionShapeFullBlock(level, above)) {
                valid = false;
                return new AltarStructureData(visited, totalPoints, pedestalCount, false, invalidReason, mainPedestal, null, 0, false);
            }
        }

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

        if (pedestalCount > 1 && altarStonePositions.isEmpty()) {
            valid = false;
            return new AltarStructureData(visited, totalPoints, pedestalCount, false, invalidReason, mainPedestal, null, 0, false);
        }

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
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                pedestal.setMainPedestal(true);
            }
        }
    }

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

    //献祭仪式
    private boolean tryPerformSacrifice(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (!isBeaconBaseBlock(belowState)) return false;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PackedMudPedestalBlockEntity pedestal)) return false;
        ItemStack stored = pedestal.getItem();
        if (!isGemBlockItem(stored)) return false;

        List<Animal> animals = new ArrayList<>();
        List<LivingEntity> villagers = new ArrayList<>();
        double radius = 1.5;
        List<Entity> entities = level.getEntities(null, new AABB(pos).inflate(radius));
        for (Entity e : entities) {
            if (e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= radius * radius) {
                if (IParasite.isParasiteNoLivingByTagOrInterface(e)) {
                    return false;
                }
                if (!e.isAlive()) continue;
                if (e instanceof Animal && !(e instanceof Monster)) {
                    animals.add((Animal) e);
                } else if (isVillagerLike(e) && e instanceof LivingEntity) {
                    villagers.add((LivingEntity) e);
                }
            }
        }
        if (animals.size() < 2 || villagers.size() < 1) return false;

        performSacrifice(serverLevel, pos, player, animals, villagers);
        return true;
    }

    private void performSacrifice(ServerLevel level, BlockPos pos, Player player,
                                  List<Animal> animals, List<LivingEntity> villagers) {
        int animalRemoved = 0;
        for (Animal a : animals) {
            if (animalRemoved >= 2) break;
            a.remove(Entity.RemovalReason.DISCARDED);
            animalRemoved++;
        }
        if (!villagers.isEmpty()) {
            villagers.get(0).remove(Entity.RemovalReason.DISCARDED);
        }

        List<BlockPos> positions = new ArrayList<>();
        int maxRadius = 72;
        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dy = -maxRadius; dy <= maxRadius; dy++) {
                for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                    BlockPos targetPos = pos.offset(dx, dy, dz);
                    double dist = Math.sqrt(pos.distSqr(targetPos));
                    if (dist > maxRadius) continue;
                    BlockState state = level.getBlockState(targetPos);
                    if (state.isAir()) continue;
                    positions.add(targetPos);
                }
            }
        }

        positions.sort(Comparator.comparingDouble(p -> p.distSqr(pos)));

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PackedMudPedestalBlockEntity pedestal) {
            pedestal.clearItem();
        }

        BlockConversionManager.getInstance().addSacrificeTask(level, pos, player.getUUID(), positions);
    }

    private boolean isBeaconBaseBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK ||
                block == Blocks.DIAMOND_BLOCK || block == Blocks.EMERALD_BLOCK ||
                block == Blocks.NETHERITE_BLOCK;
    }

    private boolean isGemBlockItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.DIAMOND_BLOCK || item == Items.EMERALD_BLOCK || item == Items.AMETHYST_BLOCK;
    }

    private boolean isVillagerLike(Entity entity) {
        return  entity instanceof AbstractVillager || entity instanceof Pillager ||
                entity instanceof Witch || entity instanceof Vindicator ||
                entity instanceof Evoker;
    }
}