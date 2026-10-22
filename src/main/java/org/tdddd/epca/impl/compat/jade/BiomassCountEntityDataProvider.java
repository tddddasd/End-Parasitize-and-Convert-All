package org.tdddd.epca.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;


public class BiomassCountEntityDataProvider implements IServerDataProvider<EntityAccessor> {

    public static final BiomassCountEntityDataProvider INSTANCE = new BiomassCountEntityDataProvider();

    @Override
    public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        int count = EntityBiomassCountManager.getCurrentBiomassCount(living);
        if (count > 0) {
            tag.putInt(EPCAJadeIds.NBT_BIOMASS_COUNT, count);
        }
    }

    @Override
    public Identifier getUid() {
        return EPCAJadeIds.BIOMASS_COUNT_INFO;
    }
}
