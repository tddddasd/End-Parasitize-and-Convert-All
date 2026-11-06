package org.tdddd.epca.impl.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.client.effect.BioTortClientState;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HotbarMarkOverlay implements IGuiOverlay {

    private static final int BOX_SIDE = 16;

    private static final float BOX_DIAGONAL = (float) (BOX_SIDE * Math.sqrt(2.0D));

    private static final int HOTBAR_LEFT = -91;

    private static final int SLOT_PITCH = 20;

    private static final int ICON_INSET = 3;

    private static final int BOX_OUTSET = 1;

    private static final int CORE_COLOR = 0xFFC060FF;

    private static final int GLOW_COLOR = 0xFF7A1FD0;

    private static final float GLOW_ALPHA = 0.35F;

    private static final int GLOW_WIDTH = 3;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!BioTortClientState.hasHotbarMark()) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            return;
        }

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        float progress = BioTortClientState.markAnimationProgress();
        int left = screenWidth / 2 + HOTBAR_LEFT;

        int top = screenHeight - 19 - BOX_OUTSET;

        for (int slot = 0; slot <= 8; slot++) {
            if (!BioTortClientState.isSlotMarked(slot)) {
                continue;
            }
            int iconX = left + slot * SLOT_PITCH + ICON_INSET;
            drawCross(guiGraphics, iconX - BOX_OUTSET, top, progress);
        }
    }

    private static void drawCross(GuiGraphics guiGraphics, int x, int y, float progress) {

        float firstStroke = Mth.clamp(progress / 0.5F, 0.0F, 1.0F);

        float secondStroke = Mth.clamp((progress - 0.5F) / 0.5F, 0.0F, 1.0F);

        drawStroke(guiGraphics, x, y, 45.0F, firstStroke);
        drawStroke(guiGraphics, x, y + BOX_SIDE, -45.0F, secondStroke);
    }

    private static void drawStroke(GuiGraphics guiGraphics, int x, int y, float degrees, float lengthFactor) {
        if (lengthFactor <= 0.0F) {
            return;
        }
        int lengthPx = Math.round(BOX_DIAGONAL * lengthFactor);
        if (lengthPx < 1) {
            return;
        }

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));

        guiGraphics.fill(-1, -1, lengthPx + 1, GLOW_WIDTH, withAlphaScale(GLOW_COLOR, GLOW_ALPHA));

        guiGraphics.fill(0, 0, lengthPx, 2, CORE_COLOR);

        pose.popPose();
    }

    private static int withAlphaScale(int argb, float scale) {
        int alpha = (argb >>> 24) & 0xFF;
        int scaled = Mth.clamp(Math.round(alpha * scale), 0, 255);
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {

        event.registerAboveAll("bio_tort_hotbar_marks", new HotbarMarkOverlay());
    }
}
