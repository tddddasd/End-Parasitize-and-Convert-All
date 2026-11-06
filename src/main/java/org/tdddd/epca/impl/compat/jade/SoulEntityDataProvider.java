package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;
import org.tdddd.epca.impl.soul.SoulSystem;

public class SoulEntityDataProvider implements IServerDataProvider<EntityAccessor> {

    public static final SoulEntityDataProvider INSTANCE = new SoulEntityDataProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath("epca", "soul_info");

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof LivingEntity living)) return;
        double max = SoulSystem.getMaxSoul(living);
        if (max <= 0.0) return;
        data.putDouble("EpcaSoulValue", SoulSystem.getSoul(living));
        data.putDouble("EpcaSoulMaxValue", max);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}