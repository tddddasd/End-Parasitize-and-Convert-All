package org.tdddd.epca.impl.client;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.tdddd.epca.impl.epca;


@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class ClientEntityTint {

    
    public static final ContextKey<Integer> MODEL_TINT =
            new ContextKey<>(Identifier.fromNamespaceAndPath(epca.MODID, "model_tint"));

    private ClientEntityTint() {
    }

    @SubscribeEvent
    public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, renderState) -> {
                    ClientColorEffect.EffectData effect = ClientColorEffect.getEffect(entity);
                    if (effect != null) {
                        renderState.setRenderData(MODEL_TINT, effect.getColorARGB());
                    }
                });
    }
}
