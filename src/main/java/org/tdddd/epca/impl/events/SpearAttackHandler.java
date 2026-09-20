package org.tdddd.epca.impl.events;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class SpearAttackHandler {

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        
        if (!(event.getEntity().level().isClientSide)) return;

        
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
            
            ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                    .getPlayerAssociatedData(player)
                    .get(new ResourceLocation(epca.MODID, "stab"));

            if (animation != null) {
                
                var keyframe = PlayerAnimationRegistry.getAnimation(new ResourceLocation(epca.MODID, "stab"));
                if (keyframe != null) {
                    animation.setAnimation(new KeyframeAnimationPlayer(keyframe));
                }
            }
        }
    }
}