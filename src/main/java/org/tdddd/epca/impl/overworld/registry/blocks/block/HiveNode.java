package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
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

/**
 * A hive node: stores up to 64 biomass (killCount) and shows one of six textures for the stored amount.
 *
 * <p>Texture bands: 0 -> 0, 1 -> 1..15, 2 -> 16..31, 3 -> 32..47, 4 -> 48..63, 5 -> 64. The top and
 * bottom faces use the matching {@code hive_node_top_N} texture.</p>
 */
public class HiveNode extends Block implements EntityBlock {

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 5);

    public HiveNode(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    /** Maps a stored biomass value onto its texture band. */
    public static int stageFor(int killCount) {
        if (killCount <= 0) {
            return 0;
        }
        if (killCount <= 15) {
            return 1;
        }
        if (killCount <= 31) {
            return 2;
        }
        if (killCount <= 47) {
            return 3;
        }
        if (killCount <= 63) {
            return 4;
        }
        return 5;
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
}
