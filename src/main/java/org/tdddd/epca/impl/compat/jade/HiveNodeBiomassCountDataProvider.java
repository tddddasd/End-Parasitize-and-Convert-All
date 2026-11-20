package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

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

    /**
     * 独立 id（不再与 {@link BiomassCountBlockDataProvider} 共用 {@code epca:kill_count_info}）：
     * 两者是不同实例、写入的 NBT 语义也不同，同 id 会触发 Jade 的
     * {@code Found different provider instances with same id} 警告。
     */
    @Override
    public Identifier getUid() {
        return EPCAJadeIds.HIVE_NODE_BIOMASS_COUNT_DATA;
    }
}
