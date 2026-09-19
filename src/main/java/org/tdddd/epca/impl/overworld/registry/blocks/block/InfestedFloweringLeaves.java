package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import net.minecraft.util.RandomSource;

public class InfestedFloweringLeaves extends LeavesBlock implements InfestedBlockInterface {
    
    private final BlockConversionManager conversionManager = BlockConversionManager.getInstance();

    public InfestedFloweringLeaves(Properties properties) {
        super(0.01F, properties);
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends LeavesBlock> codec() {
        return simpleCodec(
InfestedFloweringLeaves
::new);
    }

    /**
     * 26.1.2: {@code LeavesBlock} is abstract again (it needs the falling-leaf particle) and its
     * constructor takes the particle chance first. The original block used the vanilla
     * spruce-leaf look; the pink cherry-leaf particle restores the 铏煋 tint.
     */
    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        net.minecraft.util.ParticleUtils.spawnParticleBelow(level, pos, random,
                net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        
        return Shapes.block();
    }

    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        
        if (!level.isClientSide()) {
            checkAndConvertVinesOnSides(level, pos);
        }
    }

    
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);

        if (!level.isClientSide()) {
            
            checkAndConvertVinesOnSides(level, pos);
        }
    }

    
    protected void checkAndConvertVinesOnSides(Level level, BlockPos pos) {
        
        for (Direction direction : Direction.values()) {
            BlockPos vinePos = pos.relative(direction);
            BlockState vineState = level.getBlockState(vinePos);

            
            if (vineState.is(Blocks.VINE) && hasFace(vineState, direction.getOpposite())) {
                conversionManager.convertVineToInfested(level, vinePos, vineState);
            }
        }
    }

    
    public static boolean hasFace(BlockState state, Direction direction) {
        BooleanProperty property = getFaceProperty(direction);
        return state.hasProperty(property) && state.getValue(property);
    }

    
    public static BooleanProperty getFaceProperty(Direction direction) {
        return VineBlock.getPropertyForFace(direction);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            applyCothEffects(livingEntity, true);
        }
    }

    
    private void applyCothEffects(LivingEntity entity, boolean apply) {
        
        if (IParasite.isParasiteByTagOrInterface(entity)) {
            
            return;
        }

        if (apply) {
            
            
            MobEffectInstance cothEffect = new MobEffectInstance(
                    ModEffects.COTH, 
                    600, 
                    0, 
                    false, 
                    true, 
                    true 
            );

            MobEffectInstance slownessEffect = new MobEffectInstance(
                    MobEffects.SLOWNESS, 
                    4, 
                    0, 
                    false, 
                    false, 
                    false 
            );

            
            if (!entity.hasEffect(MobEffects.SLOWNESS) ||
                    entity.getEffect(MobEffects.SLOWNESS).getAmplifier() == 0) {
                entity.addEffect(cothEffect);
                entity.addEffect(slownessEffect);
            }
        }
        
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false; 
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 60;  
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 30;   
    }
}