package org.tdddd.epca.impl.events;

import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyInputHandler {
    
    private static final Map<UUID, Boolean> spacePressedMap = new HashMap<>();
    private static final Map<UUID, Boolean> shiftPressedMap = new HashMap<>();

    
    public static void updateKeyState(UUID playerId, boolean spacePressed, boolean shiftPressed) {
        spacePressedMap.put(playerId, spacePressed);
        shiftPressedMap.put(playerId, shiftPressed);
    }

    
    public static boolean isSpacePressed(UUID playerId) {
        return spacePressedMap.getOrDefault(playerId, false);
    }

    
    public static boolean isShiftPressed(UUID playerId) {
        return shiftPressedMap.getOrDefault(playerId, false);
    }

    
    public static void removePlayer(UUID playerId) {
        spacePressedMap.remove(playerId);
        shiftPressedMap.remove(playerId);
    }
}