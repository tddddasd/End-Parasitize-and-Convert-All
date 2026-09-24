package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
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

public class InfestedCoalOre extends Block implements InfestedBlockInterface {
    
    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    /** Blindness I duration applied when the ore is ignited by soul fire instead of normal fire. */
    public static final int SOUL_FIRE_BLINDNESS_TICKS = 100;

    public InfestedCoalOre(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NATURAL_SPAWN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL_SPAWN);

    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return simpleCodec(InfestedCoalOre::new);
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);

        if (!level.isClientSide()) {
            
            BlockState neighborState = level.getBlockState(neighborPosFrom(pos, orientation));
            if (neighborState.is(Blocks.FIRE) || neighborState.is(Blocks.SOUL_FIRE)) {
                triggerExplosion(level, pos, neighborState.is(Blocks.SOUL_FIRE));
            }
        }
    }

    /** 26.1.2: {@code neighborChanged} carries an {@code Orientation}, not a neighbour BlockPos. */
    private static BlockPos neighborPosFrom(BlockPos pos, net.minecraft.world.level.redstone.Orientation orientation) {
        if (orientation == null) {
            return pos;
        }
        Direction direction = orientation.getFront();
        return direction == null ? pos : pos.relative(direction);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.STONE.getSoundType(Blocks.STONE.defaultBlockState(), level, pos, entity);
    }

    private void triggerExplosion(Level level, BlockPos pos, boolean soulFire) {
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
                // Soul fire additionally blinds every living entity inside the explosion radius (5 s of Blindness I).
                if (soulFire) {
                    entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, SOUL_FIRE_BLINDNESS_TICKS, 0), null);
                }
                
                float damage = (float) (14.0 * (1.0 - distance / explosionRadius));
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