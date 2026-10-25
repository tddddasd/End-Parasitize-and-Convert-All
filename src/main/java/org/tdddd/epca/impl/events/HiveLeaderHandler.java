package org.tdddd.epca.impl.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HiveLeaderHandler {

    //  STAGE B SPEC

    /**  {@code epca:ender_blade_scrap}  id */
    private static final ResourceLocation ENDER_BLADE_SCRAP_ID =
            new ResourceLocation("epca", "ender_blade_scrap");
    /** SPEC <b>70%</b>  */
    private static final float ENDER_BLADE_CHANCE = 0.7F;
    /** SPEC <b>3 </b> = 60 tick20 tick = 1  */
    private static final int ENDER_EROSION_DURATION_TICKS = 60;
    /** SPEC<b>II </b>{@code MobEffectInstance}  amplifier 1  II  */
    private static final int ENDER_EROSION_AMPLIFIER = 1;

    /**  {@code epca:beckon_membrane}  id */
    private static final ResourceLocation BECKON_MEMBRANE_ID =
            new ResourceLocation("epca", "beckon_membrane");
    /** SPEC <b>30 </b> = 600 tick */
    private static final int PARA_CURSE_DURATION_TICKS = 600;
    /** SPEC<b>II </b>amplifier 1 = II  600 /  */
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

    /**
     * STAGE B
     *
     * <p><b>STAGE 3 </b>
     * {@code event.setAmount(event.getAmount() + 1.0f)}  40 tick
     * {@code ModDamageTypes.MINIMUM}  1
     * {@code HiveLeaderOrganDamageHandler#onHiveLeaderAttack}
     * {@code OrganStatSummary#minimumDamage()}
     * {@code minimumDamageIntervalTicks()} 2
     *  {@code ExtraDamageLastTick}
     * {@code HiveLeaderOrganMinDamage}</p>
     *
     * <p><b>STAGE B </b></p>
     * <ol>
     *   <li><b></b> {@code epca:ender_blade_scrap}<b></b>
     *        <b>70%</b>  <b>3  II</b>
     *       {@code ModEffects.ENDER_EROSION} = {@code epca:ender_erosion}
     *       {@code MobEffectInstance}  amplifier
     *       0 = I 1 = II  {@code MobEffectInstance#getAmplifier}
     *       amp + 1 </li>
     *   <li><b></b> {@code epca:beckon_membrane}<b></b>
     *       16  <b>30 600 tick II</b>
     *       {@code ModEffects.PARA_CURSE} = {@code epca:para_curse}amplifier 1</li>
     * </ol>
     * <p>/1  3  70%
     * 1  16  30  II
     * {@link OrganStatSummary#countIn(OrganSlotGroup, ResourceLocation)}
     *  {@code countOf}
     * </p>
     *
     * <p><b>STAGE C </b>70%  2
     * + 5  ISPEC
     * FEAR PARA_CURSE  {@code if}
     *  70%
     * </p>
     *
     * <p>{@code event.getSource().getEntity()} <b></b>/
     * {@code LivingHurtEvent}  Forge
     * {@code net.minecraftforge.event.entity.living.LivingHurtEvent}
     * {@code getEntity()} </p>
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;

        LivingEntity target = event.getEntity();
        if (target == null || target == player) return;
        //  LivingHurtEvent /
        if (player.level().isClientSide()) return;

        OrganStatSummary summary = OrganStatSummary.compute(
                HiveLeaderOrganSavedData.readOrCreate(player));

        applyEnderBladeProc(player, target, summary);
        applyBeckonMembraneProc(player, target, summary);
    }

    /**
     *  / 70%  3  II
     *
     * <p> {@code player.level().random}
     * </p>
     */
    private static void applyEnderBladeProc(Player player, LivingEntity target, OrganStatSummary summary) {
        if (countInArms(summary, ENDER_BLADE_SCRAP_ID) <= 0) return;
        if (player.level().random.nextFloat() >= ENDER_BLADE_CHANCE) return;
        target.addEffect(new MobEffectInstance(
                ModEffects.ENDER_EROSION.get(), ENDER_EROSION_DURATION_TICKS, ENDER_EROSION_AMPLIFIER));
    }

    /**
     *  30  II
     *
     * <p>SPEC <b></b> 70%
     * amplifier  1= II  +1 2
     *  SPEC 30  II </p>
     */
    private static void applyBeckonMembraneProc(Player player, LivingEntity target, OrganStatSummary summary) {
        if (countInOuter(summary, BECKON_MEMBRANE_ID) <= 0) return;
        target.addEffect(new MobEffectInstance(
                ModEffects.PARA_CURSE.get(), PARA_CURSE_DURATION_TICKS, PARA_CURSE_AMPLIFIER));
    }

    /**  6 16  */
    private static int countInOuter(OrganStatSummary summary, ResourceLocation itemId) {
        int total = 0;
        for (OrganSlotGroup group : OrganSlotGroup.OUTER_GROUPS) {
            total += summary.countIn(group, itemId);
        }
        return total;
    }

    /**  +  3  */
    private static int countInArms(OrganStatSummary summary, ResourceLocation itemId) {
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
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.POISON || effect == MobEffects.HUNGER || effect == MobEffects.CONFUSION ||
                effect == ModEffects.PARA_CURSE.get() || effect == ModEffects.VIRAL.get() || effect == ModEffects.FEAR.get() ||
                effect == ModEffects.SOLIDIFY.get() || effect == ModEffects.CORROSIVE.get() || effect == ModEffects.CONTEMPT_INORGANIC.get() ||
                effect == ModEffects.NEEDLER.get()) {
            event.setResult(Event.Result.DENY);
        }
    }

    // STAGE 1 / SPEC A1 onEntityInteract(PlayerInteractEvent.EntityInteract)
    //  ((IParasite) target).setFollowTarget(player.getUUID())
    //  A1
    //
    // IParasite#setFollowTarget / getFollowTargetFollowTargetGoal
    // ToggleFollowPacketPlayerMixin#isFriendlyParasite  follow
    // "FollowTarget"  I
    //
    // STAGE 3 / SPEC canApplyExtraDamage / setExtraDamageCooldown
    //  "ExtraDamageLastTick"
    //  HiveLeaderOrganDamageHandler
    //  per-player  "HiveLeaderOrganMinDamage"
    //
    //  STAGE C
    //  onLivingHurt
    //
    //     if (player.level().random.nextFloat() < 0.7f) {
    //         LivingEntity target = event.getEntity();
    //         int currentAmp = target.getEffect(ModEffects.PARA_CURSE.get()) != null ?
    //                 target.getEffect(ModEffects.PARA_CURSE.get()).getAmplifier() : -1;
    //         int newAmp = Math.min(currentAmp + 1, 2);
    //         target.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE.get(), 600, newAmp));
    //         target.addEffect(new MobEffectInstance(ModEffects.FEAR.get(), 300, 1));
    //     }
    //
    // <b> STAGE B  proc </b> 70% / 3  II
    //  30  IISPEC
    //  FEAR PARA_CURSE  FEAR <b> 70% </b>
    //   *  PARA_CURSE  FEAR  FEAR 70%
    //     100%  if ""
    //   *  FEAR70% / 300 tick / amplifier 1
    //      SPEC  FEAR
    //  FEAR
    // SPEC MobEffect  FEAR
    // onEffectApplicable
    //
    // ModEffects  import onEffectApplicable  PARA_CURSE/FEAR
    // MobEffects  import POISON/HUNGER/CONFUSIONMobEffectInstance
    //  LivingEntity  STAGE B  proc

    /** A hive leader killing something earns an evolution point and a short regeneration. */
    @SubscribeEvent
    public static void onLeaderKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        org.tdddd.epca.impl.overworld.data.HiveLeaderPlayerHooks.onKill(player, event.getEntity());
    }

    /** Fire damage can make a hive leader angry. */
    @SubscribeEvent
    public static void onLeaderHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        event.setAmount(org.tdddd.epca.impl.overworld.data.HiveLeaderPlayerHooks
                .onHurt(player, event.getSource(), event.getAmount()));
    }
}
