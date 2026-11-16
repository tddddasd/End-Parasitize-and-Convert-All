package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity;

public class HiveNode extends Block
        implements EntityBlock, org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface {

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 5);

    /**
     * Chance that a single random tick of this block adds biomass.
     */
    public static final double RANDOM_TICK_BIOMASS_CHANCE = 0.5D;

    /**
     * Biomass added when a random tick succeeds.
     * <p>
     * Vanilla default {@code randomTickSpeed=3} gives 3 / 4096 random-tick chance per block per tick,
     * i.e. one random tick every ~1365 game ticks. With chance 0.5 and amount 3 this averages
     * 1.5 biomass per random tick -> 1 biomass per ~910 game ticks, matching the previous
     * fixed timer (1 biomass per 45s = 900 ticks).
     */
    public static final int RANDOM_TICK_BIOMASS_AMOUNT = 3;

    public HiveNode(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    public static int stageFor(int biomassCount) {
        if (biomassCount <= 0) {
            return 0;
        }
        if (biomassCount <= 15) {
            return 1;
        }
        if (biomassCount <= 31) {
            return 2;
        }
        if (biomassCount <= 47) {
            return 3;
        }
        if (biomassCount <= 63) {
            return 4;
        }
        return 5;
    }

    /**
     * Biomass production is driven by random ticks (gamerule {@code randomTickSpeed} applies) instead of
     * the block-entity timer. The {@code STAGE} blockstate is updated by the block entity through
     * {@code level.setBlock(..., 3)} which also syncs it to tracking clients.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextDouble() >= RANDOM_TICK_BIOMASS_CHANCE) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof HiveNodeBlockEntity node)) {
            return;
        }
        if (!node.hasRoom()) {
            return;
        }
        node.addBiomassCount(RANDOM_TICK_BIOMASS_AMOUNT);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.HIVE_NODE.get()
                ? (lvl, pos, st, be) -> HiveNodeBlockEntity.tick(lvl, pos, st, (HiveNodeBlockEntity) be)
                : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HiveNodeBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(
            BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityCtx
                && entityCtx.getEntity()
                instanceof org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos,
                             net.minecraft.world.entity.Entity entity,
                             net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);

    }
}
