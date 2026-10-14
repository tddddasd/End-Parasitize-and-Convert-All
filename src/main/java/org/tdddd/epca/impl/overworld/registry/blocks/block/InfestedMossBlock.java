package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.world.level.block.Block;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

/**
 * Infested moss block (虫染苔藓块). A plain full block that only marks itself as an infested block,
 * so the conversion manager and the infestation tracking treat it like every other infested block.
 */
public class InfestedMossBlock extends Block implements InfestedBlockInterface {

    public InfestedMossBlock(Properties properties) {
        super(properties);
    }
}
