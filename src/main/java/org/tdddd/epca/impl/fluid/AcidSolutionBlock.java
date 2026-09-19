package org.tdddd.epca.impl.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.AcidWaterColorPacket;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AcidSolutionBlock extends LiquidBlock {
    /**
     * 26.1.2：{@code LiquidBlock} 的构造器由 {@code (Supplier<? extends FlowingFluid>, Properties)}
     * 变为 {@code (FlowingFluid, Properties)}，且 {@code simpleCodec(LiquidBlock::new)} 也要求
     * 直接传流体实例。这里保留 {@code Supplier} 参数的类形状，在构造器里立即取值。
     */
    public AcidSolutionBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid.get(), properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            
            List<BlockPos> affectedWaters = findWaterInRange(level, pos, 8);

            
            for (BlockPos waterPos : affectedWaters) {
                int distance = Math.abs(pos.getX() - waterPos.getX()) +
                        Math.abs(pos.getY() - waterPos.getY()) +
                        Math.abs(pos.getZ() - waterPos.getZ());

                AcidWaterColorPacket packet = new AcidWaterColorPacket(
                        waterPos, pos, distance, true
                );

                ModNetwork.sendToAll(packet);
            }
        }
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);

        if (!level.isClientSide()) {
            
            List<BlockPos> affectedWaters = findWaterInRange(level, pos, 8);

            
            for (BlockPos waterPos : affectedWaters) {
                AcidWaterColorPacket packet = new AcidWaterColorPacket(
                        waterPos, pos, 0, false
                );

                ModNetwork.sendToAll(packet);
            }
        }
    }

    
    private List<BlockPos> findWaterInRange(Level level, BlockPos center, int range) {
        List<BlockPos> result = new ArrayList<>();

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > range) continue;

                    BlockPos checkPos = center.offset(dx, dy, dz);
                    FluidState fluidState = level.getFluidState(checkPos);

                    if (fluidState.getType() == Fluids.WATER ||
                            fluidState.getType() == Fluids.FLOWING_WATER) {
                        result.add(checkPos);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 26.1.2：{@code Block#entityInside(state, level, pos, entity)} 增加了
     * {@code InsideBlockEffectApplier} 与 {@code boolean isPrecise} 两个参数。
     * 本模组不使用 effect applier，只是把原逻辑（非寄生生物 + 非创造/旁观者时施加酸液效果）原样保留。
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);

        
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            if (!IParasite.isParasiteByTagOrInterface(livingEntity)
                    && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)) {
                applyAcidSolutionEffects(livingEntity);
            }
        }
    }

    private void applyAcidSolutionEffects(LivingEntity entity) {
        Level level = entity.level();

        if (level.getGameTime() % 5 == 0) {
            
            entity.addEffect(new MobEffectInstance(
                    ModEffects.COTH,
                    1200, 
                    1,    
                    false,
                    true
            ));

            
            entity.addEffect(new MobEffectInstance(
                    ModEffects.CORROSIVE,
                    300, 
                    0,   
                    false,
                    true
            ));

            
            entity.hurt(entity.damageSources().magic(), 1.0F);

            Registry<DamageType> registry = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
            Holder<DamageType> holder = registry.getOrThrow(ModDamageTypes.MINIMUM);
            DamageSource minimumSource = new DamageSource(holder);
            entity.hurt(minimumSource, 0.25F);
        }
    }

    public FluidType getFluidType() {
        return ModFluids.ACID_SOLUTION_FLUID_TYPE.get();
    }
}