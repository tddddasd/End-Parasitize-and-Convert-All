package org.tdddd.epca.impl.events;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EnderErosionOverlay implements IGuiOverlay {

    private static final ResourceLocation EROSION_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/gui/ender_erosion.png");

    private static final float MIN_BRIGHTNESS = 0.85f;
    private static final float MAX_BRIGHTNESS = 1.0f;
    private static final float CYCLE_SECONDS = 0.8f;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        MobEffectInstance effect = player.getEffect(ModEffects.ENDER_EROSION.get());
        if (effect == null) return;

        long now = System.currentTimeMillis();
        float progress = (now % (long)(CYCLE_SECONDS * 1000)) / (CYCLE_SECONDS * 1000f);
        float brightness = (float) (MIN_BRIGHTNESS + (MAX_BRIGHTNESS - MIN_BRIGHTNESS) *
                (0.5 + 0.5 * Math.cos(2 * Math.PI * progress)));

        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0f);

        guiGraphics.blit(EROSION_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.9f);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("ender_erosion", new EnderErosionOverlay());
    }
}