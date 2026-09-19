package org.tdddd.epca.impl.events;

import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.FearEffect;

import java.util.Set;

@EventBusSubscriber(modid = epca.MODID)
public class FearEffectEventHandler {
    private static final Set<Identifier> THROWABLE_ITEMS = Set.of(
            Identifier.fromNamespaceAndPath("minecraft", "snowball"),
            Identifier.fromNamespaceAndPath("minecraft", "ender_pearl"),
            Identifier.fromNamespaceAndPath("minecraft", "egg"),
            Identifier.fromNamespaceAndPath("minecraft", "experience_bottle"),
            Identifier.fromNamespaceAndPath("minecraft", "splash_potion"),
            Identifier.fromNamespaceAndPath("minecraft", "lingering_potion")
    );

    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        
        if (!player.hasEffect(ModEffects.FEAR)) return;

        int amplifier = player.getEffect(ModEffects.FEAR).getAmplifier();
        if (FearEffect.shouldPreventBlockPlacement(amplifier)) {
            player.sendOverlayMessage(FearEffect.FEAR_MESSAGE);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        
        if (!player.hasEffect(ModEffects.FEAR)) return;

        
        ItemStack stack = player.getItemInHand(event.getHand());
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        
        if (itemId != null && (THROWABLE_ITEMS.contains(itemId) || stack.getItem() instanceof ProjectileWeaponItem)) {
            return;
        }

        int amplifier = player.getEffect(ModEffects.FEAR).getAmplifier();
        if (FearEffect.shouldPreventItemUse(amplifier)) {
            player.sendOverlayMessage(FearEffect.FEAR_MESSAGE);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}