package org.tdddd.epca.impl.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

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
                .viscosity(6000));
    }
    
    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            
            private static final ResourceLocation STILL_TEXTURE =
                    new ResourceLocation("epca", "block/acid");
            private static final ResourceLocation FLOWING_TEXTURE =
                    new ResourceLocation("epca", "block/acid_move");

            private static final int TINT_COLOR = 0xFFFFFFFF;

            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return TINT_COLOR;
            }
        });
    }
}
