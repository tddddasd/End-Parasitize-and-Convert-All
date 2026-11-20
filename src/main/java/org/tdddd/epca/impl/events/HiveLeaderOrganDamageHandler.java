package org.tdddd.epca.impl.events;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganDamageTaken;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganEffects;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;
import org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpeciesManager;
import org.tdddd.epca.impl.overworld.data.organ.stats.OrganStatSummary;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

@EventBusSubscriber(modid = epca.MODID)
public final class HiveLeaderOrganDamageHandler {

    private static final String MIN_DAMAGE_KEY = "HiveLeaderOrganMinDamage";

    /** 旧版「器官适应性」遗留的玩家持久化标签（2026-11 移除，登录/退出时顺手清掉）。 */
    private static final String LEGACY_ORGAN_ADAPTATION_KEY = "HiveLeaderOrganAdaptation";

    private HiveLeaderOrganDamageHandler() {
    }

    /**
     * 攻击最小伤害。
     *
     * <p>数值由当前特化种阶决定（虫染 0.5 / 初探 0.1 / 粗制 无 / 重塑 1.0），
     * 冷却统一为「同一目标 1 秒（20 tick）只触发一次」，由
     * {@link HiveLeaderSpeciesManager} 持久化管理。</p>
     */
    @SubscribeEvent
    public static void onHiveLeaderAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (!HiveLeaderManager.isHiveLeader(attacker.getUUID())) return;

        LivingEntity target = event.getEntity();
        if (target == null || target == attacker) return;
        if (attacker.level().isClientSide()) return;

        double minimumDamage = HiveLeaderSpeciesManager.minimumDamage(attacker);
        // 粗制种没有最小伤害（minimumDamage == 0）。
        if (minimumDamage <= 0.0D) return;

        if (event.getAmount() >= minimumDamage) return;

        if (!HiveLeaderSpeciesManager.minimumDamageOffCooldown(attacker, target)) {
            return;
        }

        applyMinimumDamage(attacker, target, (float) minimumDamage);
    }

    /**
     * 领巢者受击结算顺序：
     *
     * <ol>
     *   <li>器官「受伤倍率」（{@link HiveLeaderOrganDamageTaken#apply}）：按伤害类型查表相乘；</li>
     *   <li>种阶特化：重塑种的伤害适应性
     *       （{@link HiveLeaderSpeciesManager#modifyIncomingDamage} →
     *       {@code HiveLeaderReshapeAdaptation#adaptAndReduce}）。</li>
     * </ol>
     *
     * <p>器官<strong>不再提供任何伤害适应</strong>。护甲吸收 / 附魔减伤在
     * {@code LivingIncomingDamageEvent} 之后结算，所以以上两步与护甲、其它减伤是
     * <strong>叠乘</strong>关系。</p>
     */
    @SubscribeEvent
    public static void onHiveLeaderHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!HiveLeaderManager.isHiveLeader(player.getUUID())) return;
        if (player.level().isClientSide()) return;

        OrganStatSummary summary = OrganStatSummary.compute(
                HiveLeaderOrganSavedData.readOrCreate(player));
        float adapted = HiveLeaderOrganDamageTaken.apply(summary, event.getSource(), event.getAmount());
        adapted = HiveLeaderSpeciesManager.modifyIncomingDamage(player, event.getSource(), adapted);
        if (adapted != event.getAmount()) {
            event.setAmount(adapted);
        }
    }

    public static void clear(Player player) {
        HiveLeaderOrganEffects.clear(player);
        HiveLeaderSpeciesManager.clear(player);
        if (player == null) return;
        player.getPersistentData().remove(MIN_DAMAGE_KEY);
        // 旧版器官适应性的遗留 NBT（如果有）一并清掉，避免任何「残留状态」。
        player.getPersistentData().remove(LEGACY_ORGAN_ADAPTATION_KEY);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            org.tdddd.epca.impl.network.packet.c2s.HiveLeaderOrganTeleportHandler.clear(serverPlayer);
        }
    }

    private static void applyMinimumDamage(Player attacker, LivingEntity target, float amount) {
        Registry<DamageType> registry = attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder, attacker, attacker);
        target.hurt(minimumSource, amount);
    }
}
