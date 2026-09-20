package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;


public class KillCountBlockDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final KillCountBlockDataProvider INSTANCE = new KillCountBlockDataProvider();

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof BeckonCoreBlockEntity core) {
            int count = core.getKillCount();
            if (count > 0) {
                tag.putInt(EPCAJadeIds.NBT_KILL_COUNT, count);
            }
        }
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.KILL_COUNT_INFO;
    }
}
