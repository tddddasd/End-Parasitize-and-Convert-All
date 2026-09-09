package org.tdddd.epca.impl.overworld.registry.gui.menus;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.BiomassClientData;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;

@Mod.EventBusSubscriber(modid = epca.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BiomassHUD {
    private static final ResourceLocation ICON = new ResourceLocation(epca.MODID, "textures/item/biomass_count_icon.png");
    private static final int BECKON_COST = 15;
    private static final long BECKON_COOLDOWN_TICKS = 60 * 20;
    private static final long PARASITE_COOLDOWN_TICKS = 10 * 20;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!BiomassClientData.isNestLeader()) return;

        int points = BiomassClientData.getPoints();
        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int foodX = screenWidth / 2 + 91;
        int foodY = screenHeight - 39;

        int iconX = foodX + 81;
        int iconY = foodY;

        RenderSystem.setShaderTexture(0, ICON);
        gui.blit(ICON, iconX, iconY, 0, 0, 9, 9, 9, 9);
        String pointsText = String.valueOf(points);
        gui.drawString(mc.font, pointsText, iconX + 12, iconY + 1, 0xFFFFFF);

        long currentTick = mc.player.level().getGameTime();
        long beckonCooldownTick = mc.player.getPersistentData().getLong("BeckonPlaceCooldown");
        long beckonRemaining = Math.max(0, (beckonCooldownTick + BECKON_COOLDOWN_TICKS - currentTick));
        long beckonSeconds = beckonRemaining / 20;

        long parasiteCooldownTick = mc.player.getPersistentData().getLong("LastParasiteInteract");
        long parasiteRemaining = Math.max(0, (parasiteCooldownTick + PARASITE_COOLDOWN_TICKS - currentTick));
        long parasiteSeconds = parasiteRemaining / 20;

        int beckonX = iconX;
        int beckonY = iconY - 18;
        ItemStack blockStack = new ItemStack(ModBlocks.BECKON_CORE.get());
        gui.renderItem(blockStack, beckonX, beckonY);

        boolean hasEnoughPoints = points >= BECKON_COST;
        boolean isOnCooldown = beckonRemaining > 0;

        if (!hasEnoughPoints || isOnCooldown) {
            gui.fill(beckonX, beckonY, beckonX + 16, beckonY + 16, 0x88000000);
        }

        if (hasEnoughPoints && isOnCooldown) {
            String cdText = String.valueOf(beckonSeconds);
            gui.drawString(mc.font, cdText, beckonX + 18, beckonY + 4, 0xFFFFFF);
        }

        int parasiteX = iconX + 12 + mc.font.width(pointsText) + 4;
        int parasiteY = iconY + 1;
        String parasiteText = String.valueOf(parasiteSeconds);
        gui.drawString(mc.font, parasiteText, parasiteX, parasiteY, 0xAA00FF);
    }
}