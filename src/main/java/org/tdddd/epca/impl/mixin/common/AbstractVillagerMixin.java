package org.tdddd.epca.impl.mixin.common;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.utils.MerchantOfferMixinAccess;

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
                
                var effectType = ModEffects.COTH.get();
                if (effectType != null) {
                    MobEffectInstance effect = new MobEffectInstance(effectType, 1200, 2); 
                    ((AbstractVillager) (Object) this).addEffect(effect);
                }
                
                access.epca$clearInfestedEmeraldFlag();
            }
        }
    }
}