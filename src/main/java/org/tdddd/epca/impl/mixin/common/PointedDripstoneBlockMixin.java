package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedPointedDripstone;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {

    @Inject(method = "isPointedDripstoneWithDirection", at = @At("HEAD"), cancellable = true)
    private static void epca$acceptInfestedDripstone(
            BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof InfestedPointedDripstone
                && state.getValue(PointedDripstoneBlock.TIP_DIRECTION) == direction) {
            cir.setReturnValue(true);
        }
    }
}
