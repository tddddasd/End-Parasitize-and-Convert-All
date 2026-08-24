package org.tdddd.epca.impl.utils;

import net.minecraft.client.Minecraft;
import org.tdddd.epca.impl.overworld.registry.gui.menus.EPCANoteScreen;

public class ClientScreenUtil {
    public static void openNoteScreen() {
        
        Minecraft.getInstance().setScreen(new EPCANoteScreen());
    }
}