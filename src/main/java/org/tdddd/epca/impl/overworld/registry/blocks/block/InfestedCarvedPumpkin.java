package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;
import net.minecraft.server.level.ServerLevel;

public class InfestedCarvedPumpkin extends HorizontalDirectionalBlock implements InfestedBlockInterface {

    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedCarvedPumpkin(Properties properties) {
        super(properties.pushReaction(PushReaction.DESTROY));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NATURAL_SPAWN, true)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(NATURAL_SPAWN);

    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(InfestedCarvedPumpkin::new);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.CARVED_PUMPKIN.getSoundType(Blocks.CARVED_PUMPKIN.defaultBlockState(), level, pos, entity);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Normal placement is enough (no sneak requirement); placing only puts down the
        // plain block, it never spawns the physics pumpkin.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * 26.1.2: {@code onRemove(state, level, pos, newState, moved)} was replaced by
     * {@code affectNeighborsAfterRemoval(BlockState,ServerLevel,BlockPos,boolean)}, which drops the
     * incoming state; "was this really replaced?" is read back from the level.
     *
     * <p>Kit 4: the {@code ModEntities.INFESTED_PUMPKIN_HEAD} spawn call that kit 2 had put here is
     * gone together with that entity and its renderer.  This matches the original 1.20.1 behaviour
     * (that version had no pumpkin-head mob at all), so destroying the carved pumpkin now only
     * performs the neighbour updates below and then falls through to the vanilla block-break path:
     * the block is removed, the loot table {@code data/epca/loot_table/blocks/infested_carved_pumpkin.json}
     * decides the drop (the block itself), and the CARVED_PUMPKIN break sound/particles play.
     * No entity is created and no extra drop is added or removed.</p>
     */
    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (!level.getBlockState(pos).is(state.getBlock())) {
            level.updateNeighborsAt(pos, Blocks.AIR);
            level.updateNeighbourForOutputSignal(pos, Blocks.AIR);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;
    }
}
