package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

public class InfestedPumpkin extends Block implements InfestedBlockInterface {

    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedPumpkin(BlockBehaviour.Properties properties) {
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
        return Blocks.PUMPKIN.getSoundType(Blocks.PUMPKIN.defaultBlockState(), level, pos, entity);
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

    /**
     * 26.1.2: {@code onRemove(state, level, pos, newState, moved)} was replaced by
     * {@code affectNeighborsAfterRemoval(BlockState,ServerLevel,BlockPos,boolean)}. The old
     * {@code playerDestroy} override is gone with that method, so this is now the removal hook.
     *
     * <p>Kit 4: kit 2 had used this hook to roll a 1/2 chance of spawning the
     * {@code ModEntities.INFESTED_PUMPKIN_HEAD} mob, gated on a silk-touch marker that
     * {@code playerWillDestroy} had stashed in a static set.  Both the roll and the marker are
     * removed together with that entity and its renderer, so destroying an infested pumpkin now
     * only runs the vanilla removal path below: the block disappears, the loot table
     * {@code data/epca/loot_table/blocks/infested_pumpkin.json} decides the drop (the block
     * itself, with any tool), and the PUMPKIN break sound/particles play.  No entity is spawned,
     * and nothing else those blocks did is affected: placement still clears {@code natural_spawn},
     * the collision shape still lets {@code InfestedSilverfish} pass, and
     * flammability / fire spread are unchanged.</p>
     *
     * <p>The statement above about the old {@code onRemove} /
     * {@code affectNeighborsAfterRemoval} migration is history; the method body is now
     * deliberately trivial.</p>
     */
    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
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
