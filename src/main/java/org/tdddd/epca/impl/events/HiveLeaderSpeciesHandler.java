package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.organ.species.HiveLeaderSpeciesManager;

/**
 * 种阶特化的事件入口：
 *
 * <ul>
 *   <li>伤害免疫（墙内窒息 / 水中窒息；重塑种模型显示时的摔落伤害）；</li>
 *   <li>击杀累计（进化重塑种）与粗制种死亡爆体；</li>
 *   <li>登录 / 登出 / 换维度 / 重生时的状态同步与属性修饰符清理。</li>
 * </ul>
 *
 * <p>Forge 1.20.1 没有 {@code LivingIncomingDamageEvent}，所以用更早、可取消的
 * {@link LivingAttackEvent} 来完全免疫伤害。</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HiveLeaderSpeciesHandler {

    private HiveLeaderSpeciesHandler() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        DamageSource source = event.getSource();
        if (HiveLeaderSpeciesManager.shouldCancelDamage(player, source)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // 粗制种死亡：假死爆体 + 原地诅虫。
        if (event.getEntity() instanceof ServerPlayer player) {
            HiveLeaderSpeciesManager.onLeaderDeath(player);
        }

        // 领巢者击杀生物：累计进化击杀数。
        if (event.getSource().getEntity() instanceof Player killer) {
            LivingEntity killed = event.getEntity();
            if (killed != killer) {
                HiveLeaderSpeciesManager.onKill(killer, killed);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HiveLeaderSpeciesManager.refresh(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HiveLeaderSpeciesManager.clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HiveLeaderSpeciesManager.refresh(player);
        }
    }

    /**
     * 重生：Forge 的 {@code ServerPlayer#restoreFrom} 只把 {@code PlayerPersisted} 子标签搬到新实体，
     * 玩家 ForgeData 里的种阶 / 适应进度不会自动过去。这里立刻 refresh 一次，
     * 把缓存里的状态<strong>重新写回新实体</strong>并同步客户端，避免「死亡 → 退出 → 重登」丢进度，
     * 也保证新实体上不会残留旧的属性修饰符（refresh 会重新收敛修饰符）。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HiveLeaderSpeciesManager.refresh(player);
        }
    }
}
