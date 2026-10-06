package org.tdddd.epca.impl.events;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganAdaptation;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganEffects;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

/**
 * STAGE 3 SPEC C9  /  /
 *
 * <h2></h2>
 * <ul>
 *   <li><b></b>{@link #onNestLeaderAttack}
 *       {@code OrganStatSummary#minimumDamage()}  YawningNekoAPI
 *        +  mixin
 *       {@code minimumDamageIntervalTicks()} 40 tick = 2
 *        COTH / FEAR</li>
 *   <li><b></b>{@link #onNestLeaderHurt} {@code LivingHurtEvent}
 *        {@link NestLeaderOrganAdaptation#resolve}</li>
 * </ul>
 *
 * <h2></h2>
 * <p>YawningNekoAPI
 *  {@code MinimumDamageManager} <b></b>
 * {@code data/yawning_neko_api/minimum_damage/*.json}
 *   API  {@link ModDamageTypes#MINIMUM}
 * {@code yawning_neko_api:minimum}
 * {@code YawningNekoAPI/src/main/resources/data/yawning_neko_api/damage_type/minimum.json}
 *  {@link DamageSource}  {@code target.hurt(source, amount)}API
 * {@code LivingEntityMixin#onHurtMinimum}  {@code hurt}  HEAD  amount
 *  {@code BioTortIncarnation#applyMinimumDamage}
 * {@code new DamageSource(holder, attacker, directEntity)}</p>
 *
 * <h2></h2>
 * <p><b> + </b>
 * {@code NestLeaderOrganMinDamage} {@code last_tick} + {@code target}
 * </p>
 * <p><b></b>
 * {@code NestLeaderOrganSavedData}<b></b>
 *  2 </p>
 * <p>
 * {@code event.getAmount() < minimumDamage}
 * <b></b>
 * {@code event.setAmount(amount + 1.0f)} SPEC </p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NestLeaderOrganDamageHandler {

    /**  */
    private static final String MIN_DAMAGE_KEY = "NestLeaderOrganMinDamage";
    private static final String TAG_LAST_TICK = "last_tick";
    private static final String TAG_TARGET = "target";

    private NestLeaderOrganDamageHandler() {
    }

    /**  */
    @SubscribeEvent
    public static void onNestLeaderAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (!NestLeaderManager.isNestLeader(attacker.getUUID())) return;

        LivingEntity target = event.getEntity();
        if (target == null || target == attacker) return;
        if (attacker.level().isClientSide()) return;

        OrganStatSummary summary = OrganStatSummary.compute(
                NestLeaderOrganSavedData.readOrCreate(attacker));
        double minimumDamage = summary.minimumDamage();
        if (minimumDamage <= 0.0D) return;

        if (event.getAmount() >= minimumDamage) return;

        long now = attacker.level().getGameTime();
        if (!intervalElapsed(attacker, target, now, Math.max(1, summary.minimumDamageIntervalTicks()))) {
            return;
        }

        applyMinimumDamage(attacker, target, (float) minimumDamage, now);
    }

    /**  + SPEC C9 / D6 */
    @SubscribeEvent
    public static void onNestLeaderHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;
        if (player.level().isClientSide()) return;

        OrganStatSummary summary = OrganStatSummary.compute(
                NestLeaderOrganSavedData.readOrCreate(player));
        float adapted = NestLeaderOrganAdaptation.resolve(player, event.getSource(), summary, event.getAmount());
        if (adapted != event.getAmount()) {
            event.setAmount(adapted);
        }
    }

    /**  /  UUID  */
    public static void clear(Player player) {
        NestLeaderOrganEffects.clear(player);
        if (player == null) return;
        player.getPersistentData().remove(MIN_DAMAGE_KEY);
        // STAGE B
        // level.getGameTime()
        // NestLeaderOrganTeleportHandler
        // //
        // <b></b> "PlayerPersisted"
        // ServerPlayer#restoreFrom  11541158 <b></b>
        // =
        //  NestLeaderOrganSavedData
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            org.tdddd.epca.impl.network.packet.c2s.NestLeaderOrganTeleportHandler.clear(serverPlayer);
        }
    }


    /**
     *  YawningNekoAPI
     * {@link ModDamageTypes#MINIMUM} + {@code new DamageSource(holder, causing, direct)} + {@code hurt}
     *
     */
    private static void applyMinimumDamage(Player attacker, LivingEntity target, float amount, long now) {
        Registry<DamageType> registry = attacker.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder, attacker, attacker);
        target.hurt(minimumSource, amount);

        var tag = attacker.getPersistentData().getCompound(MIN_DAMAGE_KEY);
        tag.putLong(TAG_LAST_TICK, now);
        tag.putUUID(TAG_TARGET, target.getUUID());
        attacker.getPersistentData().put(MIN_DAMAGE_KEY, tag);
    }

    /**
     *  {@code now - last >= interval}
     *
     * <p> true <b></b>
     * {@link #applyMinimumDamage} /
     * </p>
     */
    private static boolean intervalElapsed(Player attacker, LivingEntity target, long now, int interval) {
        var tag = attacker.getPersistentData().getCompound(MIN_DAMAGE_KEY);
        if (!tag.hasUUID(TAG_TARGET) || !tag.contains(TAG_LAST_TICK)) return true;
        if (!tag.getUUID(TAG_TARGET).equals(target.getUUID())) return true;
        return now - tag.getLong(TAG_LAST_TICK) >= interval;
    }
}

