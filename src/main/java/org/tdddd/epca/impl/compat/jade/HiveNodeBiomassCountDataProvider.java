package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Server side of the hive node biomass display. */
public class HiveNodeBiomassCountDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final HiveNodeBiomassCountDataProvider INSTANCE = new HiveNodeBiomassCountDataProvider();

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof
                org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity node) {
            tag.putInt(EPCAJadeIds.NBT_BIOMASS_COUNT, node.getBiomassCount());
            tag.putInt("EPCA_BiomassCountMax",
                    org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity.MAX_BIOMASS_COUNT);
        }
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.BIOMASS_COUNT_INFO;
    }
}
