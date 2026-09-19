package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

import java.lang.reflect.Method;

public class InfestedNetherseaBrandGrown extends Block implements InfestedBlockInterface , SimpleWaterloggedBlock {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    
    private static boolean sanityAvailable = false;
    private static Method deductMethod = null;
    
    static {
        try {
            Class<?> clazz = Class.forName("net.mcreator.caerulaarbor.procedures.DeductPlayerSanityProcedure");
            deductMethod = clazz.getMethod("execute", Entity.class, double.class);
            sanityAvailable = true;
        } catch (Exception e) {
        }
    }

    private static final String COOLDOWN_KEY = "InfestedNetherseaBrandCooldown";
    
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    
    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedNetherseaBrandGrown(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NATURAL_SPAWN, true)
                .setValue(WATERLOGGED, false));
    }

    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 4;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        return !belowState.isAir() && belowState.isSolidRender(); 
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL_SPAWN, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(NATURAL_SPAWN, false)
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos currentPos,
                                  Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, currentPos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        if (!level.isClientSide()) {
            
            if (!state.canSurvive(level, pos)) {
                level.destroyBlock(pos, false); 
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);

        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            
            if (IParasite.isParasiteByTagOrInterface(living)) {
                return;
            }

            
            TagKey<EntityType<?>> OCEAN_OFFSPRING_TAG = TagKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath("caerula_arbor", "oceanoffspring")
            );
            if (living.getType().builtInRegistryHolder().is(OCEAN_OFFSPRING_TAG)) {
                return;
            }

            
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 0, false, false, false));

            long currentTick = level.getGameTime();
            long lastTrigger = living.getPersistentData().getLong(COOLDOWN_KEY).orElse(0L);

            if (currentTick - lastTrigger >= 20) {
                living.getPersistentData().putLong(COOLDOWN_KEY, currentTick);

                
                living.hurt(level.damageSources().magic(), 1.0F);

                
                if (sanityAvailable) {
                    try {
                        deductMethod.invoke(null, living, 5.0);
                    } catch (Exception e) {
                    }
                }

                
                living.addEffect(new MobEffectInstance(ModEffects.COTH, 600, 0));
            }
        }
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.SCULK_VEIN.getSoundType(Blocks.SCULK_VEIN.defaultBlockState(), level, pos, entity);
    }

    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        
        if (context instanceof EntityCollisionContext entityCtx) {
            Entity entity = entityCtx.getEntity();
            if (entity instanceof InfestedSilverfish) {
                return Shapes.empty();
            }
        }
        
        return SHAPE;
    }
}