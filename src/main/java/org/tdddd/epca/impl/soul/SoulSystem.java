package org.tdddd.epca.impl.soul;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.yawning_neko_api.damages.MinimumDamage;

@EventBusSubscriber(modid = epca.MODID)
public final class SoulSystem {

    private SoulSystem() { }

    public static float soulDamageFor(LivingEntity attacker) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString();
        return switch (id) {
            case "epca:ripper", "epca:fins", "epca:mozzie",
                 "epca:flying_carrier", "epca:light_carrier" -> 0.1F;
            case "epca:reshape_longarms", "epca:reshape_yelloweye" -> 1.0F;
            default -> 0.5F;
        };
    }

    public static int protectionLevel(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.SOUL_PROTECTION);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    public static float reduceByProtection(LivingEntity target, float amount) {
        float reduction = Math.min(0.6F, protectionLevel(target) * 0.05F);
        return amount * (1.0F - reduction);
    }

    public static float applySoulDamage(LivingEntity target, DamageSource source, float amount) {
        if (target == null || amount <= 0.0F) return 0.0F;
        if (target.level().isClientSide()) return 0.0F;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return 0.0F;
        if (ParasiteEvents.isParasiteEntity(target)) return 0.0F;
        float dealt = reduceByProtection(target, amount);
        MinimumDamage.apply(target, source, dealt);
        return dealt;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (!ParasiteEvents.isParasiteByTagOrInterface(attacker)) return;
        applySoulDamage(event.getEntity(), event.getSource(), soulDamageFor(attacker));
    }
}