package org.tdddd.epca.impl.events;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.animation.layered.ModifierLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;

@EventBusSubscriber(modid = epca.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class SpearAttackHandler {

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        
        if (!(event.getEntity().level().isClientSide())) return;

        
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

        
        ItemStack mainHand = player.getMainHandItem();
        boolean isSpear = mainHand.getItem() == ModItems.WOODEN_SPEAR.get() ||
                mainHand.getItem() == ModItems.STONE_SPEAR.get() ||
                mainHand.getItem() == ModItems.FLINT_SPEAR.get() ||
                mainHand.getItem() == ModItems.COPPER_SPEAR.get() ||
                mainHand.getItem() == ModItems.IRON_SPEAR.get() ||
                mainHand.getItem() == ModItems.GOLDEN_SPEAR.get() ||
                mainHand.getItem() == ModItems.DIAMOND_SPEAR.get() ||
                mainHand.getItem() == ModItems.NETHERITE_SPEAR.get();

        
        boolean isLiving = event.getTarget() instanceof LivingEntity;

        
        if (isSpear && isLiving) {
            // Player Animation Library 1.2.6: the per-player layer is read back by the same id it was
            // registered under, and the animation id is triggered on the PlayerAnimationController the
            // factory put inside that layer. The library loads epca:stab itself from
            // assets/epca/player_animations/stab.animation.json, so no registry lookup is needed.
            IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(
                    player,
                    Identifier.fromNamespaceAndPath(epca.MODID, "stab"));

            if (layer instanceof ModifierLayer<?> modifierLayer
                    && modifierLayer.getAnimation() instanceof PlayerAnimationController controller) {
                controller.triggerAnimation(Identifier.fromNamespaceAndPath(epca.MODID, "stab"));
            }
        }
    }
}