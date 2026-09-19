package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.data.EntityKillCountManager;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * 服务端专用（击杀数 / 实体）。
 *
 * <p>只实现 Jade 的服务端数据接口 {@link IServerDataProvider}：签名、字段与 import 都不含任何
 * 客户端类型（{@code ITooltip}、{@code JadeUI}、{@code LayoutElement} 等），因此可以在专用服务器上
 * 被 {@code register(IWailaCommonRegistration)} 安全加载。
 *
 * <p>UID 与 NBT 键保持不变，客户端渲染端见
 * {@link KillCountEntityComponentProvider}。
 */
public class KillCountEntityDataProvider implements IServerDataProvider<EntityAccessor> {

    public static final KillCountEntityDataProvider INSTANCE = new KillCountEntityDataProvider();

    @Override
    public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        int count = EntityKillCountManager.getCurrentKillCount(living);
        if (count > 0) {
            tag.putInt(EPCAJadeIds.NBT_KILL_COUNT, count);
        }
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.KILL_COUNT_INFO;
    }
}
