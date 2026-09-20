package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.data.EntityKillCountManager;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;


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
