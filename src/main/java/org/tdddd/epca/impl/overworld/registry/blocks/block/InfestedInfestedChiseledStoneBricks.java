package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

public class InfestedInfestedChiseledStoneBricks extends Block implements InfestedBlockInterface {
    
    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedInfestedChiseledStoneBricks(Properties properties) {
        
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NATURAL_SPAWN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL_SPAWN);

    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return simpleCodec(InfestedInfestedChiseledStoneBricks::new);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    @Override
    /**
     * 26.1.2: {@code onRemove(BlockState,Level,BlockPos,BlockState,boolean)} became
     * {@code affectNeighborsAfterRemoval(BlockState,ServerLevel,BlockPos,boolean)}, which no longer
     * receives the incoming state.
     */
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (movedByPiston) {
            super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
            return;
        }

        if (!level.isClientSide()) {
            
            if (!movedByPiston) {
                
                Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5.0, false);
                boolean shouldSpawn = true;

                if (nearestPlayer != null) {
                    
                    if (nearestPlayer.isCreative()) {
                        shouldSpawn = false;
                    } else {
                        
                        ItemStack mainHand = nearestPlayer.getMainHandItem();
if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                        .getOrThrow(Enchantments.SILK_TOUCH), mainHand) > 0) {
                            shouldSpawn = false;
                        }
                    }
                }

                if (shouldSpawn) {
                    
                    EntityType<?> infestedSilverfishEntityType = ModEntities.INFESTED_SILVERFISH.get();
Entity infestedSilverfish = infestedSilverfishEntityType.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                    if (infestedSilverfish != null) {
                        infestedSilverfish.setPos(
                                pos.getX() + 0.5,
                                pos.getY(),
                                pos.getZ() + 0.5
                        );
                        level.addFreshEntity(infestedSilverfish);
                    }

                    
                    ((ServerLevel) level).sendParticles(ParticleTypes.POOF,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            10, 0.2, 0.2, 0.2, 0.0);
                }
            }
        }
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.STONE.getSoundType(Blocks.STONE.defaultBlockState(), level, pos, entity);
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