package org.tdddd.epca.impl.client.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;
import org.tdddd.epca.impl.soul.SoulSystem;

public enum SoulServerProvider implements IServerDataProvider<EntityAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = new ResourceLocation("epca", "soul");

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        var entity = accessor.getEntity();
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
        double max = SoulSystem.getMaxSoul(living);
        if (max <= 0.0) return;
        data.putDouble("EpcaSoulValue", SoulSystem.getSoul(living));
        data.putDouble("EpcaSoulMaxValue", max);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
