package org.tdddd.epca.impl.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.KeyPacket;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientHandlerI {
    @SubscribeEvent
    public static void onKeyInput0(InputEvent.Key event) {
        
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player != null && mc.level != null) {
            
            if (event.getKey() == GLFW.GLFW_KEY_V && event.getAction() == GLFW.GLFW_PRESS) {
                
                ModNetwork.sendToServer(new KeyPacket());
            }
        }
    }
}
