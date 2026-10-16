package org.tdddd.epca.impl.compat;

import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.eej.api.AltarBlockTags;
import org.tdddd.eej.api.AltarInteractionRegistry;
import org.tdddd.epca.impl.overworld.registry.blocks.EpcaAltarInteractionHandler;

/**
 * Every eej reference of epca lives here.
 *
 * <p>Nothing may call into this class unless {@link EejCompat#loaded()} is
 * true, which is what keeps epca loadable without eej installed.</p>
 */
public final class EejAltarCompat {

    private EejAltarCompat() {
    }

    /** Registers epca's altar interaction handler with eej. */
    public static void registerInteraction() {
        AltarInteractionRegistry.register(new EpcaAltarInteractionHandler());
    }

    /** True when the block is one of eej's altar blocks (they convert faster). */
    public static boolean isAltarBlock(BlockState state) {
        return state.is(AltarBlockTags.PEDESTAL_TAG) || state.is(AltarBlockTags.ALTAR_STONE_TAG);
    }
}
