package org.tdddd.epca.impl.overworld.registry.blocks;

import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.api.AltarBlockTags;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.data.SacrificeSavedData;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.blocks.block.*;
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
        
        
        
        
        
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::handleServerStarted);
        NeoForge.EVENT_BUS.addListener(this::handlePlayerLoggedOut);
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
            Identifier loc = Identifier.fromNamespaceAndPath(epca.MODID, fileName);
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
            return false; 
        }
        float multiplier = 1.0f;
        if (state.is(AltarBlockTags.PEDESTAL_TAG) || state.is(AltarBlockTags.ALTAR_STONE_TAG)) {
            multiplier = 2.0f;
        }
        return convertBlockUsingMap(level, pos, state, stageIConfig.conversions, multiplier);
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
        float multiplier = 1.0f;
        if (state.is(AltarBlockTags.PEDESTAL_TAG) || state.is(AltarBlockTags.ALTAR_STONE_TAG)) {
            multiplier = 2.0f;
        }
        return convertBlockUsingMap(level, pos, state, stageIIConfig.conversions, multiplier);
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
        float multiplier = 1.0f;
        if (state.is(AltarBlockTags.PEDESTAL_TAG) || state.is(AltarBlockTags.ALTAR_STONE_TAG)) {
            multiplier = 2.0f;
        }
        return convertBlockUsingMap(level, pos, state, generalConfig.conversions, multiplier);
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
    
    private boolean convertBlockUsingMap(ServerLevel level, BlockPos pos, BlockState state, Map<String, String> map, float hardnessMultiplier) {
        Block originalBlock = state.getBlock();
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(originalBlock);
        String fullBlockId = blockId.toString();
        String targetBlockId = map.get(fullBlockId);

        if (targetBlockId != null) {
            Block targetBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(targetBlockId));
            if (targetBlock != null && targetBlock != Blocks.AIR) {
                BlockState newState = targetBlock.defaultBlockState();
                newState = copyCommonBlockProperties(state, newState);
                level.setBlock(pos, newState, 3);
                
                sendInfestedPacketToClients(level, pos, true);
                afterBlockConverted(level, pos, newState);
                return true;
            }
        } else {
            if (!level.getGameRules().get(epca.DO_INFESTED_FALLBACK)) {
                return false;
            }
            
            if (isInfestedBlock(state)) {
                return false;
            }
            
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                return false;
            }
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            boolean isAltarBlock = state.is(AltarBlockTags.PEDESTAL_TAG) || state.is(AltarBlockTags.ALTAR_STONE_TAG);
            if (id.getNamespace().equals("minecraft")
                    || (id.getNamespace().equals("epca") || id.getNamespace().equals("eej")) && !isAltarBlock) {
                return false;
            }
            float hardness = state.getDestroySpeed(level, pos) * hardnessMultiplier;

            if (hardness == 0.0f || !state.isCollisionShapeFullBlock(level, pos)) {
                int layers = 2 + level.getRandom().nextInt(5); 
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

    
    public void convertLilyPadsAboveInfested(ServerLevel level, BlockPos infestedPos) {
        for (int i = 1; i <= 7; i++) {
            BlockPos checkPos = infestedPos.above(i);
            BlockState state = level.getBlockState(checkPos);
            if (state.is(Blocks.LILY_PAD)) {
                convertLilyPadWithBelow(level, checkPos);
            }
        }
    }

    
    private void convertLilyPadWithBelow(ServerLevel level, BlockPos pos) {
        
        String fullBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.LILY_PAD).toString();
        String targetBlockId = generalConfig.conversions.get(fullBlockId);
        Block targetBlock = null;
        if (targetBlockId != null) {
            targetBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(targetBlockId));
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
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.NORTH) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.NORTH)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.NORTH,
                    source.getValue(net.minecraft.world.level.block.WallBlock.NORTH));
        }
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.SOUTH) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.SOUTH)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.SOUTH,
                    source.getValue(net.minecraft.world.level.block.WallBlock.SOUTH));
        }
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.EAST) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.EAST)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.EAST,
                    source.getValue(net.minecraft.world.level.block.WallBlock.EAST));
        }
        if (source.hasProperty(net.minecraft.world.level.block.WallBlock.WEST) &&
                target.hasProperty(net.minecraft.world.level.block.WallBlock.WEST)) {
            target = target.setValue(net.minecraft.world.level.block.WallBlock.WEST,
                    source.getValue(net.minecraft.world.level.block.WallBlock.WEST));
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
            BlockState lowerState = level.getBlockState(lowerPos);
            if (!lowerState.is(Blocks.TALL_GRASS) && !lowerState.is(Blocks.LARGE_FERN)) continue;
            if (lowerState.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER) continue;

            BlockPos upperPos = lowerPos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (!upperState.isAir() && !upperState.canBeReplaced() && !upperState.is(lowerState.getBlock())) {
                continue;
            }

            DeferredHolder<Block, Block> targetBlock = lowerState.is(Blocks.TALL_GRASS) ?
                    ModBlocks.INFESTED_TALL_GRASS : ModBlocks.INFESTED_TALL_FERN;

            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), 2);

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

    private void convertPlantsInRange(ServerLevel level, BlockPos center, int radius) {
        boolean hasDoublePlant = false;
        int verticalRadius = radius;
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
        if (hasDoublePlant) {
            verticalRadius = radius + 1;
        }
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
        if (state.is(Blocks.SHORT_GRASS)) {
            BlockState targetState;
            if (level.getRandom().nextFloat() < 0.35f) {
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
        if (state.is(Blocks.DEAD_BUSH)) {
            level.setBlock(pos, ModBlocks.INFESTED_DEAD_BUSH.get().defaultBlockState(), 3);
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
                            infestedLeavesState = level.getRandom().nextFloat() < 0.95f ?
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

    // ═══════════════════ Cursed world: silent chunk-level conversion ═══════════════════

    /**
     * The conversion targets that are plants and therefore keep their own leaf/face state instead of going
     * through {@link #copyCommonBlockProperties}.
     */
    /**
     * Lazily built: these are EPCA blocks, and their DeferredHolders are still unbound while mod
     * loading is registering blocks. A static initialiser would throw "Trying to access unbound
     * value" and abort the whole registry event, so the set is created on first use instead.
     */
    private static volatile Set<Block> PLANT_CONVERSION_TARGETS;

    private static Set<Block> plantConversionTargets() {
        Set<Block> targets = PLANT_CONVERSION_TARGETS;
        if (targets == null) {
            targets = Set.of(
            ModBlocks.INFESTED_GRASS.get(),
            ModBlocks.INFESTED_SHORT_GRASS.get(),
            ModBlocks.INFESTED_FERN.get(),
            ModBlocks.INFESTED_TALL_GRASS.get(),
            ModBlocks.INFESTED_TALL_FERN.get(),
            ModBlocks.INFESTED_DEAD_BUSH.get(),
            ModBlocks.INFESTED_VINE.get(),
            ModBlocks.INFESTED_LEAVES.get(),
            ModBlocks.INFESTED_FLOWERING_LEAVES.get(),
            ModBlocks.INFESTED_LILY_PAD.get());
            PLANT_CONVERSION_TARGETS = targets;
        }
        return targets;
    }

    /** Block states that {@link #convertPlantAtPosition} or the vine/leaf rules handle specially. */
    private static final Set<Block> SPECIAL_PLANT_BLOCKS = Set.of(
            Blocks.SHORT_GRASS,
            Blocks.FERN,
            Blocks.DEAD_BUSH,
            Blocks.TALL_GRASS,
            Blocks.LARGE_FERN,
            Blocks.VINE,
            Blocks.LILY_PAD,
            Blocks.FLOWERING_AZALEA_LEAVES);

    /** Blocks that are never converted and would only waste a lookup. */
    private static final Set<Block> NEVER_CONVERTIBLE = Set.of(
            Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.WATER, Blocks.LAVA, Blocks.BEDROCK);

    /**
     * Every block that the chunk generation conversion can turn into something else: the entries of the
     * general conversion config plus the vanilla plants/vines/leaves the hardcoded rules cover.
     *
     * <p>Built per converted chunk (not cached in a field) because {@link #generalConfig} is loaded by the
     * constructor and an instance is cheap to build; used as a section-palette predicate so that a section
     * whose palette contains none of these blocks is skipped without reading a single block.
     */
    private Set<Block> buildConvertibleBlockSet() {
        Set<Block> blocks = new HashSet<>();
        for (String id : generalConfig.conversions.keySet()) {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
            if (block != null) blocks.add(block);
        }
        blocks.addAll(SPECIAL_PLANT_BLOCKS);
        blocks.add(Blocks.OAK_LEAVES);
        blocks.add(Blocks.SPRUCE_LEAVES);
        blocks.add(Blocks.BIRCH_LEAVES);
        blocks.add(Blocks.JUNGLE_LEAVES);
        blocks.add(Blocks.ACACIA_LEAVES);
        blocks.add(Blocks.DARK_OAK_LEAVES);
        blocks.add(Blocks.MANGROVE_LEAVES);
        blocks.add(Blocks.AZALEA_LEAVES);
        blocks.removeAll(NEVER_CONVERTIBLE);
        return blocks;
    }

    /**
     * Silent, chunk-level conversion used for freshly generated chunks of the cursed world.
     *
     * <p>Unlike the interactive single-block methods this entry point produces <b>no</b> side effects at all:
     * no particles, no sounds, no network packets, no random rolls and no delayed queueing. It only reads the
     * chunk and replaces blocks, and it applies <b>every</b> conversion unconditionally (the "convert all, not
     * by probability" requirement).
     *
     * <p>Performance shape:
     * <ul>
     *   <li>sections with {@code hasOnlyAir()} are skipped outright;</li>
     *   <li>a section whose palette holds no convertible block is skipped before any block is read;</li>
     *   <li>blocks already carried by an {@link InfestedBlockInterface} are skipped;</li>
     *   <li>blocks are written with update flag {@value #CHUNK_CONVERSION_UPDATE_FLAGS}, matching the other
     *       bulk-generation writer in this class (the chunk is brand new, so no neighbour notification is
     *       needed and no client packet has to be broadcast).</li>
     * </ul>
     *
     * @return the number of block positions that were replaced
     */
    public int convertChunkAtGeneration(ServerLevel level, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        int minSectionY = chunk.getMinSectionY();
        Set<Block> convertible = buildConvertibleBlockSet();
        int converted = 0;

        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section == null || section.hasOnlyAir()) continue;
            if (!section.maybeHas(state -> convertible.contains(state.getBlock()))) continue;

            int sectionBaseY = SectionPos.sectionToBlockCoord(minSectionY + index);
            for (int localY = 0; localY < 16; localY++) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        BlockPos pos = new BlockPos(baseX + localX, sectionBaseY + localY, baseZ + localZ);
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        if (state.isAir() || isInfestedBlock(state)) continue;
                        if (convertSingleBlockSilently(level, pos, state)) converted++;
                    }
                }
            }
        }
        return converted;
    }

    /** The update flag used by {@link #convertChunkAtGeneration}. */
    private static final int CHUNK_CONVERSION_UPDATE_FLAGS = 2;

    /**
     * One block, silently. Plant rules first (they own the grass/fern/vine/leaf shapes), then the general
     * conversion map.
     *
     * @return whether the block was replaced
     */
    private boolean convertSingleBlockSilently(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.SHORT_GRASS)) {
            // convertPlantAtPosition rolls a 35% chance for the short variant; "convert everything" means the
            // full block, so the chunk path is deterministic here.
            level.setBlock(pos, ModBlocks.INFESTED_GRASS.get().defaultBlockState(), CHUNK_CONVERSION_UPDATE_FLAGS);
            return true;
        }
        if (state.is(Blocks.FERN)) {
            level.setBlock(pos, ModBlocks.INFESTED_FERN.get().defaultBlockState(), CHUNK_CONVERSION_UPDATE_FLAGS);
            return true;
        }
        if (state.is(Blocks.DEAD_BUSH)) {
            level.setBlock(pos, ModBlocks.INFESTED_DEAD_BUSH.get().defaultBlockState(), CHUNK_CONVERSION_UPDATE_FLAGS);
            return true;
        }
        if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN)) {
            return convertDoublePlantSilently(level, pos, state);
        }
        if (state.is(Blocks.VINE)) {
            convertVineToInfestedSilently(level, pos, state);
            return true;
        }
        if (isConvertibleLeaves(state)) {
            BlockState infested = state.is(Blocks.FLOWERING_AZALEA_LEAVES)
                    ? ModBlocks.INFESTED_FLOWERING_LEAVES.get().defaultBlockState()
                    : ModBlocks.INFESTED_LEAVES.get().defaultBlockState();
            // A vanilla leaf keeps its distance/persistent properties; the interactive path drops them because
            // it always builds the default state.
            if (state.hasProperty(LeavesBlock.DISTANCE) && infested.hasProperty(LeavesBlock.DISTANCE)) {
                infested = infested.setValue(LeavesBlock.DISTANCE, state.getValue(LeavesBlock.DISTANCE));
            }
            if (state.hasProperty(LeavesBlock.PERSISTENT) && infested.hasProperty(LeavesBlock.PERSISTENT)) {
                infested = infested.setValue(LeavesBlock.PERSISTENT, state.getValue(LeavesBlock.PERSISTENT));
            }
            level.setBlock(pos, infested, CHUNK_CONVERSION_UPDATE_FLAGS);
            return true;
        }
        return convertGeneralBlockSilently(level, pos, state);
    }

    /**
     * Replaces one or both halves of a tall grass / large fern. A block that is not mapped to a double plant
     * (for example {@code minecraft:large_fern} in a config that remaps it elsewhere) falls through to the
     * general map instead of being dropped.
     */
    private boolean convertDoublePlantSilently(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState lowerState = state;
        BlockPos lowerPos = pos;
        if (state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
            lowerPos = pos.below();
            lowerState = level.getBlockState(lowerPos);
        }
        DeferredHolder<Block, Block> target = null;
        if (lowerState.is(Blocks.TALL_GRASS)) {
            target = ModBlocks.INFESTED_TALL_GRASS;
        } else if (lowerState.is(Blocks.LARGE_FERN)) {
            target = ModBlocks.INFESTED_TALL_FERN;
        }
        if (target == null) {
            return convertGeneralBlockSilently(level, pos, state);
        }

        Block targetBlock = target.get();
        level.setBlock(lowerPos, targetBlock.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), CHUNK_CONVERSION_UPDATE_FLAGS);
        BlockPos upperPos = lowerPos.above();
        // Do not overwrite whatever ended up above the lower half (the upper half itself, or a neighbour that
        // has already been converted while scanning this chunk).
        if (level.getBlockState(upperPos).is(lowerState.getBlock())) {
            level.setBlock(upperPos, targetBlock.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), CHUNK_CONVERSION_UPDATE_FLAGS);
        }
        return true;
    }

    /** Silent variant of {@link #convertVineToInfested}: same face-preserving result, no level parameter type change. */
    private void convertVineToInfestedSilently(ServerLevel level, BlockPos pos, BlockState vineState) {
        BlockState infestedVineState = ModBlocks.INFESTED_VINE.get().defaultBlockState();
        for (Direction dir : Direction.values()) {
            BooleanProperty property = VineBlock.getPropertyForFace(dir);
            if (property != null && vineState.hasProperty(property) && vineState.getValue(property)) {
                infestedVineState = infestedVineState.setValue(property, true);
            }
        }
        level.setBlock(pos, infestedVineState, CHUNK_CONVERSION_UPDATE_FLAGS);
    }

    /**
     * The general-config lookup of {@link #convertBlockUsingMap}, without the packets, the fallback gating and
     * the residue rules' extra bookkeeping.
     */
    private boolean convertGeneralBlockSilently(ServerLevel level, BlockPos pos, BlockState state) {
        String targetId = generalConfig.conversions.get(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if (targetId == null) return false;
        Block targetBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(targetId));
        if (targetBlock == null || targetBlock == Blocks.AIR) return false;

        BlockState newState = targetBlock.defaultBlockState();
        if (!plantConversionTargets().contains(targetBlock)) {
            newState = copyCommonBlockProperties(state, newState);
        } else if (newState.hasProperty(InfestedLilyPad.NATURAL_SPAWN)) {
            // Same marker the interactive lily-pad path sets.
            newState = newState.setValue(InfestedLilyPad.NATURAL_SPAWN, true);
        }
        level.setBlock(pos, newState, CHUNK_CONVERSION_UPDATE_FLAGS);
        return true;
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
                ModNetwork.sendToAll(new InfestedSourcePacket.AddInfestedSourcePacket(pos));
            } else {
                ModNetwork.sendToAll(new InfestedSourcePacket.RemoveInfestedSourcePacket(pos));
            }
        }
    }

    
    public Block getTargetLilyPadBlock() {
        String targetId = generalConfig.conversions.get("minecraft:lily_pad");
        if (targetId != null) {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(targetId));
            if (block != null && block != Blocks.AIR) {
                return block;
            }
        }
        return Blocks.LILY_PAD;
    }

    
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



    

    
    private static final class SacrificeTask {
        
        static final int BATCH_SIZE = 700;
        
        private static final int PARTICLE_INTERVAL = 20;
        private static final int HEARTBEAT_INTERVAL = 60;
        
        private static final int PERCENT_STEP = 10;

        final ResourceKey<Level> dimension;
        final BlockPos center;
        final UUID playerId;
        
        final BlockPos[] positions;
        int index;
        int tick;
        int lastReportedPercent = -1;

        SacrificeTask(ResourceKey<Level> dimension, BlockPos center, UUID playerId, BlockPos[] positions, int index) {
            this.dimension = dimension;
            this.center = center;
            this.playerId = playerId;
            this.positions = positions;
            this.index = Math.max(0, Math.min(index, positions.length));
        }

        SacrificeTask(ResourceKey<Level> dimension, BlockPos center, UUID playerId, BlockPos[] positions) {
            this(dimension, center, playerId, positions, 0);
        }

        boolean isComplete() { return index >= positions.length; }

        int percent() {
            return positions.length == 0 ? 100 : (int) ((long) index * 100L / positions.length);
        }
    }

    
    private final Map<ResourceKey<Level>, Queue<SacrificeTask>> sacrificeTasks = new HashMap<>();

    
    private static final int PERSIST_INTERVAL_TICKS = 100;
    private int persistCounter;

    
    @Nullable
    private net.minecraft.server.MinecraftServer recoveredServer;

    
    public boolean addSacrificeTask(ServerLevel level, BlockPos center, UUID playerId, List<BlockPos> positions,
                                    @Nullable Runnable conflictHandler) {
        BlockPos[] snapshot = positions.toArray(new BlockPos[0]);
        synchronized (sacrificeTasks) {
            Queue<SacrificeTask> queue = sacrificeTasks.computeIfAbsent(level.dimension(), k -> new LinkedList<>());
            for (SacrificeTask running : queue) {
                if (running.center.equals(center)) {
                    if (conflictHandler != null) conflictHandler.run();
                    return false;
                }
            }
            queue.add(new SacrificeTask(level.dimension(), center, playerId, snapshot));
        }
        
        persistedTasks(level).setTasksForDimension(level.dimension(), toSavedEntries(level));
        return true;
    }

    
    public boolean addSacrificeTask(ServerLevel level, BlockPos center, UUID playerId, List<BlockPos> positions) {
        return addSacrificeTask(level, center, playerId, positions, null);
    }

    
    public boolean hasSacrificeTask(ServerLevel level, BlockPos center) {
        synchronized (sacrificeTasks) {
            Queue<SacrificeTask> queue = sacrificeTasks.get(level.dimension());
            if (queue == null) return false;
            for (SacrificeTask task : queue) {
                if (task.center.equals(center)) return true;
            }
            return false;
        }
    }

    
    public boolean cancelSacrificeTask(ResourceKey<Level> dimension, BlockPos center) {
        boolean cancelled = false;
        synchronized (sacrificeTasks) {
            Queue<SacrificeTask> queue = sacrificeTasks.get(dimension);
            if (queue != null && queue.removeIf(task -> task.center.equals(center))) {
                cancelled = true;
            }
        }
        ServerLevel level = levelOf(dimension);
        if (level != null) {
            SacrificeSavedData data = persistedTasks(level);
            if (data.removeTasksAt(dimension, center)) cancelled = true;
        }
        return cancelled;
    }

    
    public List<BlockPos> cancelSacrificeTasksForPlayer(ResourceKey<Level> dimension, UUID playerId) {
        List<BlockPos> cancelled = new ArrayList<>();
        synchronized (sacrificeTasks) {
            Queue<SacrificeTask> queue = sacrificeTasks.get(dimension);
            if (queue != null) {
                Iterator<SacrificeTask> it = queue.iterator();
                while (it.hasNext()) {
                    SacrificeTask task = it.next();
                    if (task.playerId.equals(playerId)) {
                        cancelled.add(task.center);
                        it.remove();
                    }
                }
            }
        }
        ServerLevel level = levelOf(dimension);
        if (level != null && persistedTasks(level).removeTasksForPlayer(dimension, playerId) && cancelled.isEmpty()) {
            
            cancelled.add(BlockPos.ZERO);
        }
        return cancelled;
    }

    
    public void clearSacrificeTasks() {
        synchronized (sacrificeTasks) {
            sacrificeTasks.clear();
        }
    }

    

    private static SacrificeSavedData persistedTasks(ServerLevel level) {
        return SacrificeSavedData.get(level);
    }

    
    private List<SacrificeSavedData.SacrificeEntry> toSavedEntries(ServerLevel level) {
        List<SacrificeSavedData.SacrificeEntry> entries = new ArrayList<>();
        synchronized (sacrificeTasks) {
            Queue<SacrificeTask> queue = sacrificeTasks.get(level.dimension());
            if (queue == null) return entries;
            for (SacrificeTask task : queue) {
                long[] packed = new long[task.positions.length];
                boolean encodable = true;
                for (int i = 0; i < task.positions.length; i++) {
                    BlockPos pos = task.positions[i];
                    int dx = pos.getX() - task.center.getX();
                    int dy = pos.getY() - task.center.getY();
                    int dz = pos.getZ() - task.center.getZ();
                    if (!SacrificeSavedData.isEncodableOffset(dx, dy, dz)) {
                        encodable = false;
                        break;
                    }
                    packed[i] = SacrificeSavedData.encodeOffset(dx, dy, dz);
                }
                if (!encodable) {
                    
                    epca.LOGGER.error("Sacrifice task at {} has positions outside the encodable range; not persisting it",
                            task.center);
                    continue;
                }
                entries.add(new SacrificeSavedData.SacrificeEntry(level.dimension(), task.center, task.playerId,
                        packed, task.index));
            }
        }
        return entries;
    }

    
    @SubscribeEvent
    public void handleServerStarted(ServerStartedEvent event) {
        net.minecraft.server.MinecraftServer server = event.getServer();
        recoveredServer = server;
        restoreSacrificeTasks(server);
    }

    
    private void restoreSacrificeTasks(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            SacrificeSavedData data = persistedTasks(level);
            List<SacrificeSavedData.SacrificeEntry> stored = data.getTasks();
            if (stored.isEmpty()) continue;

            List<SacrificeTask> restored = new ArrayList<>();
            for (SacrificeSavedData.SacrificeEntry entry : stored) {
                if (!entry.dimension().equals(level.dimension())) continue;
                BlockPos[] positions = SacrificeSavedData.decodePositions(entry.positions(), entry.center());
                if (entry.nextIndex() >= positions.length) {
                    
                    restored.add(new SacrificeTask(entry.dimension(), entry.center(), entry.playerId(), positions,
                            positions.length));
                    continue;
                }
                restored.add(new SacrificeTask(entry.dimension(), entry.center(), entry.playerId(), positions,
                        entry.nextIndex()));
            }
            if (restored.isEmpty()) {
                data.setTasksForDimension(level.dimension(), List.of());
                continue;
            }

            synchronized (sacrificeTasks) {
                Queue<SacrificeTask> queue = sacrificeTasks.computeIfAbsent(level.dimension(), k -> new LinkedList<>());
                for (SacrificeTask task : restored) {
                    boolean duplicate = queue.stream().anyMatch(running -> running.center.equals(task.center));
                    if (!duplicate) queue.add(task);
                }
            }
            for (SacrificeTask task : restored) {
                epca.LOGGER.info("Restored in-progress sacrifice ritual at {} in {} ({}%)",
                        task.center, level.dimension().identifier(), task.percent());
                
                for (ServerPlayer player : level.players()) {
                    sendRitualMessage(player, Component.translatable("ritual.epca.resumed", task.percent()));
                }
            }
        }
    }

    
    @SubscribeEvent
    public void handlePlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        ResourceKey<Level> dimension = serverPlayer.level().dimension();
        List<BlockPos> cancelled = cancelSacrificeTasksForPlayer(dimension, serverPlayer.getUUID());
        if (!cancelled.isEmpty()) {
            sendRitualMessage(serverPlayer, Component.translatable("ritual.epca.cancelled"));
            epca.LOGGER.info("Cancelled {} in-progress sacrifice ritual(s) because their owner logged out",
                    cancelled.size());
        }
    }

    

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        
        if (!pendingDoublePlantConversions.isEmpty()) {
            Map<ServerLevel, Set<BlockPos>> toProcess;
            synchronized (pendingDoublePlantConversions) {
                toProcess = new HashMap<>(pendingDoublePlantConversions);
                pendingDoublePlantConversions.clear();
            }
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (Map.Entry<ServerLevel, Set<BlockPos>> entry : toProcess.entrySet()) {
                    if (server.getLevel(entry.getKey().dimension()) == null) continue;
                    applyPendingDoublePlantConversions(entry.getKey(), entry.getValue());
                }
            }
        }

        advanceSacrificeTasks();
    }

    
    private void advanceSacrificeTasks() {
        persistCounter++;
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server != recoveredServer) {
            
            
            
            recoveredServer = server;
            restoreSacrificeTasks(server);
        }
        List<SacrificeTask> finished = new ArrayList<>();
        List<ResourceKey<Level>> touched = new ArrayList<>();

        synchronized (sacrificeTasks) {
            Iterator<Map.Entry<ResourceKey<Level>, Queue<SacrificeTask>>> it = sacrificeTasks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ResourceKey<Level>, Queue<SacrificeTask>> entry = it.next();
                Queue<SacrificeTask> queue = entry.getValue();
                if (queue.isEmpty()) {
                    it.remove();
                    continue;
                }
                SacrificeTask task = queue.peek();
                if (task == null) continue;

                ServerLevel level = levelOf(entry.getKey());
                if (level == null || level.isClientSide()) {
                    
                    continue;
                }

                tickAndConvert(level, task);
                touched.add(entry.getKey());

                if (task.isComplete()) {
                    queue.poll();
                    finished.add(task);
                }
            }
        }

        boolean persistNow = persistCounter % PERSIST_INTERVAL_TICKS == 0;
        if (persistNow) {
            for (ResourceKey<Level> dimension : touched) {
                ServerLevel level = levelOf(dimension);
                if (level != null) {
                    persistedTasks(level).setTasksForDimension(dimension, toSavedEntries(level));
                }
            }
        }
        for (SacrificeTask task : finished) {
            ServerLevel level = levelOf(task.dimension);
            if (level != null) completeSacrifice(level, task);
        }
    }

    
    private void tickAndConvert(ServerLevel level, SacrificeTask task) {
        int end = Math.min(task.index + SacrificeTask.BATCH_SIZE, task.positions.length);
        for (int i = task.index; i < end; i++) {
            BlockPos targetPos = task.positions[i];
            BlockState state = level.getBlockState(targetPos);
            if (state.isAir()) continue;
            double dist = Math.sqrt(task.center.distSqr(targetPos));
            if (dist <= 48) {
                convertBlockUsingGeneralConfig(level, targetPos, state);
            } else if (dist <= 64) {
                if (level.getRandom().nextFloat() < 0.65f) {
                    convertBlockUsingGeneralConfig(level, targetPos, state);
                }
            } else if (dist <= 72) {
                if (level.getRandom().nextFloat() < 0.20f) {
                    convertBlockUsingGeneralConfig(level, targetPos, state);
                }
            }
        }
        task.index = end;
        task.tick++;

        
        if (task.tick % SacrificeTask.PARTICLE_INTERVAL == 0) {
            double x = task.center.getX() + 0.5;
            double y = task.center.getY() + 1.0;
            double z = task.center.getZ() + 0.5;
            level.sendParticles(ParticleTypes.SOUL, x, y, z, 3, 0.6, 0.8, 0.6, 0.01);
            level.sendParticles(ModParticles.COTH.get(), x, y, z, 1, 0.5, 0.5, 0.5, 0.0);
        }

        
        if (task.tick % SacrificeTask.HEARTBEAT_INTERVAL == 0) {
            level.playSound(null, task.center, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6F, 0.6F);
            int percent = task.percent();
            if (percent / SacrificeTask.PERCENT_STEP > task.lastReportedPercent / SacrificeTask.PERCENT_STEP) {
                task.lastReportedPercent = percent;
                Player owner = level.getPlayerByUUID(task.playerId);
                if (owner != null) {
                    sendRitualMessage(owner, Component.translatable("ritual.epca.progress", percent));
                }
                
                for (Player nearby : nearbyPlayers(level, task.center, NEARBY_MESSAGE_RADIUS)) {
                    if (nearby != owner) {
                        sendRitualMessage(nearby, Component.translatable("ritual.epca.progress_nearby"));
                    }
                }
            }
        }
    }

    private void completeSacrifice(ServerLevel level, SacrificeTask task) {
        BlockPos center = task.center;

        BlockPos belowPos = center.below();
        level.setBlock(belowPos, ModBlocks.INFESTED_METALLIKE.get().defaultBlockState(), 3);

        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        lightning.setPos(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
        level.addFreshEntity(lightning);

        MobEffectInstance cothEffect = new MobEffectInstance(ModEffects.COTH, 1200, 4);
        AABB effectBox = new AABB(center).inflate(72);
        List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, effectBox);
        for (LivingEntity living : livingEntities) {
            living.addEffect(cothEffect);
        }

        Player player = level.getPlayerByUUID(task.playerId);
        if (player != null) {
            NestLeaderManager.addNestLeader(player.getUUID());
        }

        
        persistedTasks(level).removeTasksAt(level.dimension(), center);
        level.playSound(null, center, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
        if (player != null) {
            sendRitualMessage(player, Component.translatable("ritual.epca.completed"));
        }
        for (Player nearby : nearbyPlayers(level, center, NEARBY_MESSAGE_RADIUS)) {
            if (nearby != player) {
                sendRitualMessage(nearby, Component.translatable("ritual.epca.completed_nearby"));
            }
        }
    }

    
    private static final double NEARBY_MESSAGE_RADIUS = 32.0;

    private static List<Player> nearbyPlayers(ServerLevel level, BlockPos center, double radius) {
        List<Player> result = new ArrayList<>();
        for (Player candidate : level.players()) {
            if (candidate.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5)
                    <= radius * radius) {
                result.add(candidate);
            }
        }
        return result;
    }

    @Nullable
    private static ServerLevel levelOf(ResourceKey<Level> dimension) {
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getLevel(dimension);
    }

    
    private static void sendRitualMessage(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
    }
}