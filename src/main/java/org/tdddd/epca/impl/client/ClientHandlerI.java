package org.tdddd.epca.impl.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.packet.c2s.KeyPacket;
import net.minecraft.client.Minecraft;
import org.tdddd.epca.impl.network.packet.c2s.ToggleFollowPacket;
import org.tdddd.epca.impl.network.packet.c2s.VKeyStatePacket;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientHandlerI {
    @SubscribeEvent
    public static void onKeyInput0(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            // 26.1.2: SimpleChannel/ModNetwork.INSTANCE are gone; serverbound payloads go
            // through the client-only ClientPacketDistributor (see ModNetwork's javadoc).
            if (event.getKey() == GLFW.GLFW_KEY_V && event.getAction() == GLFW.GLFW_PRESS) {
                ClientPacketDistributor.sendToServer(new KeyPacket());
            }
            if (event.getKey() == GLFW.GLFW_KEY_V) {
                boolean pressed = event.getAction() != GLFW.GLFW_RELEASE;
                ClientPacketDistributor.sendToServer(new VKeyStatePacket(pressed));
            }
            if (event.getKey() == GLFW.GLFW_KEY_I && event.getAction() == GLFW.GLFW_PRESS) {
                ClientPacketDistributor.sendToServer(new ToggleFollowPacket());
            }
        }
    }
}
