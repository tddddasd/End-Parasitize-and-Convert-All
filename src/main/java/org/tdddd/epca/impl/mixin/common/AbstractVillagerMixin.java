package org.tdddd.epca.impl.mixin.common;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.utils.MerchantOfferMixinAccess;

/**
 * 村民交易后：如果这笔交易用过虫染绿宝石，就给村民挂 COTH。
 *
 * <p><b>26.1.2 改动</b>：{@code notifyTrade(MerchantOffer)} 签名不变。
 * {@code new MobEffectInstance(MobEffect, ...)} → {@code MobEffectInstance(Holder<MobEffect>, ...)}：
 * {@code ModEffects.COTH} 本身就是 {@code DeferredHolder<MobEffect, MobEffect>}
 * （即 {@code Holder<MobEffect>}），直接传即可，原来的 {@code .get()} 与空值判断都去掉了
 * （{@code DeferredHolder} 在模组加载完成后一定已绑定，容器本身不可能为 null）。
 */
@Mixin(AbstractVillager.class)
public class AbstractVillagerMixin {

    @Inject(
            method = "notifyTrade",
            at = @At("HEAD")
    )
    private void onNotifyTrade(MerchantOffer offer, CallbackInfo ci) {
        
        if (offer instanceof MerchantOfferMixinAccess) { 
            MerchantOfferMixinAccess access = (MerchantOfferMixinAccess) offer;
            if (access.epca$hasUsedInfestedEmerald()) {
                
                MobEffectInstance effect = new MobEffectInstance(ModEffects.COTH, 1200, 2); 
                ((AbstractVillager) (Object) this).addEffect(effect);
                
                access.epca$clearInfestedEmeraldFlag();
            }
        }
    }
}
