package org.tdddd.epca.impl.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class EnderErosionOverlay implements GuiLayer {

    private static final Identifier EROSION_TEXTURE =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/gui/ender_erosion.png");

    private static final float MIN_BRIGHTNESS = 0.85f;
    private static final float MAX_BRIGHTNESS = 1.0f;
    private static final float CYCLE_SECONDS = 0.8f;

    /**
     * 26.1.2: the Forge {@code IGuiOverlay#render(ForgeGui, GuiGraphics, partialTick, screenWidth, screenHeight)}
     * signature became {@code GuiLayer#render(GuiGraphicsExtractor, DeltaTracker)}. The screen size now comes from
     * {@code GuiGraphicsExtractor#guiWidth()/guiHeight()}; {@code RenderSystem#setShaderColor} is gone, so the
     * brightness tint is folded into the blit colour (the old code also left the shader colour at
     * (1,1,1,0.9) afterwards -> tint colour * 0.9 alpha).
     */
    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // 26.1.2: LivingEntity#getEffect takes a Holder<MobEffect>. DeferredHolder#get() yields the raw value,
        // so the holder view has to be requested explicitly through getDelegate().
        MobEffectInstance effect = player.getEffect(ModEffects.ENDER_EROSION.getDelegate());
        if (effect == null) return;

        long now = System.currentTimeMillis();
        float progress = (now % (long)(CYCLE_SECONDS * 1000)) / (CYCLE_SECONDS * 1000f);
        float brightness = (float) (MIN_BRIGHTNESS + (MAX_BRIGHTNESS - MIN_BRIGHTNESS) *
                (0.5 + 0.5 * Math.cos(2 * Math.PI * progress)));

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, EROSION_TEXTURE, 0, 0, 0.0F, 0.0F,
                screenWidth, screenHeight, screenWidth, screenHeight,
                ARGB.colorFromFloat(0.9f, brightness, brightness, brightness));
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        // 26.1.2: layer ids are Identifiers instead of plain strings.
        event.registerBelowAll(Identifier.fromNamespaceAndPath(epca.MODID, "ender_erosion"), new EnderErosionOverlay());
    }
}