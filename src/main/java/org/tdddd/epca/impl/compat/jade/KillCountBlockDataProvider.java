package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * 服务端专用（击杀数 / 方块）。
 *
 * <p>Jade 26.1.11 没有独立的 {@code IServerBlockDataProvider}：方块与实体共用
 * {@link IServerDataProvider}（{@code registerBlockDataProvider(IServerDataProvider<BlockAccessor>, Class<?>)}）。
 * 本类只实现该服务端接口，可在专用服务器上安全加载。
 */
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
