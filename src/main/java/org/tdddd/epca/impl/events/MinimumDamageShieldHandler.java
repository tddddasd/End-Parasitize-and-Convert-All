package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.utils.ShieldProtectionHelper;
import org.tdddd.yawning_neko_api.events.MinimumDamageEvent;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MinimumDamageShieldHandler {

    private MinimumDamageShieldHandler() { }

    @SubscribeEvent
    public static void onMinimumDamage(MinimumDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        float remaining = ShieldProtectionHelper.applyShieldPool(entity, event.getAmount());
        if (remaining <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        if (remaining < event.getAmount()) {
            event.setAmount(remaining);
        }
    }
}
