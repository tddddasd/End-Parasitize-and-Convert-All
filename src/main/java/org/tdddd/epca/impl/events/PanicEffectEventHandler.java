package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.FearEffect;

/**
 * 恐慌（epca:fear）在非玩家生物身上的攻击效果：
 * 生物攻击时固定 15% 概率打空，不造成任何伤害，但仍然保留仇恨。
 *
 * <p><b>为什么挂在 LivingHurtEvent 而不是 LivingAttackEvent</b></p>
 * <ul>
 *   <li>{@code LivingAttackEvent} 在 {@code LivingEntity#hurt} 的 HEAD 触发，早于
 *       {@code this.setLastHurtByMob(attacker)} 这段仇恨记账；在那里取消会让被打者完全
 *       “没被打过”，仇恨/反击目标会丢。</li>
 *   <li>{@code LivingHurtEvent} 在 {@code LivingEntity#hurt} 的末尾触发：此时
 *       {@code setLastHurtByMob}、{@code setLastHurtByMobTimestamp}、{@code hurtTime}、
 *       {@code hurtDuration} 都已经写完，取消它只跳过后面的 {@code actuallyHurt}，
 *       也就是只跳过伤害结算，不碰仇恨。</li>
 * </ul>
 *
 * <p>另外这里再补一次 {@code setLastHurtByMob(victim)}：{@code Mob#doHurtTarget} 末尾那一句
 * 只在被攻击者 {@code hurt(...)} 返回 true 时才执行（1.20.1 中 {@code amount > 0} 即返回 true），
 * 为了不依赖那个返回值，显式写一次。<b>不使用 {@code mob.setTarget(victim)}</b>，以免覆盖
 * 生物自己的目标（例如正在追别人时被第三方打一下）。</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PanicEffectEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        
        if (attacker instanceof Player) return;

        
        MobEffectInstance panic = attacker.getEffect(ModEffects.FEAR.get());
        if (panic == null) return;
        if (panic.getAmplifier() < 0) return;

        
        if (event.getSource().isIndirect()) return;

        LivingEntity victim = event.getEntity();

        if (FearEffect.rollPanicMiss(attacker)) {
            
            event.setCanceled(true);

            
            if (attacker instanceof Mob mob && mob != victim) {
                mob.setLastHurtByMob(victim);
            }
        }
    }
}
