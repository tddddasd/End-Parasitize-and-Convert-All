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
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@EventBusSubscriber(modid = epca.MODID)
public class NestLeaderHandler {

    //  STAGE B SPEC

    /**  {@code epca:ender_blade_scrap}  id */
    private static final Identifier ENDER_BLADE_SCRAP_ID =
            Identifier.fromNamespaceAndPath("epca", "ender_blade_scrap");
    /** SPEC <b>70%</b>  */
    private static final float ENDER_BLADE_CHANCE = 0.7F;
    /** SPEC <b>3 </b> = 60 tick20 tick = 1  */
    private static final int ENDER_EROSION_DURATION_TICKS = 60;
    /** SPEC<b>II </b>{@code MobEffectInstance}  amplifier 1  II  */
    private static final int ENDER_EROSION_AMPLIFIER = 1;

    /**  {@code epca:beckon_membrane}  id */
    private static final Identifier BECKON_MEMBRANE_ID =
            Identifier.fromNamespaceAndPath("epca", "beckon_membrane");
    /** SPEC <b>30 </b> = 600 tick */
    private static final int COTH_DURATION_TICKS = 600;
    /** SPEC<b>II </b>amplifier 1 = II  */
    private static final int COTH_AMPLIFIER = 1;

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

    /**
     * STAGE B
     *
     * <p><b>STAGE 3 </b>
     * {@code event.setAmount(event.getAmount() + 1.0f)}  40 tick
     * {@code ModDamageTypes.MINIMUM}  1
     * {@code NestLeaderOrganDamageHandler#onNestLeaderAttack}
     * {@code OrganStatSummary#minimumDamage()}
     * {@code minimumDamageIntervalTicks()} 2
     *  {@code ExtraDamageLastTick}
     * {@code NestLeaderOrganMinDamage}</p>
     *
     * <p><b>STAGE B </b></p>
     * <ol>
     *   <li><b></b> {@code epca:ender_blade_scrap}<b></b>
     *        <b>70%</b>  <b>3  II</b>
     *       {@code ModEffects.ENDER_EROSION} = {@code epca:ender_erosion}
     *       {@code MobEffectInstance}  amplifier
     *       0 = I 1 = II </li>
     *   <li><b></b> {@code epca:beckon_membrane}<b></b>
     *       16  <b>30 600 tick II</b>
     *       {@code ModEffects.COTH} = {@code epca:coth}amplifier 1</li>
     * </ol>
     * <p>/1  3  70%
     * 1  16  30  II
     * {@link OrganStatSummary#countIn(OrganSlotGroup, Identifier)}
     *  {@code countOf}
     * </p>
     *
     * <p><b>STAGE C </b>70%  2
     * + 5  ISPEC
     * FEAR COTH  {@code if}
     *  70% </p>
     *
     * <p>26.1.2 1.20.1  {@code LivingHurtEvent}
     * {@code LivingIncomingDamageEvent}NeoForge {@code getSource()} /
     * {@code getEntity()} / {@code getAmount()} / {@code setAmount(float)} </p>
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;

        LivingEntity target = event.getEntity();
        if (target == null || target == player) return;
        // /
        if (player.level().isClientSide()) return;

        OrganStatSummary summary = OrganStatSummary.compute(
                NestLeaderOrganSavedData.readOrCreate(player));

        applyEnderBladeProc(player, target, summary);
        applyBeckonMembraneProc(player, target, summary);
    }

    /**
     *  / 70%  3  II
     *
     * <p> {@code player.level().getRandom()}
     * </p>
     */
    private static void applyEnderBladeProc(Player player, LivingEntity target, OrganStatSummary summary) {
        if (countInArms(summary, ENDER_BLADE_SCRAP_ID) <= 0) return;
        if (player.level().getRandom().nextFloat() >= ENDER_BLADE_CHANCE) return;
        target.addEffect(new MobEffectInstance(
                ModEffects.ENDER_EROSION, ENDER_EROSION_DURATION_TICKS, ENDER_EROSION_AMPLIFIER));
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
                ModEffects.COTH, COTH_DURATION_TICKS, COTH_AMPLIFIER));
    }

    /**  6 16  */
    private static int countInOuter(OrganStatSummary summary, Identifier itemId) {
        int total = 0;
        for (OrganSlotGroup group : OrganSlotGroup.OUTER_GROUPS) {
            total += summary.countIn(group, itemId);
        }
        return total;
    }

    /**  +  3  */
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
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;

        // 26.1.2event.getEffectInstance().getEffect()  Holder<MobEffect>
        //  ModEffects.*DeferredHolder Holder
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.POISON || effect == MobEffects.HUNGER || effect == MobEffects.NAUSEA ||
                effect == ModEffects.COTH || effect == ModEffects.VIRAL || effect == ModEffects.FEAR ||
                effect == ModEffects.SOLIDIFY || effect == ModEffects.CORROSIVE || effect == ModEffects.CONTEMPT_INORGANIC ||
                effect == ModEffects.NEEDLER) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // STAGE 1 / SPEC A1 onEntityInteract(PlayerInteractEvent.EntityInteract)
    //  ((IParasite) target).setFollowTarget(player.getUUID())
    // 1.20.1
    //  A1 26
    //
    // IParasite#setFollowTarget / getFollowTargetFollowTargetGoal
    // ToggleFollowPacketPlayerMixin#isFriendlyParasite  follow
    // "FollowTarget"  I
    //
    // STAGE 3 / SPEC canApplyExtraDamage / setExtraDamageCooldown
    //  "ExtraDamageLastTick"
    //  NestLeaderOrganDamageHandler
    //  per-player  "NestLeaderOrganMinDamage"
    //
    //  STAGE C
    //  onLivingHurt
    //
    //     if (player.level().getRandom().nextFloat() < 0.7f) {
    //         LivingEntity target = event.getEntity();
    //         int currentAmp = target.getEffect(ModEffects.COTH) != null ?
    //                 target.getEffect(ModEffects.COTH).getAmplifier() : -1;
    //         int newAmp = Math.min(currentAmp + 1, 2);
    //         target.addEffect(new MobEffectInstance(ModEffects.COTH, 600, newAmp));
    //         target.addEffect(new MobEffectInstance(ModEffects.FEAR, 300, 1));
    //     }
    //
    // <b> STAGE B  proc </b> 70% / 3  II
    //  30  IISPEC
    //  FEAR COTH  FEAR <b> 70% </b>
    //   *  COTH  FEAR  FEAR 70%
    //     100%  if ""
    //   *  FEAR70% / 300 tick / amplifier 1
    //      SPEC  FEAR
    //  FEAR
    // SPEC MobEffect  FEAR
    // onEffectApplicable
}

