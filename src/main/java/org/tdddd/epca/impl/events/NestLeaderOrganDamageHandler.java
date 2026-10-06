package org.tdddd.epca.impl.events;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
 *       {@code minimumDamageIntervalTicks()} 40 tick = 2 </li>
 *   <li><b></b>{@link #onNestLeaderHurt}
 *       {@link NestLeaderOrganAdaptation#resolve}</li>
 * </ul>
 *
 * <h2></h2>
 * <p>YawningNekoAPI
 *  {@code MinimumDamageManager} <b></b>
 * {@code data/yawning_neko_api/minimum_damage/*.json}
 *   API  {@link ModDamageTypes#MINIMUM}
 * {@code yawning_neko_api:minimum} {@link DamageSource}
 * {@code target.hurt(source, amount)}API  {@code LivingEntityMixin#onHurtMinimum}
 *  {@code hurt}  HEAD  amount
 *  {@code BioTortIncarnation#applyMinimumDamage}
 * {@code new DamageSource(holder, attacker, attacker)}</p>
 *
 * <h2></h2>
 * <p><b> + </b>
 * {@code NestLeaderOrganMinDamage} {@code last_tick} + {@code target}
 * </p>
 * <p>
 * {@code event.getAmount() < minimumDamage}
 * </p>
 *
 * <h2>1.20.1 -&gt; 26.1.2  API </h2>
 * <ul>
 *   <li>{@code net.minecraftforge.event.entity.living.LivingHurtEvent} -&gt;
 *       {@code net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent}
 *       26.1.2 {@code getEntity()} / {@code getSource()} /
 *       {@code getAmount()} / {@code setAmount(float)} </li>
 *   <li>{@code registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)} -&gt;
 *       {@code registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)}26.1.2
 *        {@code NestLeaderHandler}  53 </li>
 *   <li>{@code registry.getHolderOrThrow(KEY)} -&gt; {@code registry.getOrThrow(KEY)}</li>
 *   <li>{@code @Mod.EventBusSubscriber(bus = FORGE)} -&gt; {@code @EventBusSubscriber(modid = epca.MODID)}
 *       26.1.2  game bus</li>
 *   <li><b>NBT  Optional</b>{@code CompoundTag}  26.1.2
 *       <b></b> {@code putUUID/getUUID/hasUUID} _tmp_26src {@code CompoundTag.java}
 *        UUID  {@code UUID}
 *       <b></b>{@code UUID#toString()} / {@code getStringOr(name, "")}
 *        {@code target} UUID</li>
 * </ul>
 */
@EventBusSubscriber(modid = epca.MODID)
public final class NestLeaderOrganDamageHandler {

    /**  */
    private static final String MIN_DAMAGE_KEY = "NestLeaderOrganMinDamage";
    private static final String TAG_LAST_TICK = "last_tick";
    private static final String TAG_TARGET = "target";

    private NestLeaderOrganDamageHandler() {
    }

    /**  */
    @SubscribeEvent
    public static void onNestLeaderAttack(LivingIncomingDamageEvent event) {
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
    public static void onNestLeaderHurt(LivingIncomingDamageEvent event) {
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
        // <b></b>=
        // NestLeaderOrganSavedData
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
                .lookupOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder, attacker, attacker);
        target.hurt(minimumSource, amount);

        CompoundTag tag = attacker.getPersistentData().getCompoundOrEmpty(MIN_DAMAGE_KEY);
        tag.putLong(TAG_LAST_TICK, now);
        // 26.1.2CompoundTag  putUUID
        tag.putString(TAG_TARGET, target.getUUID().toString());
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
        CompoundTag tag = attacker.getPersistentData().getCompoundOrEmpty(MIN_DAMAGE_KEY);
        String stored = tag.getStringOr(TAG_TARGET, "");
        if (stored.isEmpty() || !tag.contains(TAG_LAST_TICK)) return true;
        if (!stored.equals(target.getUUID().toString())) return true;
        return now - tag.getLongOr(TAG_LAST_TICK, 0L) >= interval;
    }
}

