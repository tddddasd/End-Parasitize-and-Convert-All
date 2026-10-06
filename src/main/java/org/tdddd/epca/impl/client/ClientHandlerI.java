package org.tdddd.epca.impl.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.client.organ.NestLeaderOrganClientInput;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.KeyPacket;
import net.minecraft.client.Minecraft;
import org.tdddd.epca.impl.network.packet.c2s.ToggleFollowPacket;
import org.tdddd.epca.impl.network.packet.c2s.VKeyStatePacket;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientHandlerI {
    @SubscribeEvent
    public static void onKeyInput0(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            if (event.getKey() == GLFW.GLFW_KEY_V && event.getAction() == GLFW.GLFW_PRESS) {
                ModNetwork.sendToServer(new KeyPacket());
            }
            if (event.getKey() == GLFW.GLFW_KEY_V) {
                boolean pressed = event.getAction() != GLFW.GLFW_RELEASE;
                ModNetwork.sendToServer(new VKeyStatePacket(pressed));
            }
            if (event.getKey() == GLFW.GLFW_KEY_I && event.getAction() == GLFW.GLFW_PRESS) {
                ModNetwork.INSTANCE.sendToServer(new ToggleFollowPacket());
            }
        }
    }

    /**
     *  GUI SPEC B1/B2
     *
     * <p> {@code ClientTickEvent} + {@code KeyMapping#consumeClick()}
     *  {@code InputEvent.Key}  GLFW {@code consumeClick()} ""
     *  - {@code KeyMapping}
     *  {@code KeyboardHandler}  {@code KeyMapping.click(...)}
     *  tick ""</p>
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        NestLeaderOrganClientInput.tick();
    }
}

