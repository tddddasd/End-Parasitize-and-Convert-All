package org.tdddd.epca.impl.fluid;

import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;


public class AcidSolutionType extends FluidType {
    public AcidSolutionType() {
        super(Properties.create()
                .descriptionId("block.epca.acid_solution")
                .fallDistanceModifier(0F)
                .canExtinguish(true)
                .canConvertToSource(false)
                .supportsBoating(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .motionScale(0.007D)
                .canHydrate(false)
                .lightLevel(0)
                .density(3000)
                .temperature(1300)
                .viscosity(6000)
                
                .isWaterLike(true));
    }
}
