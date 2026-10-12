package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.FearEffect;

/**
 * 恐慌（epca:fear）在非玩家生物身上的攻击效果：
 * 生物攻击时固定 15% 概率打空，不造成任何伤害，但仍然保留仇恨。
 *
 * <p><b>为什么挂在 LivingIncomingDamageEvent 而不是别的钩子</b></p>
 * <ul>
 *   <li>1.20.1 的钩子是 {@code LivingHurtEvent}，26.1.2 对应名为
 *       {@code net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent}
 *       （同一棵树里 ViralEffectEventHandler / ShieldEventHandler / LivingHurtEventHandler 都是这样对应的）。</li>
 *   <li>该事件在 {@code LivingEntity#hurtServer} 的后段触发：此时被打者的
 *       {@code setLastHurtByMob} / 仇恨时间戳已经写完，取消它只跳过伤害结算，不碰仇恨。</li>
 * </ul>
 *
 * <p>这里再补一次 {@code setLastHurtByMob(victim)}，不依赖 {@code Mob#doHurtTarget} 末尾那句
 * 受返回值保护的赋值。<b>不使用 {@code mob.setTarget(victim)}</b>，以免覆盖生物自己的目标。</p>
 */
@EventBusSubscriber(modid = epca.MODID)
public class PanicEffectEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        
        if (attacker instanceof Player) return;

        // 26.1.2: DeferredHolder<MobEffect, MobEffect> is passed straight to hasEffect/getEffect
        // (ModEffects.FEAR, no .get()); LivingEntity#getEffect takes a Holder<MobEffect>.
        MobEffectInstance panic = attacker.getEffect(ModEffects.FEAR);
        if (panic == null) return;
        if (panic.getAmplifier() < 0) return;

        // 26.1.2: DamageSource#isIndirect() is gone; the same test is written as !isDirect()
        // (AfterimageModuleEventHandler.java:161).
        if (!event.getSource().isDirect()) return;

        LivingEntity victim = event.getEntity();

        if (FearEffect.rollPanicMiss(attacker)) {
            
            event.setCanceled(true);

            
            if (attacker instanceof Mob mob && mob != victim) {
                mob.setLastHurtByMob(victim);
            }
        }
    }
}
