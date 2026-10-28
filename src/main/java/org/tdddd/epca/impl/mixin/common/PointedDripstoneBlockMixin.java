package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedPointedDripstone;

/**
 * Makes the vanilla pointed-dripstone helpers accept the infested variant.
 *
 * <p>Vanilla tests {@code state.is(Blocks.POINTED_DRIPSTONE)}, which is false for the separate
 * infested block even though it extends {@link PointedDripstoneBlock}. Injecting at the head of the
 * private helper is enough; normal pointed dripstone still runs the vanilla body.</p>
 */
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
