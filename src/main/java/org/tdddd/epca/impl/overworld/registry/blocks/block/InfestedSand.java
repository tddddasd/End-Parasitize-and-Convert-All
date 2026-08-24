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
import net.minecraftforge.common.IPlantable;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

import java.util.HashSet;
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
        if (!state.getValue(NATURAL_SPAWN)) return;
        if (random.nextFloat() < 0.01f) {
            spawnCactus(level, pos, random);
        }

        if (hasWaterAround(level, pos) && level.getBlockState(pos.above()).isAir()) {
            if (random.nextFloat() < 0.0075f) {
                spawnSugarCanes(level, pos, random, 5 + random.nextInt(2), 2, 1, 2); // 5~6个，x/z半径2，y半径1
            }
            if (random.nextFloat() < 0.005f) {
                spawnSugarCanes(level, pos, random, 2 + random.nextInt(3), 1, 1, 1); // 2~4个，x/z半径1，y半径1
            }
        }
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
                // 在 spawnCactus 方法中：
                Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);

                Holder<Biome> holder = biomeRegistry.getHolderOrThrow(biomeRegistry.getResourceKey(biome).get());
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
                    if (aboveBlock instanceof IPlantable) {
                        canPlace = belowState.canSustainPlant(level, below, Direction.UP, (IPlantable) aboveBlock);
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
        // 收集周围48格内已有的虫染甘蔗，用于距离检查
        Set<BlockPos> existingCanes = new HashSet<>();
        BlockPos.betweenClosedStream(center.offset(-48, -48, -48), center.offset(48, 48, 48))
                .forEach(p -> {
                    if (level.getBlockState(p).getBlock() == ModBlocks.INFESTED_SUGAR_CANE.get()) {
                        existingCanes.add(p.immutable());
                    }
                });

        int placed = 0;
        for (int i = 0; i < count * 20; i++) { // 尝试多次，最多 count*20 次
            if (placed >= count) break;

            int dx = random.nextInt(2 * rangeX + 1) - rangeX;
            int dy = random.nextInt(2 * rangeY + 1) - rangeY;
            int dz = random.nextInt(2 * rangeZ + 1) - rangeZ;
            BlockPos targetPos = center.offset(dx, dy, dz);

            // 必须为空气
            if (!level.isEmptyBlock(targetPos)) continue;

            // 下方必须为 InfestedDirt 或 InfestedSand
            BlockPos below = targetPos.below();
            BlockState belowState = level.getBlockState(below);
            if (!(belowState.getBlock() == ModBlocks.INFESTED_DIRT.get() ||
                    belowState.getBlock() == ModBlocks.INFESTED_SAND.get())) {
                continue;
            }

            // 检查 targetPos 下方方块的水平方向是否有水
            if (!hasWaterAround(level, targetPos.below())) continue;

            // 距离已有甘蔗至少 48 格（48^2 = 2304）
            boolean tooClose = false;
            for (BlockPos canePos : existingCanes) {
                if (canePos.distSqr(targetPos) < 48 * 48) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            // 放置甘蔗（默认 age=0, natural_spawn=true, top=true 由方块默认状态决定）
            level.setBlock(targetPos, ModBlocks.INFESTED_SUGAR_CANE.get()
                    .defaultBlockState()
                    .setValue(InfestedSugarCane.AGE, 0)
                    .setValue(InfestedSugarCane.NATURAL_SPAWN, true)
                    .setValue(InfestedSugarCane.TOP, true), 3);
            existingCanes.add(targetPos.immutable());
            placed++;
        }
    }

    /**
     * 检查方块水平四周是否有水源
     */
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
