package org.tdddd.epca.impl.overworld.registry.blocks;

import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedFloweringLeaves;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedLeaves;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedLilyPad;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedResidue;
import org.tdddd.epca.impl.epca;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class BlockConversionManager {
    private static final BlockConversionManager INSTANCE = new BlockConversionManager();
    private final Gson gson = new Gson();

    
    private StageConfig stageIConfig = new StageConfig();
    
    private StageConfig stageIIConfig = new StageConfig();
    private StageConfig generalConfig = new StageConfig();

    private final Map<ServerLevel, Set<BlockPos>> pendingDoublePlantConversions = new HashMap<>();
    private final Map<BlockPos, LeavesTask> leavesConversionQueue = new HashMap<>();

    private BlockConversionManager() {
        loadStageIConfig();
        loadStageIIConfig();
        loadGeneralConfig();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static BlockConversionManager getInstance() {
        return INSTANCE;
    }

    
    private void loadStageIConfig() {
        stageIConfig = loadConfig("stage_i_block_conversions.json");
        if (stageIConfig == null) {
            stageIConfig = new StageConfig(); 
        }
    }

    private void loadStageIIConfig() {
        stageIIConfig = loadConfig("stage_ii_block_conversions.json");
        if (stageIIConfig == null) {
            stageIIConfig = new StageConfig();
        }
    }

    private void loadGeneralConfig() {
        generalConfig = loadConfig("general_block_conversions.json");
        if (generalConfig == null) {
            generalConfig = new StageConfig();
        }
    }

    private StageConfig loadConfig(String fileName) {
        try {
            ResourceLocation loc = new ResourceLocation(epca.MODID, fileName);
            InputStream inputStream = BlockConversionManager.class.getClassLoader()
                    .getResourceAsStream("data/" + loc.getNamespace() + "/block_conversions/" + loc.getPath());
            if (inputStream != null) {
                StageConfig config = gson.fromJson(new InputStreamReader(inputStream), StageConfig.class);
                inputStream.close();
                if (config.conversions == null) config.conversions = new HashMap<>();
                return config;
            } else {
            }
        } catch (Exception e) {
        }
        return null;
    }

    
    private static class StageConfig {
        Map<String, String> conversions = new HashMap<>();
        int plant_radius = 1;
        int leaves_radius = 3;
        int leaves_interval = 5;
    }


    public boolean convertBlockUsingStageIConfig(ServerLevel level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0f || hardness > 2.0f) {
            return false; // 不转化
        }
        return convertBlockUsingMap(level, pos, state, stageIConfig.conversions);
    }

    public void convertPlantsInRangeForStageI(ServerLevel level, BlockPos center) {
        convertPlantsInRange(level, center, stageIConfig.plant_radius);
    }

    public void scheduleLeavesConversionForStageI(ServerLevel level, BlockPos pos) {
        scheduleLeavesConversion(level, pos, stageIConfig.leaves_interval, stageIConfig.leaves_radius);
    }

    public void convertNearbyLeavesForStageI(ServerLevel level, BlockPos center) {
        convertNearbyLeaves(level, center, stageIConfig.leaves_radius);
    }

    
    public boolean convertBlockUsingStageIIConfig(ServerLevel level, BlockPos pos, BlockState state) {
        return convertBlockUsingMap(level, pos, state, stageIIConfig.conversions);
    }

    public void convertPlantsInRangeForStageII(ServerLevel level, BlockPos center) {
        convertPlantsInRange(level, center, stageIIConfig.plant_radius);
    }

    public void scheduleLeavesConversionForStageII(ServerLevel level, BlockPos pos) {
        scheduleLeavesConversion(level, pos, stageIIConfig.leaves_interval, stageIIConfig.leaves_radius);
    }

    public void convertNearbyLeavesForStageII(ServerLevel level, BlockPos center) {
        convertNearbyLeaves(level, center, stageIIConfig.leaves_radius);
    }
    
    public boolean convertBlockUsingGeneralConfig(ServerLevel level, BlockPos pos, BlockState state) {
        return convertBlockUsingMap(level, pos, state, generalConfig.conversions);
    }

    public void convertPlantsInRangeForGeneral(ServerLevel level, BlockPos center) {
        convertPlantsInRange(level, center, generalConfig.plant_radius);
    }

    public void scheduleLeavesConversionForGeneral(ServerLevel level, BlockPos pos) {
        scheduleLeavesConversion(level, pos, generalConfig.leaves_interval, generalConfig.leaves_radius);
    }

    public void convertNearbyLeavesForGeneral(ServerLevel level, BlockPos center) {
        convertNearbyLeaves(level, center, generalConfig.leaves_radius);
    }

    
    private boolean convertBlockUsingMap(ServerLevel level, BlockPos pos, BlockState state, Map<String, String> map) {

        Block originalBlock = state.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(originalBlock);
        String fullBlockId = blockId.toString();
        String targetBlockId = map.get(fullBlockId);

        if (targetBlockId != null) {
            Block targetBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(targetBlockId));
            if (targetBlock != null && targetBlock != Blocks.AIR) {
                BlockState newState = targetBlock.defaultBlockState();
                newState = copyCommonBlockProperties(state, newState);
                level.setBlock(pos, newState, 3);
                // 发送添加包给附近玩家（或所有玩家）
                sendInfestedPacketToClients(level, pos, true);
                afterBlockConverted(level, pos, newState);
                return true;
            }
        } else {
            if (!level.getGameRules().getBoolean(epca.DO_INFESTED_FALLBACK)) {
                return false;
            }
            
            if (isInfestedBlock(state)) {
                return false;
            }
            
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                return false;
            }
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            if (id.getNamespace().equals("minecraft") || id.getNamespace().equals("epca")) {
                return false;
            }
            float hardness = state.getDestroySpeed(level, pos);

            if (hardness == 0.0f || !state.isCollisionShapeFullBlock(level, pos)) {
                int layers = 2 + level.random.nextInt(5); 
                BlockState residueState = ModBlocks.INFESTED_RESIDUE.get().defaultBlockState()
                        .setValue(InfestedResidue.LAYERS, layers);
                level.setBlock(pos, residueState, 3);
                sendInfestedPacketToClients(level, pos, true);
                afterBlockConverted(level, pos, residueState);
                return true;
            }
            if (hardness > 0.0f && hardness < 1.5f && state.isCollisionShapeFullBlock(level, pos)) {
                BlockState residueState = ModBlocks.INFESTED_DUSTLIKE.get().defaultBlockState();
                level.setBlock(pos, residueState, 3);
                sendInfestedPacketToClients(level, pos, true);
                afterBlockConverted(level, pos, residueState);
                return true;
            }
            if (hardness == 2.0f && state.isCollisionShapeFullBlock(level, pos)) {
                BlockState residueState = ModBlocks.INFESTED_PLANKSLIKE.get().defaultBlockState();
                level.setBlock(pos, residueState, 3);
                sendInfestedPacketToClients(level, pos, true);
                afterBlockConverted(level, pos, residueState);
                return true;
            }
            if (hardness >= 1.5f && hardness <= 3.5f && hardness != 2.0f && state.isCollisionShapeFullBlock(level, pos)) {
                BlockState residueState = ModBlocks.INFESTED_ROCKLIKE.get().defaultBlockState();
                level.setBlock(pos, residueState, 3);
                sendInfestedPacketToClients(level, pos, true);
                afterBlockConverted(level, pos, residueState);
                return true;
            }
            if (hardness > 3.5f && hardness < 50.0f && state.isCollisionShapeFullBlock(level, pos)) {
                BlockState residueState = ModBlocks.INFESTED_METALLIKE.get().defaultBlockState();
                level.setBlock(pos, residueState, 3);
                sendInfestedPacketToClients(level, pos, true);
                afterBlockConverted(level, pos, residueState);
                return true;
            }
            if ((hardness < 0.0f || hardness >= 50.0f) && state.isCollisionShapeFullBlock(level, pos)) {
                BlockState residueState = ModBlocks.INFESTED_HARDLIKE.get().defaultBlockState();
                level.setBlock(pos, residueState, 3);
                sendInfestedPacketToClients(level, pos, true);
                afterBlockConverted(level, pos, residueState);
                return true;
            }
            return false;
        }
        return false;
    }

    private void afterBlockConverted(ServerLevel level, BlockPos pos, BlockState newState) {
        if (newState.getBlock() instanceof InfestedBlockInterface) {
            convertLilyPadsAboveInfested(level, pos);
        }
    }

    /**
     * 从指定虫染方块向上扫描 7 格，将发现的原版睡莲转化为虫染睡莲。
     * @param level 服务端世界
     * @param infestedPos 虫染方块位置
     */
    public void convertLilyPadsAboveInfested(ServerLevel level, BlockPos infestedPos) {
        for (int i = 1; i <= 7; i++) {
            BlockPos checkPos = infestedPos.above(i);
            BlockState state = level.getBlockState(checkPos);
            if (state.is(Blocks.LILY_PAD)) {
                convertLilyPadWithBelow(level, checkPos);
            }
        }
    }

    /**
     * 转化指定位置的睡莲，复制下方虫染方块的朝向。
     */
    private void convertLilyPadWithBelow(ServerLevel level, BlockPos pos) {
        // 获取配置映射（使用通用配置）
        String fullBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.LILY_PAD).toString();
        String targetBlockId = generalConfig.conversions.get(fullBlockId);
        Block targetBlock = null;
        if (targetBlockId != null) {
            targetBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(targetBlockId));
        }
        if (targetBlock == null || targetBlock == Blocks.AIR) return;

        BlockState newState = targetBlock.defaultBlockState();

        if (newState.hasProperty(InfestedLilyPad.NATURAL_SPAWN)) {
            newState = newState.setValue(InfestedLilyPad.NATURAL_SPAWN, true);
        }

        level.setBlock(pos, newState, 3);
        sendInfestedPacketToClients(level, pos, true);
    }

    private BlockState copyCommonBlockProperties(BlockState source, BlockState target) {
        
        if (source.hasProperty(RotatedPillarBlock.AXIS) && target.hasProperty(RotatedPillarBlock.AXIS)) {
            target = target.setValue(RotatedPillarBlock.AXIS, source.getValue(RotatedPillarBlock.AXIS));
        }
        
        if (source.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE) &&
                target.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
            target = target.setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                    source.getValue(net.minecraft.world.level.block.SlabBlock.TYPE));
        }
        if (source.hasProperty(net.minecraft.world.level.block.SlabBlock.WATERLOGGED) &&
                target.hasProperty(net.minecraft.world.level.block.SlabBlock.WATERLOGGED)) {
            target = target.setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED,
                    source.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED));
        }
        
        if (source.hasProperty(net.minecraft.world.level.block.StairBlock.FACING) &&
                target.hasProperty(net.minecraft.world.level.block.StairBlock.FACING)) {
            target = target.setValue(net.minecraft.world.level.block.StairBlock.FACING,
                    source.getValue(net.minecraft.world.level.block.StairBlock.FACING));
        }
        if (source.hasProperty(net.minecraft.world.level.block.StairBlock.HALF) &&
                target.hasProperty(net.minecraft.world.level.block.StairBlock.HALF)) {
            target = target.setValue(net.minecraft.world.level.block.StairBlock.HALF,
                    source.getValue(net.minecraft.world.level.block.StairBlock.HALF));
        }
        if (source.hasProperty(net.minecraft.world.level.block.StairBlock.SHAPE) &&
                target.hasProperty(net.minecraft.world.level.block.StairBlock.SHAPE)) {
            target = target.setValue(net.minecraft.world.level.block.StairBlock.SHAPE,
                    source.getValue(net.minecraft.world.level.block.StairBlock.SHAPE));
        }
        if (source.hasProperty(net.minecraft.world.level.block.StairBlock.WATERLOGGED) &&
                target.hasProperty(net.minecraft.world.level.block.StairBlock.WATERLOGGED)) {
            target = target.setValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED,
                    source.getValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED));
        }
        
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.UP) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.UP)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.UP,
                    source.getValue(net.minecraft.world.level.block.WallBlock.UP));
        }
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.NORTH_WALL) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.NORTH_WALL)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL,
                    source.getValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL));
        }
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.SOUTH_WALL) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.SOUTH_WALL)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL,
                    source.getValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL));
        }
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.EAST_WALL) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.EAST_WALL)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.EAST_WALL,
                    source.getValue(net.minecraft.world.level.block.WallBlock.EAST_WALL));
        }
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.WEST_WALL) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.WEST_WALL)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.WEST_WALL,
                    source.getValue(net.minecraft.world.level.block.WallBlock.WEST_WALL));
        }
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.WATERLOGGED) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.WATERLOGGED)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.WATERLOGGED,
                    source.getValue(net.minecraft.world.level.block.WallBlock.WATERLOGGED));
        }
        
        if (source.hasProperty(FenceBlock.NORTH) && target.hasProperty(FenceBlock.NORTH)) {
            target = target.setValue(FenceBlock.NORTH, source.getValue(FenceBlock.NORTH));
        }
        if (source.hasProperty(FenceBlock.SOUTH) && target.hasProperty(FenceBlock.SOUTH)) {
            target = target.setValue(FenceBlock.SOUTH, source.getValue(FenceBlock.SOUTH));
        }
        if (source.hasProperty(FenceBlock.EAST) && target.hasProperty(FenceBlock.EAST)) {
            target = target.setValue(FenceBlock.EAST, source.getValue(FenceBlock.EAST));
        }
        if (source.hasProperty(FenceBlock.WEST) && target.hasProperty(FenceBlock.WEST)) {
            target = target.setValue(FenceBlock.WEST, source.getValue(FenceBlock.WEST));
        }
        
        if (source.hasProperty(PointedDripstoneBlock.TIP_DIRECTION) && target.hasProperty(PointedDripstoneBlock.TIP_DIRECTION)) {
            target = target.setValue(PointedDripstoneBlock.TIP_DIRECTION, source.getValue(PointedDripstoneBlock.TIP_DIRECTION));
        }
        if (source.hasProperty(PointedDripstoneBlock.THICKNESS) && target.hasProperty(PointedDripstoneBlock.THICKNESS)) {
            target = target.setValue(PointedDripstoneBlock.THICKNESS, source.getValue(PointedDripstoneBlock.THICKNESS));
        }
        
        if (source.hasProperty(SnowLayerBlock.LAYERS) && target.hasProperty(SnowLayerBlock.LAYERS)) {
            target = target.setValue(SnowLayerBlock.LAYERS, source.getValue(SnowLayerBlock.LAYERS));
        }
        
        if (source.hasProperty(BlockStateProperties.WATERLOGGED) && target.hasProperty(BlockStateProperties.WATERLOGGED)) {
            target = target.setValue(BlockStateProperties.WATERLOGGED, source.getValue(BlockStateProperties.WATERLOGGED));
        }
        return target;
    }

    private void scheduleDoublePlantConversion(ServerLevel level, BlockPos lowerPos) {
        synchronized (pendingDoublePlantConversions) {
            pendingDoublePlantConversions.computeIfAbsent(level, k -> new HashSet<>()).add(lowerPos);
        }
    }

    private void applyPendingDoublePlantConversions(ServerLevel level, Set<BlockPos> lowerPositions) {
        for (BlockPos lowerPos : lowerPositions) {
            // 1. 检查下半是否仍有效
            BlockState lowerState = level.getBlockState(lowerPos);
            if (!lowerState.is(Blocks.TALL_GRASS) && !lowerState.is(Blocks.LARGE_FERN)) continue;
            if (lowerState.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER) continue;

            BlockPos upperPos = lowerPos.above();
            BlockState upperState = level.getBlockState(upperPos);
            // 2. 检查上半是否可被替换（允许被原版上半覆盖或空气）
            if (!upperState.isAir() && !upperState.canBeReplaced() && !upperState.is(lowerState.getBlock())) {
                continue; // 结构不完整或被占用，放弃转换
            }

            // 3. 确定目标虫染方块
            RegistryObject<Block> targetBlock = lowerState.is(Blocks.TALL_GRASS) ?
                    ModBlocks.INFESTED_TALL_GRASS : ModBlocks.INFESTED_TALL_FERN;

            // 4. 移除旧方块（先上半，再下半，使用标志 2 抑制邻居更新）
            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), 2);

            // 5. 放置虫染方块（先下半，再上半，使用标志 3 正常通知）
            BlockState newLower = targetBlock.get().defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
            BlockState newUpper = targetBlock.get().defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);

            level.setBlock(lowerPos, newLower, 3);
            level.setBlock(upperPos, newUpper, 3);

            sendInfestedPacketToClients(level, lowerPos, true);
            sendInfestedPacketToClients(level, upperPos, true);
        }
    }

    // Tick 事件处理：在服务器 Tick 结束时处理延迟队列
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pendingDoublePlantConversions.isEmpty()) return;

        // 复制一份当前队列，避免遍历时修改
        Map<ServerLevel, Set<BlockPos>> toProcess;
        synchronized (pendingDoublePlantConversions) {
            if (pendingDoublePlantConversions.isEmpty()) return;
            toProcess = new HashMap<>(pendingDoublePlantConversions);
            pendingDoublePlantConversions.clear();
        }

        for (Map.Entry<ServerLevel, Set<BlockPos>> entry : toProcess.entrySet()) {
            ServerLevel level = entry.getKey();
            if (ServerLifecycleHooks.getCurrentServer().getLevel(level.dimension()) == null) continue;
            applyPendingDoublePlantConversions(level, entry.getValue());
        }
    }

    private void convertPlantsInRange(ServerLevel level, BlockPos center, int radius) {
        // 先检查 3x3x3 范围内是否有双格植物
        boolean hasDoublePlant = false;
        int verticalRadius = radius; // 默认垂直半径等于水平半径
        outer:
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN)) {
                        hasDoublePlant = true;
                        break outer;
                    }
                }
            }
        }
        // 如果存在双格植物，增大垂直半径
        if (hasDoublePlant) {
            verticalRadius = radius + 1;
        }
        // 执行实际转换
        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    convertPlantAtPosition(level, checkPos, state);
                }
            }
        }
    }

    public void convertPlantAtPosition(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.GRASS)) {
            BlockState targetState;
            if (level.random.nextFloat() < 0.35f) {
                targetState = ModBlocks.INFESTED_SHORT_GRASS.get().defaultBlockState();
            } else {
                targetState = ModBlocks.INFESTED_GRASS.get().defaultBlockState();
            }
            level.setBlock(pos, targetState, 3);
            return;
        }
        if (state.is(Blocks.FERN)) {
            level.setBlock(pos, ModBlocks.INFESTED_FERN.get().defaultBlockState(), 3);
            return;
        }

        if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN)) {
            BlockPos lowerPos = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ?
                    pos.below() : pos;
            scheduleDoublePlantConversion(level, lowerPos);
        }
    }

    public void convertVineToInfested(Level level, BlockPos pos, BlockState vineState) {
        BlockState infestedVineState = ModBlocks.INFESTED_VINE.get().defaultBlockState();
        for (Direction dir : Direction.values()) {
            BooleanProperty property = net.minecraft.world.level.block.VineBlock.getPropertyForFace(dir);
            if (property != null && vineState.hasProperty(property) && vineState.getValue(property)) {
                infestedVineState = infestedVineState.setValue(property, true);
            }
        }
        level.setBlock(pos, infestedVineState, 3);
    }

    private void scheduleLeavesConversion(ServerLevel level, BlockPos pos, int interval, int radius) {
        synchronized (leavesConversionQueue) {
            leavesConversionQueue.put(pos, new LeavesTask(interval, radius));
        }
        convertNearbyLeaves(level, pos, radius);
    }

    private void convertNearbyLeaves(ServerLevel level, BlockPos center, int radius) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(center);
        visited.add(center);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(direction);
                if (visited.contains(neighborPos)) continue;

                if (Math.abs(neighborPos.getX() - center.getX()) <= radius &&
                        Math.abs(neighborPos.getY() - center.getY()) <= radius &&
                        Math.abs(neighborPos.getZ() - center.getZ()) <= radius) {

                    visited.add(neighborPos);
                    BlockState neighborState = level.getBlockState(neighborPos);

                    if (isConvertibleLeaves(neighborState) &&
                            !(neighborState.getBlock() instanceof InfestedLeaves) &&
                            !(neighborState.getBlock() instanceof InfestedFloweringLeaves)) {
                        BlockState infestedLeavesState;
                        if (neighborState.is(Blocks.FLOWERING_AZALEA_LEAVES)) {
                            infestedLeavesState = ModBlocks.INFESTED_FLOWERING_LEAVES.get().defaultBlockState();
                        } else {
                            infestedLeavesState = level.random.nextFloat() < 0.95f ?
                                    ModBlocks.INFESTED_LEAVES.get().defaultBlockState() :
                                    ModBlocks.INFESTED_FLOWERING_LEAVES.get().defaultBlockState();
                        }
                        level.setBlock(neighborPos, infestedLeavesState, 3);
                        queue.add(neighborPos);
                    } else if (isInfestedBlock(neighborState) ||
                            neighborState.getBlock() instanceof InfestedLeaves ||
                            neighborState.getBlock() instanceof InfestedFloweringLeaves) {
                        queue.add(neighborPos);
                    }
                }
            }
        }
    }

    public boolean isConvertibleLeaves(BlockState state) {
        return state.is(Blocks.OAK_LEAVES) ||
                state.is(Blocks.SPRUCE_LEAVES) ||
                state.is(Blocks.BIRCH_LEAVES) ||
                state.is(Blocks.JUNGLE_LEAVES) ||
                state.is(Blocks.ACACIA_LEAVES) ||
                state.is(Blocks.DARK_OAK_LEAVES) ||
                state.is(Blocks.MANGROVE_LEAVES) ||
                state.is(Blocks.AZALEA_LEAVES) ||
                state.is(Blocks.FLOWERING_AZALEA_LEAVES) ||
                state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock;
    }

    public boolean isInfestedBlock(BlockState state) {
        return state.getBlock() instanceof InfestedBlockInterface;
    }

    public void removeFromQueue(BlockPos pos) {
        synchronized (leavesConversionQueue) {
            leavesConversionQueue.remove(pos);
        }
    }

    public boolean isExposed(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            FluidState neighborFluid = level.getFluidState(neighborPos);
            if (neighborState.isAir() || !neighborFluid.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> getStageIConversionMap() {
        return Collections.unmodifiableMap(stageIConfig.conversions);
    }

    public Map<String, String> getStageIIConversionMap() {
        return Collections.unmodifiableMap(stageIIConfig.conversions);
    }

    public Map<String, String> getGeneralConversionMap() {
        return Collections.unmodifiableMap(generalConfig.conversions);
    }

    private static class LeavesTask {
        int timer;
        final int interval;
        final int radius;
        LeavesTask(int interval, int radius) {
            this.timer = 0;
            this.interval = interval;
            this.radius = radius;
        }
    }

    private void sendInfestedPacketToClients(ServerLevel level, BlockPos pos, boolean add) {
        if (!level.isClientSide()) {
            if (add) {
                ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new InfestedSourcePacket.AddInfestedSourcePacket(pos));
            } else {
                ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new InfestedSourcePacket.RemoveInfestedSourcePacket(pos));
            }
        }
    }

    /**
     * 获取配置中睡莲对应的目标方块，若未配置则返回默认虫染睡莲
     */
    public Block getTargetLilyPadBlock() {
        String targetId = generalConfig.conversions.get("minecraft:lily_pad");
        if (targetId != null) {
            Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(targetId));
            if (block != null && block != Blocks.AIR) {
                return block;
            }
        }
        return Blocks.LILY_PAD;
    }

    /**
     * 检测指定位置下方七格内是否有虫染方块
     */
    @Nullable
    public BlockState findInfestedBlockBelow(Level level, BlockPos pos, int range) {
        for (int i = 1; i <= range; i++) {
            BlockPos belowPos = pos.below(i);
            BlockState belowState = level.getBlockState(belowPos);
            if (isInfestedBlock(belowState) && !belowState.isAir() && belowState.getFluidState().isEmpty()) {
                return belowState;
            }
        }
        return null;
    }
}