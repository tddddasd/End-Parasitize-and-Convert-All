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
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.overworld.registry.ModDamageTypes;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.AcidWaterColorPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AcidSolutionBlock extends LiquidBlock {
    public AcidSolutionBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            
            List<BlockPos> affectedWaters = findWaterInRange(level, pos, 8);

            
            for (BlockPos waterPos : affectedWaters) {
                int distance = Math.abs(pos.getX() - waterPos.getX()) +
                        Math.abs(pos.getY() - waterPos.getY()) +
                        Math.abs(pos.getZ() - waterPos.getZ());

                AcidWaterColorPacket packet = new AcidWaterColorPacket(
                        waterPos, pos, distance, true
                );

                ModNetwork.INSTANCE.send(
                        PacketDistributor.ALL.noArg(),
                        packet
                );
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            
            List<BlockPos> affectedWaters = findWaterInRange(level, pos, 8);

            
            for (BlockPos waterPos : affectedWaters) {
                AcidWaterColorPacket packet = new AcidWaterColorPacket(
                        waterPos, pos, 0, false
                );

                ModNetwork.INSTANCE.send(
                        PacketDistributor.ALL.noArg(),
                        packet
                );
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

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        
        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
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
                    ModEffects.COTH.get(),
                    1200, 
                    1,    
                    false,
                    true
            ));

            
            entity.addEffect(new MobEffectInstance(
                    ModEffects.CORROSIVE.get(),
                    300, 
                    0,   
                    false,
                    true
            ));

            
            entity.hurt(entity.damageSources().magic(), 1.0F);

            Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
            DamageSource minimumSource = new DamageSource(holder);
            entity.hurt(minimumSource, 0.25F);
        }
    }

    public FluidType getFluidType() {
        return ModFluids.ACID_SOLUTION_FLUID_TYPE.get();
    }
}