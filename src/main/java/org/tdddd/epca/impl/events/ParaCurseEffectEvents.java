package org.tdddd.epca.impl.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@Mod.EventBusSubscriber
public class ParaCurseEffectEvents {
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {

        if (event.getEffectInstance().getEffect() != ModEffects.PARA_CURSE.get()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        MobEffectInstance newEffect = event.getEffectInstance();
        int newAmplifier = newEffect.getAmplifier();

        MobEffectInstance existingEffect = entity.getEffect(ModEffects.PARA_CURSE.get());

        if (existingEffect != null && newAmplifier <= existingEffect.getAmplifier()) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * 虫咒缚身 II 级及以上（amplifier &gt;= 1）无法被牛奶（curative item）解除。
     *
     * <p>只在 {@code LivingEntity#curePotionEffects} 这条线程内路径生效（{@code ParaCurseRemovalGuard}），
     * 所以 {@code /effect clear}、死亡清除、自然到期、代码里的 {@code removeEffect} 全都保持原样。</p>
     */
    @SubscribeEvent
    public static void onEffectRemovedByCurativeItem(MobEffectEvent.Remove event) {
        if (!org.tdddd.epca.impl.utils.ParaCurseRemovalGuard.blocksCurativeRemoval(event.getEffectInstance())) {
            return;
        }
        event.setCanceled(true);
        // 不发送任何 actionbar / 聊天提示：玩家通过"牛奶喝掉但效果仍在"自行感知
    }
}
