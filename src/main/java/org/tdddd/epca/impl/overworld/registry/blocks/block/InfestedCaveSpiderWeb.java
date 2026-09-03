package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfestedCaveSpiderWeb extends WebBlock implements InfestedBlockInterface {
    public static final BooleanProperty SPIDER = BooleanProperty.create("spider");
    private static final Map<BlockPos, Long> PLACE_TIME = new HashMap<>();

    public InfestedCaveSpiderWeb(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(SPIDER, false));
    }

    public void entityInside(BlockState blockState, Level level, BlockPos pos, Entity entity) {
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPIDER);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            PLACE_TIME.put(pos.immutable(), level.getGameTime());
            level.scheduleTick(pos, this, 1);
            if (state.getValue(SPIDER)) {
                level.scheduleTick(pos, this, 20 * 60);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        processEntitiesInBlock(state, level, pos);

        Long placeTime = PLACE_TIME.get(pos);
        if (placeTime != null && state.getValue(SPIDER)) {
            long elapsed = level.getGameTime() - placeTime;
            if (elapsed >= 20 * 60) {
                level.destroyBlock(pos, false);
                PLACE_TIME.remove(pos);
                return;
            }
        }

        if (level.getBlockState(pos).getBlock() == this) {
            level.scheduleTick(pos, this, 1);
        }
    }

    private void processEntitiesInBlock(BlockState state, ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(0.1);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> !(e instanceof IParasite));

        for (LivingEntity living : entities) {
            applyCothAndDamage(living, level);
        }
    }

    private void applyCothAndDamage(LivingEntity living, ServerLevel level) {
        var data = living.getPersistentData();
        long now = level.getGameTime();

        long lastEffect = data.getLong("InfestedWebLastEffect");
        if (now - lastEffect >= 20) {
            living.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 20 * 20, 0));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15 * 20, 0));
            data.putLong("InfestedWebLastEffect", now);
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 60;
    }
}