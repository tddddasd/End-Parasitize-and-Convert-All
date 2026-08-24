package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

import java.lang.reflect.Method;

public class InfestedNetherseaBrandSolid extends Block implements InfestedBlockInterface {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);
    
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
    
    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedNetherseaBrandSolid(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NATURAL_SPAWN, true));
    }

    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 4;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL_SPAWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(NATURAL_SPAWN, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        if (!level.isClientSide && entity instanceof LivingEntity living) {
            
            if (IParasite.isParasiteByTagOrInterface(living)) {
                return;
            }

            
            TagKey<EntityType<?>> OCEAN_OFFSPRING_TAG = TagKey.create(
                    Registries.ENTITY_TYPE,
                    new ResourceLocation("caerula_arbor", "oceanoffspring")
            );
            if (living.getType().is(OCEAN_OFFSPRING_TAG)) {
                return;
            }

            
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1, false, false, false));

            long currentTick = level.getGameTime();
            long lastTrigger = living.getPersistentData().getLong(COOLDOWN_KEY);

            if (currentTick - lastTrigger >= 20) {
                living.getPersistentData().putLong(COOLDOWN_KEY, currentTick);

                
                living.hurt(level.damageSources().magic(), 1.0F);

                
                if (sanityAvailable) {
                    try {
                        deductMethod.invoke(null, living, 5.0);
                    } catch (Exception e) {
                    }
                }

                
                living.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, 0,false, false, true));
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