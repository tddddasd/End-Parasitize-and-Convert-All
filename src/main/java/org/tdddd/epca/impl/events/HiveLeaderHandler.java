package org.tdddd.epca.impl.events;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@EventBusSubscriber(modid = epca.MODID)
public class HiveLeaderHandler {

    private static final Identifier ENDER_BLADE_SCRAP_ID =
            Identifier.fromNamespaceAndPath("epca", "ender_blade_scrap");

    private static final float ENDER_BLADE_CHANCE = 0.7F;

    private static final int ENDER_EROSION_DURATION_TICKS = 60;

    private static final int ENDER_EROSION_AMPLIFIER = 1;

    private static final Identifier BECKON_MEMBRANE_ID =
            Identifier.fromNamespaceAndPath("epca", "beckon_membrane");

    private static final int PARA_CURSE_DURATION_TICKS = 600;

    private static final int PARA_CURSE_AMPLIFIER = 1;

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        if (HiveLeaderManager.isHiveLeader(player.getUUID())) {
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
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;

        LivingEntity target = event.getEntity();
        if (target == null || target == player) return;

        if (player.level().isClientSide()) return;

        OrganStatSummary summary = OrganStatSummary.compute(
                HiveLeaderOrganSavedData.readOrCreate(player));

        applyEnderBladeProc(player, target, summary);
        applyBeckonMembraneProc(player, target, summary);
    }

    private static void applyEnderBladeProc(Player player, LivingEntity target, OrganStatSummary summary) {
        if (countInArms(summary, ENDER_BLADE_SCRAP_ID) <= 0) return;
        if (player.level().getRandom().nextFloat() >= ENDER_BLADE_CHANCE) return;
        target.addEffect(new MobEffectInstance(
                ModEffects.ENDER_EROSION, ENDER_EROSION_DURATION_TICKS, ENDER_EROSION_AMPLIFIER));
    }

    private static void applyBeckonMembraneProc(Player player, LivingEntity target, OrganStatSummary summary) {
        if (countInOuter(summary, BECKON_MEMBRANE_ID) <= 0) return;
        target.addEffect(new MobEffectInstance(
                ModEffects.PARA_CURSE, PARA_CURSE_DURATION_TICKS, PARA_CURSE_AMPLIFIER));
    }

    private static int countInOuter(OrganStatSummary summary, Identifier itemId) {
        int total = 0;
        for (OrganSlotGroup group : OrganSlotGroup.OUTER_GROUPS) {
            total += summary.countIn(group, itemId);
        }
        return total;
    }

    private static int countInArms(OrganStatSummary summary, Identifier itemId) {
        int total = 0;
        for (OrganSlotGroup group : OrganSlotGroup.ARM_GROUPS) {
            total += summary.countIn(group, itemId);
        }
        return total;
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;

        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.POISON || effect == MobEffects.HUNGER || effect == MobEffects.NAUSEA ||
                effect == ModEffects.PARA_CURSE || effect == ModEffects.VIRAL || effect == ModEffects.FEAR ||
                effect == ModEffects.SOLIDIFY || effect == ModEffects.CORROSIVE || effect == ModEffects.CONTEMPT_INORGANIC ||
                effect == ModEffects.NEEDLER) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onLeaderKill(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        org.tdddd.epca.impl.overworld.data.HiveLeaderPlayerHooks.onKill(player, event.getEntity());
    }

    @SubscribeEvent
    public static void onLeaderHurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        event.setAmount(org.tdddd.epca.impl.overworld.data.HiveLeaderPlayerHooks
                .onHurt(player, event.getSource(), event.getAmount()));
    }
}
