package org.tdddd.epca.impl.events;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

@EventBusSubscriber(modid = epca.MODID)
public class NestLeaderHandler {
    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        if (NestLeaderManager.isNestLeader(player.getUUID())) {
            event.setDisplayname(Component.literal(event.getDisplayname().getString())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        BiomassEventHandler.syncBiomass(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;

        Level level = player.level();
        Registry<DamageType> registry = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder);

        event.setAmount(event.getAmount() + 1.0f);

        if (canApplyExtraDamage(player)) {
            event.getEntity().hurt(minimumSource, 1.0f);
            setExtraDamageCooldown(player);
        }

        if (player.level().getRandom().nextFloat() < 0.7f) {
            LivingEntity target = event.getEntity();
            int currentAmp = target.getEffect(ModEffects.COTH) != null ?
                    target.getEffect(ModEffects.COTH).getAmplifier() : -1;
            int newAmp = Math.min(currentAmp + 1, 2);
            target.addEffect(new MobEffectInstance(ModEffects.COTH, 600, newAmp));
            target.addEffect(new MobEffectInstance(ModEffects.FEAR, 300, 1));
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;
        
        
        
        
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.POISON || effect == MobEffects.HUNGER || effect == MobEffects.NAUSEA ||
                effect == ModEffects.COTH || effect == ModEffects.VIRAL || effect == ModEffects.FEAR ||
                effect == ModEffects.SOLIDIFY || effect == ModEffects.CORROSIVE || effect == ModEffects.CONTEMPT_INORGANIC ||
                effect == ModEffects.NEEDLER) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.getMainHandItem().isEmpty()) return;
        Entity target = event.getTarget();
        if (target instanceof IParasite && target instanceof LivingEntity) {
            ((IParasite) target).setFollowTarget(player.getUUID());
            event.setCanceled(true);
        }
    }

    private static boolean canApplyExtraDamage(Player player) {
        long last = player.getPersistentData().getLong("ExtraDamageLastTick").orElse(0L);
        long now = player.level().getGameTime();
        return (now - last) >= 40;
    }
    private static void setExtraDamageCooldown(Player player) {
        player.getPersistentData().putLong("ExtraDamageLastTick", player.level().getGameTime());
    }
}