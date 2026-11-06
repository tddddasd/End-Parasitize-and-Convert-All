package org.tdddd.epca.impl.compat;

import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.eej.api.AltarBlockTags;
import org.tdddd.eej.api.AltarInteractionRegistry;
import org.tdddd.epca.impl.overworld.registry.blocks.EpcaAltarInteractionHandler;

public final class EejAltarCompat {

    private EejAltarCompat() {
    }

    public static void registerInteraction() {
        AltarInteractionRegistry.register(new EpcaAltarInteractionHandler());
    }

    public static boolean isAltarBlock(BlockState state) {
        return state.is(AltarBlockTags.PEDESTAL_TAG) || state.is(AltarBlockTags.ALTAR_STONE_TAG);
    }
}
