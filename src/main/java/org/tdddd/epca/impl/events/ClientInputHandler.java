package org.tdddd.epca.impl.events;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.client.KeyInputHandlerClient;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.KeyPressPacket;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientInputHandler {
    @OnlyIn(Dist.CLIENT)
    private static boolean lastSpaceState = false;
    @OnlyIn(Dist.CLIENT)
    private static boolean lastShiftState = false;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        boolean currentSpaceState = KeyInputHandlerClient.FLIGHT_KEY.isDown();
        boolean currentShiftState = KeyInputHandlerClient.HOVER_KEY.isDown();

        if (currentSpaceState != lastSpaceState || currentShiftState != lastShiftState) {
            lastSpaceState = currentSpaceState;
            lastShiftState = currentShiftState;
            sendKeyPressPacket(currentSpaceState, currentShiftState);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onMouseInput(InputEvent.MouseButton event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        boolean currentSpaceState = KeyInputHandlerClient.FLIGHT_KEY.isDown();
        boolean currentShiftState = KeyInputHandlerClient.HOVER_KEY.isDown();

        if (currentSpaceState != lastSpaceState || currentShiftState != lastShiftState) {
            lastSpaceState = currentSpaceState;
            lastShiftState = currentShiftState;
            sendKeyPressPacket(currentSpaceState, currentShiftState);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        if (Minecraft.getInstance().player == null) {
            return;
        }

        if (Minecraft.getInstance().player.tickCount % 10 == 0) {
            boolean currentSpaceState = KeyInputHandlerClient.FLIGHT_KEY.isDown();
            boolean currentShiftState = KeyInputHandlerClient.HOVER_KEY.isDown();

            if (currentSpaceState || currentShiftState) {
                sendKeyPressPacket(currentSpaceState, currentShiftState);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void sendKeyPressPacket(boolean spacePressed, boolean shiftPressed) {
        ModNetwork.INSTANCE.sendToServer(new KeyPressPacket(
                Minecraft.getInstance().player.getUUID(),
                spacePressed,
                shiftPressed
        ));
    }
}