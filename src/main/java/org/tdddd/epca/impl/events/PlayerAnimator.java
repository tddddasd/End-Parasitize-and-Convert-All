package org.tdddd.epca.impl.events;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.animation.layered.ModifierLayer;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.playeranimator.FirstPersonModifier;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class PlayerAnimator {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        // Player Animation Library 1.2.6: the factory is still keyed by the layer id, but it is now
        // called with the player as a net.minecraft.world.entity.Avatar (AbstractClientPlayer extends
        // Player extends Avatar), so the factory no longer takes an AbstractClientPlayer.
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                Identifier.fromNamespaceAndPath(epca.MODID, "stab"),
                50,
                PlayerAnimator::registerPlayerAnimation);

        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                Identifier.fromNamespaceAndPath(epca.MODID, "kill_stick"),
                50,
                PlayerAnimator::registerPlayerAnimation);
    }

    private static IAnimation registerPlayerAnimation(Avatar player) {
        // epca keeps its own layer so FirstPersonModifier can force the first person mode of the
        // animations this layer plays. The state handler returns PlayState.STOP, which is the new API
        // way of saying "this layer only ever plays animations that were triggered by id".
        PlayerAnimationController controller = new PlayerAnimationController(
                player,
                (animationController, data, setter) -> PlayState.STOP);

        ModifierLayer<IAnimation> layer = new ModifierLayer<>();
        layer.setAnimation(controller);

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
