package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Server side of the hive node biomass display. */
public class HiveNodeKillCountDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final HiveNodeKillCountDataProvider INSTANCE = new HiveNodeKillCountDataProvider();

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof
                org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity node) {
            tag.putInt(EPCAJadeIds.NBT_KILL_COUNT, node.getKillCount());
            tag.putInt("EPCA_KillCountMax",
                    org.tdddd.epca.impl.overworld.registry.blocks.block.entity.HiveNodeBlockEntity.MAX_KILL_COUNT);
        }
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.KILL_COUNT_INFO;
    }
}
