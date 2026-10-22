package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;


public class BiomassCountBlockDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final BiomassCountBlockDataProvider INSTANCE = new BiomassCountBlockDataProvider();

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof BeckonCoreBlockEntity core) {
            int count = core.getBiomassCount();
            if (count > 0) {
                tag.putInt(EPCAJadeIds.NBT_BIOMASS_COUNT, count);
            tag.putInt(EPCAJadeIds.NBT_BIOMASS_COUNT_MAX,
                    BeckonCoreBlockEntity.MAX_BIOMASS_COUNT);
            }
        }
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.BIOMASS_COUNT_INFO;
    }
}
