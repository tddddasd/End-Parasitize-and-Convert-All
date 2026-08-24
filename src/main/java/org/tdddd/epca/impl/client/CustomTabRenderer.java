package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.items.CustomTab;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class CustomTabRenderer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof CreativeModeInventoryScreen creativeScreen)) {
            return;
        }

        
        CreativeModeTab selectedTab = CreativeModeInventoryScreen.selectedTab;
        if (!(selectedTab instanceof CustomTab customTab)) {
            return;
        }

        var graphics = event.getGuiGraphics();
        int mouseX = (int) mc.mouseHandler.xpos();
        int mouseY = (int) mc.mouseHandler.ypos();
        float partialTick = event.getPartialTick();

        int leftPos = creativeScreen.width;
        int topPos = creativeScreen.height;

        int slotSize = 18;
        int startX = leftPos + 8;
        int startY = topPos + 8;

        for (var entry : customTab.renderedEntries.entrySet()) {
            int slotIndex = entry.getKey();
            CustomTab.ITabEntry tabEntry = entry.getValue();
            if (tabEntry.hasSpecialRendering()) {
                int col = slotIndex % 9;
                int row = slotIndex / 9;
                int x = startX + col * slotSize;
                int y = startY + row * slotSize;
                tabEntry.render(graphics, x, y, mouseX, mouseY, partialTick);
            }
        }
    }
}