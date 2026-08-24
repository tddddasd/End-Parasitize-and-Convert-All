package org.tdddd.epca.impl.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "epca", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyInputHandlerClient {
    @OnlyIn(Dist.CLIENT)
    public static KeyMapping FLIGHT_KEY;
    @OnlyIn(Dist.CLIENT)
    public static KeyMapping HOVER_KEY;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        FLIGHT_KEY = new KeyMapping(
                "key.epca.flight",
                GLFW.GLFW_KEY_SPACE,
                "key.categories.epca"
        );
        HOVER_KEY = new KeyMapping(
                "key.epca.hover",
                GLFW.GLFW_KEY_LEFT_SHIFT,
                "key.categories.epca"
        );
        event.register(FLIGHT_KEY);
        event.register(HOVER_KEY);
    }
}