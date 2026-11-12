package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.utils.ShieldProtectionHelper;
import org.tdddd.yawning_neko_api.events.MinimumDamageEvent;

@EventBusSubscriber(modid = epca.MODID)
public final class MinimumDamageShieldHandler {

    private MinimumDamageShieldHandler() { }

    @SubscribeEvent
    public static void onMinimumDamage(MinimumDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        float reduced = org.tdddd.epca.impl.soul.SoulSystem.reduceByProtection(entity, event.getAmount());
        if (reduced <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        if (reduced < event.getAmount()) {
            event.setAmount(reduced);
        }
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
