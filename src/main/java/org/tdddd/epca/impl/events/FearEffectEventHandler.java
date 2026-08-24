package org.tdddd.epca.impl.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.FearEffect;

import java.util.Set;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FearEffectEventHandler {
    private static final Set<ResourceLocation> THROWABLE_ITEMS = Set.of(
            new ResourceLocation("minecraft", "snowball"),
            new ResourceLocation("minecraft", "ender_pearl"),
            new ResourceLocation("minecraft", "egg"),
            new ResourceLocation("minecraft", "experience_bottle"),
            new ResourceLocation("minecraft", "splash_potion"),
            new ResourceLocation("minecraft", "lingering_potion")
    );

    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        
        if (!player.hasEffect(ModEffects.FEAR.get())) return;

        int amplifier = player.getEffect(ModEffects.FEAR.get()).getAmplifier();
        if (FearEffect.shouldPreventBlockPlacement(amplifier)) {
            player.displayClientMessage(FearEffect.FEAR_MESSAGE, true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        
        if (!player.hasEffect(ModEffects.FEAR.get())) return;

        
        ItemStack stack = player.getItemInHand(event.getHand());
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());

        
        if (itemId != null && (THROWABLE_ITEMS.contains(itemId) || stack.getItem() instanceof ProjectileWeaponItem)) {
            return;
        }

        int amplifier = player.getEffect(ModEffects.FEAR.get()).getAmplifier();
        if (FearEffect.shouldPreventItemUse(amplifier)) {
            player.displayClientMessage(FearEffect.FEAR_MESSAGE, true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}