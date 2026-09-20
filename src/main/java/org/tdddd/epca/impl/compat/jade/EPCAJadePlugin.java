package org.tdddd.epca.impl.compat.jade;

import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.BeckonCore;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.BeckonCoreBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;


@WailaPlugin
public class EPCAJadePlugin implements IWailaPlugin {

    
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(KillCountEntityDataProvider.INSTANCE, LivingEntity.class);

        registration.registerEntityDataProvider(DamageAdaptationEntityDataProvider.INSTANCE, LivingEntity.class);

        registration.registerBlockDataProvider(KillCountBlockDataProvider.INSTANCE, BeckonCoreBlockEntity.class);
    }

    
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(DamageAdaptationEntityComponentProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityComponent(KillCountEntityComponentProvider.INSTANCE, LivingEntity.class);
        registration.registerBlockComponent(KillCountBlockComponentProvider.INSTANCE, BeckonCore.class);
        // Config keys may already be registered by yawningapi
        try {
            registration.addConfig(EPCAJadeIds.KILL_COUNT_INFO, true);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            registration.addConfig(EPCAJadeIds.DAMAGE_ADAPTATION_INFO, true);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
