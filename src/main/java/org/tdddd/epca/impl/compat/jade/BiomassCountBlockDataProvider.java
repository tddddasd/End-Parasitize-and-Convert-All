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

    /**
     * 独立 id（不再与 {@link HiveNodeBiomassCountDataProvider} 共用 {@code epca:kill_count_info}）：
     * 两者是不同实例、写入的 NBT 语义也不同（召唤柱只在生物量为正时写），
     * 同 id 会触发 Jade 的 {@code Found different provider instances with same id} 警告。
     */
    @Override
    public Identifier getUid() {
        return EPCAJadeIds.BIOMASS_COUNT_BLOCK_DATA;
    }
}
