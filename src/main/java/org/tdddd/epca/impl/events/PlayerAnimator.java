package org.tdddd.epca.impl.events;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.playeranimator.FirstPersonModifier;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PlayerAnimator {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                new ResourceLocation(epca.MODID, "stab"),
                50,
                PlayerAnimator::registerPlayerAnimation);

        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                new ResourceLocation(epca.MODID, "kill_stick"),
                50,
                PlayerAnimator::registerPlayerAnimation);
    }

    private static IAnimation registerPlayerAnimation(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = new ModifierLayer<>();

        FirstPersonConfiguration config = new FirstPersonConfiguration()
                .setShowRightArm(true)
                .setShowLeftArm(true)
                .setShowRightItem(true)
                .setShowLeftItem(true);

        layer.addModifierBefore(new FirstPersonModifier(
                FirstPersonMode.THIRD_PERSON_MODEL,
                config
        ));

        return layer;
    }
}
