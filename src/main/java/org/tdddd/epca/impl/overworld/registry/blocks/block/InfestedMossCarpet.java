package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.world.level.block.CarpetBlock;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

/**
 * Infested moss carpet (虫染苔藓地毯). Mirrors the vanilla moss carpet: a thin carpet block with the
 * vanilla carpet shape and no collision, so it keeps {@link CarpetBlock}'s placement rules.
 */
public class InfestedMossCarpet extends CarpetBlock implements InfestedBlockInterface {

    public InfestedMossCarpet(Properties properties) {
        super(properties);
    }
}
