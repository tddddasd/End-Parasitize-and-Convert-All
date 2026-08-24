package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

public class InfestedHeavyCoalOre extends Block implements InfestedBlockInterface {
    
    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedHeavyCoalOre(Properties properties) {
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        if (!level.isClientSide) {
            
            BlockState neighborState = level.getBlockState(fromPos);
            if (neighborState.is(Blocks.FIRE) || neighborState.is(Blocks.SOUL_FIRE)) {
                triggerExplosion(level, pos);
            }
        }
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.DEEPSLATE_COAL_ORE.getSoundType(Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(), level, pos, entity);
    }

    private void triggerExplosion(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        float power = 4.0f; 

        
        
        level.explode(null, x, y, z, power, Level.ExplosionInteraction.TNT);

        
        double explosionRadius = power * 2.0; 
        AABB area = new AABB(pos).inflate(explosionRadius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!entity.isAlive()) continue;
            double distance = Math.sqrt(entity.distanceToSqr(x, y, z));
            if (distance <= explosionRadius) {
                
                float damage = (float) (28.0 * (1.0 - distance / explosionRadius));
                if (damage > 0) {
                    entity.hurt(level.damageSources().explosion(null, null), damage);
                    
                    applyKnockback(entity, x, y, z, distance, explosionRadius);
                }
            }
        }
    }

    
    private void applyKnockback(LivingEntity entity, double x, double y, double z, double distance, double maxRadius) {
        double dx = entity.getX() - x;
        double dz = entity.getZ() - z;
        double magnitude = Math.sqrt(dx * dx + dz * dz);
        if (magnitude > 0) {
            dx /= magnitude;
            dz /= magnitude;
            double knockbackStrength = (1.0 - distance / maxRadius) * 0.5; 
            entity.setDeltaMovement(
                    entity.getDeltaMovement().add(dx * knockbackStrength, 0.3, dz * knockbackStrength)
            );
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
}