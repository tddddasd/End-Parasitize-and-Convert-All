package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

public class InfestedLog extends RotatedPillarBlock implements InfestedBlockInterface {
    
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    
    public static final BooleanProperty HAS_ADVANCED_DIRT_BELOW = BooleanProperty.create("has_dirt_below");
    
    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedLog(Properties properties) {
        super(properties);
        
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(NATURAL_SPAWN, true) 
                .setValue(HAS_ADVANCED_DIRT_BELOW, false)); 
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, NATURAL_SPAWN, HAS_ADVANCED_DIRT_BELOW);
    }

    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!state.is(oldState.getBlock())) {
            
            boolean hasAdvancedDirtBelow = (state.getValue(AXIS) == Direction.Axis.Y) &&
                    hasAdvancedInfestedDirtBelow(level, pos);

            
            if (state.getValue(HAS_ADVANCED_DIRT_BELOW) != hasAdvancedDirtBelow) {
                level.setBlock(pos, state.setValue(HAS_ADVANCED_DIRT_BELOW, hasAdvancedDirtBelow), 3);
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        
        boolean hasAdvancedDirtBelow = (state.getValue(AXIS) == Direction.Axis.Y) &&
                hasAdvancedInfestedDirtBelow(level, pos);

        
        BlockState newState = state
                .setValue(NATURAL_SPAWN, false) 
                .setValue(HAS_ADVANCED_DIRT_BELOW, hasAdvancedDirtBelow); 

        level.setBlock(pos, newState, 3);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.OAK_LOG.getSoundType(Blocks.OAK_LOG.defaultBlockState(), level, pos, entity);
    }

    
    public static boolean hasAdvancedInfestedDirtBelow(Level level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        Block belowBlock = belowState.getBlock();

        
        return belowBlock == ModBlocks.INFESTED_DIRT.get();
    }

    public static class InfestedLogItem extends BlockItem {
        public InfestedLogItem(Block block, Properties properties) {
            super(block, properties);
        }

        
        @Override
        public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
            return 300;
        }
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
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 20;  
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;   
    }
}