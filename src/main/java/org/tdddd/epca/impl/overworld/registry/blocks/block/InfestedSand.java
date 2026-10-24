package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.world.level.biome.Biome;

public class InfestedSand extends Block implements InfestedBlockInterface {

    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedSand(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NATURAL_SPAWN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL_SPAWN);

    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return simpleCodec(InfestedSand::new);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.SAND.getSoundType(Blocks.SAND.defaultBlockState(), level, pos, entity);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityCtx) {
            Entity entity = entityCtx.getEntity();
            if (entity instanceof InfestedSilverfish) {
                return Shapes.empty();
            }
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(NATURAL_SPAWN);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Random ticks are extremely common once a large area is infested, and each one used to try
        // several plants with dozens of candidate positions. Skipping the body most of the time keeps
        // growth working while cutting the per tick block operations by an order of magnitude.
        if (random.nextFloat() > 0.08F) {
            return;
        }
        if (!state.getValue(NATURAL_SPAWN)) return;
        if (random.nextFloat() < 0.01f) {
            spawnCactus(level, pos, random);
        }

        // Infested dry grass: a sparse 0.5 % roll that only ever fires in desert / badlands type biomes,
        // 5~9 plants per attempt of which 2~4 are the tall variant.
        if (random.nextFloat() < 0.005f) {
            spawnDryGrasses(level, pos, random);
        }

        if (hasWaterAround(level, pos) && level.getBlockState(pos.above()).isAir()) {
            if (random.nextFloat() < 0.0075f) {
                spawnSugarCanes(level, pos, random, 5 + random.nextInt(2), 2, 1, 2); 
            }
            if (random.nextFloat() < 0.005f) {
                spawnSugarCanes(level, pos, random, 2 + random.nextInt(3), 1, 1, 1); 
            }
        }
    }

    /**
     * Sows infested short / tall dry grass on the infested sand around {@code pos}.
     *
     * <p>Only desert and badlands type biomes qualify. The presence scan covers a radius of 12 blocks,
     * plants are only ever placed inside a radius of 8 blocks, and one attempt sows 5~9 plants of which
     * 2~4 are {@link ModBlocks#INFESTED_TALL_DRY_GRASS}.
     */
    private void spawnDryGrasses(ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isDryGrassBiome(level, pos)) return;

        final int checkRadius = 12;
        final int spawnRadius = 8;

        Set<BlockPos> existingGrasses = new HashSet<>();
        BlockPos.betweenClosedStream(
                        pos.offset(-checkRadius, -checkRadius, -checkRadius),
                        pos.offset(checkRadius, checkRadius, checkRadius))
                .forEach(p -> {
                    Block block = level.getBlockState(p).getBlock();
                    if (block == ModBlocks.INFESTED_SHORT_DRY_GRASS.get()
                            || block == ModBlocks.INFESTED_TALL_DRY_GRASS.get()) {
                        existingGrasses.add(p.immutable());
                    }
                });

        int count = 5 + random.nextInt(5); // 5~9
        int tallCount = 2 + random.nextInt(3); // 2~4 of them are the tall variant
        List<Integer> tallIndices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tallIndices.add(i);
        }
        Collections.shuffle(tallIndices, new Random(random.nextLong()));
        Set<Integer> tallSlots = new HashSet<>(tallIndices.subList(0, Math.min(tallCount, count)));

        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 30; attempt++) {
                int dx = random.nextInt(2 * spawnRadius + 1) - spawnRadius;
                int dy = random.nextInt(2 * spawnRadius + 1) - spawnRadius;
                int dz = random.nextInt(2 * spawnRadius + 1) - spawnRadius;
                BlockPos targetPos = pos.offset(dx, dy, dz);

                if (targetPos.equals(pos)) continue;

                if (!level.isEmptyBlock(targetPos)) continue;

                BlockPos below = targetPos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.getBlock() != ModBlocks.INFESTED_SAND.get()) continue;

                boolean tooClose = false;
                for (BlockPos grassPos : existingGrasses) {
                    if (grassPos.distSqr(targetPos) < 16) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                Block target = tallSlots.contains(i)
                        ? ModBlocks.INFESTED_TALL_DRY_GRASS.get()
                        : ModBlocks.INFESTED_SHORT_DRY_GRASS.get();
                level.setBlock(targetPos, target.defaultBlockState(), 3);
                existingGrasses.add(targetPos.immutable());
                break;
            }
        }
    }

    /** True when the biome at {@code pos} is a desert or a badlands type biome. */
    private boolean isDryGrassBiome(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.unwrapKey()
                .map(key -> {
                    String path = key.identifier().getPath();
                    return path.contains("desert") || path.contains("badlands") || path.contains("mesa");
                })
                .orElse(false);
    }

    private void spawnCactus(ServerLevel level, BlockPos pos, RandomSource random) {
        Set<BlockPos> existingBushes = new HashSet<>();
        BlockPos.betweenClosedStream(pos.offset(-32, -32, -32), pos.offset(32, 32, 32))
                .forEach(p -> {
                    if (level.getBlockState(p).getBlock() == ModBlocks.INFESTED_CACTUS.get()) {
                        existingBushes.add(p.immutable());
                    }
                });

        int count = 1 + random.nextInt(3); // 1~2
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 20; attempt++) {
                int dx = random.nextInt(25) - 12;
                int dy = random.nextInt(11) - 5;
                int dz = random.nextInt(25) - 12;
                BlockPos targetPos = pos.offset(dx, dy, dz);
                if (!level.isEmptyBlock(targetPos)) continue;

                Biome biome = level.getBiome(targetPos).value();
                
                Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);

                Holder<Biome> holder = biomeRegistry.getOrThrow(biomeRegistry.getResourceKey(biome).get());
                if (!(holder.is(BiomeTags.IS_BADLANDS))) {
                    continue;
                }

                BlockPos below = targetPos.below();
                BlockState belowState = level.getBlockState(below);

                boolean canPlace = false;
                if (belowState.getBlock() == ModBlocks.INFESTED_SAND.get()) {
                    canPlace = true;
                } else {
                    Block aboveBlock = ModBlocks.INFESTED_CACTUS.get();
// 26.1.2: Forge's IPlantable is gone. NeoForge exposes
                    // IBlockStateExtension#canSustainPlant(BlockGetter, BlockPos, Direction, BlockState),
                    // which returns a TriState (DEFAULT = "let the plant decide").
                    var soilDecision = belowState.canSustainPlant(level, below, Direction.UP,
                            aboveBlock.defaultBlockState());
                    if (!soilDecision.isDefault()) {
                        canPlace = soilDecision.isTrue();
                    }
                }
                if (!canPlace) continue;

                boolean tooClose = false;
                for (BlockPos bushPos : existingBushes) {
                    if (bushPos.distSqr(targetPos) < 32 * 32) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                level.setBlock(targetPos, ModBlocks.INFESTED_CACTUS.get()
                        .defaultBlockState().setValue(InfestedSweetBerryBush.AGE, 0), 3);
                existingBushes.add(targetPos.immutable());
                break;
            }
        }
    }

    private void spawnSugarCanes(ServerLevel level, BlockPos center, RandomSource random, int count, int rangeX, int rangeY, int rangeZ) {
        
        Set<BlockPos> existingCanes = new HashSet<>();
        BlockPos.betweenClosedStream(center.offset(-48, -48, -48), center.offset(48, 48, 48))
                .forEach(p -> {
                    if (level.getBlockState(p).getBlock() == ModBlocks.INFESTED_SUGAR_CANE.get()) {
                        existingCanes.add(p.immutable());
                    }
                });

        int placed = 0;
        for (int i = 0; i < count * 20; i++) { 
            if (placed >= count) break;

            int dx = random.nextInt(2 * rangeX + 1) - rangeX;
            int dy = random.nextInt(2 * rangeY + 1) - rangeY;
            int dz = random.nextInt(2 * rangeZ + 1) - rangeZ;
            BlockPos targetPos = center.offset(dx, dy, dz);

            
            if (!level.isEmptyBlock(targetPos)) continue;

            
            BlockPos below = targetPos.below();
            BlockState belowState = level.getBlockState(below);
            if (!(belowState.getBlock() == ModBlocks.INFESTED_DIRT.get() ||
                    belowState.getBlock() == ModBlocks.INFESTED_SAND.get())) {
                continue;
            }

            
            if (!hasWaterAround(level, targetPos.below())) continue;

            
            boolean tooClose = false;
            for (BlockPos canePos : existingCanes) {
                if (canePos.distSqr(targetPos) < 48 * 48) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            
            level.setBlock(targetPos, ModBlocks.INFESTED_SUGAR_CANE.get()
                    .defaultBlockState()
                    .setValue(InfestedSugarCane.AGE, 0)
                    .setValue(InfestedSugarCane.NATURAL_SPAWN, true)
                    .setValue(InfestedSugarCane.TOP, true), 3);
            existingCanes.add(targetPos.immutable());
            placed++;
        }
    }

    
    private boolean hasWaterAround(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState sideState = level.getBlockState(pos.relative(dir));
            if (sideState.is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }
}
