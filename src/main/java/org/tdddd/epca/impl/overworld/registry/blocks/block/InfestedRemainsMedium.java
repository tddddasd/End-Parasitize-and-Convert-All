package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedRemainsMedium extends Block {
    
    private static final VoxelShape INTERACTION_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D); 
    
    private static final VoxelShape COLLISION_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 0.1D, 16.0D);

    /**
     * 26.1.2: {@code DeferredRegister.Blocks#registerBlock} takes a
     * {@code Function<Properties, B>} so the registry id can be attached to the
     * properties. A no-arg-only block is registered through the plain
     * {@code register(...)} overload, which leaves the id unset and aborts the whole
     * block registry at runtime with {@code NullPointerException: Block id not set}.
     */
    public InfestedRemainsMedium(Properties properties) {
        super(properties);
    }

    /** Kept for {@code simpleCodec} / hand construction with the original defaults. */
    public InfestedRemainsMedium() {
        this(Properties.of()
                .noOcclusion() 
                .strength(0.0f) 
                .sound(SoundType.NETHER_WART)
                .isViewBlocking((state, world, pos) -> false) 
                .isSuffocating((state, world, pos) -> false) 
                .instabreak() 
                .pushReaction(PushReaction.DESTROY) 
        );
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return simpleCodec(InfestedRemainsMedium::new);
    }

    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            
            if (!livingEntity.hasEffect(ModEffects.COTH)) {
                
                livingEntity.addEffect(new MobEffectInstance(
                        ModEffects.COTH,
                        900,   
                        1,     
                        false, 
                        true,  
                        true   
                ));
            }
        }
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return INTERACTION_SHAPE;
    }

    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    
    @Override
    public boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        return Block.canSupportCenter(level, belowPos, Direction.UP);
    }

    
    @Override
    public BlockState updateShape(BlockState state, LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks,
                                  BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                  RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }
}