package org.tdddd.epca.impl.overworld.registry.gui.menus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.BiomassClientData;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.data.NestLeaderClientCache;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;

import java.util.List;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = epca.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class BiomassHUD {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(epca.MODID, "textures/item/biomass_count_icon.png");
    private static final String COMPASS_BASE = "minecraft:textures/item/recovery_compass_";
    private static final int BECKON_COST = 15;
    private static final long BECKON_COOLDOWN_TICKS = 60 * 20;
    private static final long PARASITE_COOLDOWN_TICKS = 10 * 20;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!BiomassClientData.isNestLeader()) return;

        int points = BiomassClientData.getPoints();
        GuiGraphicsExtractor gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int foodX = screenWidth / 2 + 91;
        int foodY = screenHeight - 39;

        int iconX = foodX + 81;
        int iconY = foodY;

        // 26.1.2: GuiGraphics#blit(Identifier, x, y, u, v, w, h, texW, texH) gained a leading RenderPipeline and
        // the texture is bound by the extraction, so RenderSystem#setShaderTexture(...) is gone.
        gui.blit(RenderPipelines.GUI_TEXTURED, ICON, iconX, iconY, 0.0F, 0.0F, 9, 9, 9, 9);
        String pointsText = String.valueOf(points);
        gui.text(mc.font, pointsText, iconX + 12, iconY + 1, 0xFFFFFF);

        // 26.1.2: CompoundTag#getLong returns Optional<Long>; getLongOr keeps the 1.20.1 default of 0.
        long currentTick = mc.player.level().getGameTime();
        long beckonCooldownTick = mc.player.getPersistentData().getLongOr("BeckonPlaceCooldown", 0L);
        long beckonRemaining = Math.max(0, (beckonCooldownTick + BECKON_COOLDOWN_TICKS - currentTick));
        long beckonSeconds = beckonRemaining / 20;

        long parasiteCooldownTick = mc.player.getPersistentData().getLongOr("LastParasiteInteract", 0L);
        long parasiteRemaining = Math.max(0, (parasiteCooldownTick + PARASITE_COOLDOWN_TICKS - currentTick));
        long parasiteSeconds = parasiteRemaining / 20;

        int beckonX = iconX;
        int beckonY = iconY - 18;
        ItemStack blockStack = new ItemStack(ModBlocks.BECKON_CORE.get());
        gui.item(blockStack, beckonX, beckonY);

        boolean hasEnoughPoints = points >= BECKON_COST;
        boolean isOnCooldown = beckonRemaining > 0;

        if (!hasEnoughPoints || isOnCooldown) {
            gui.fill(beckonX, beckonY, beckonX + 16, beckonY + 16, 0x88000000);
        }

        if (hasEnoughPoints && isOnCooldown) {
            String cdText = String.valueOf(beckonSeconds);
            gui.text(mc.font, cdText, beckonX + 18, beckonY + 4, 0xFFFFFF);
        }

        int parasiteX = iconX + 12 + mc.font.width(pointsText) + 4;
        int parasiteY = iconY + 1;
        String parasiteText = String.valueOf(parasiteSeconds);
        gui.text(mc.font, parasiteText, parasiteX, parasiteY, 0xAA00FF);

        if (mc.getConnection() != null && mc.getConnection().getOnlinePlayers().size() > 1) {
            renderTracker(gui, mc, beckonX, beckonY);
        }
    }

    private static void renderTracker(GuiGraphicsExtractor gui, Minecraft mc, int baseX, int baseY) {
        Player player = mc.player;
        if (player == null) return;

        List<Player> others = mc.level.players().stream()
                .filter(p -> p != player)
                .filter(p -> !NestLeaderClientCache.isNestLeader(p.getUUID()))
                .collect(Collectors.toList());
        if (others.isEmpty()) return;

        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player other : others) {
            double d = player.distanceToSqr(other);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = other;
            }
        }
        if (nearest == null) return;

        double dist = Math.sqrt(nearestDist);
        boolean within128 = dist <= (128 * (6 + EvolutionManager.getStageForDimension(player.level())));

        double dx = nearest.getX() - player.getX();
        double dz = nearest.getZ() - player.getZ();

        double targetAngleRad = Math.atan2(dx, dz);
        float targetAngleDeg = (float) Math.toDegrees(targetAngleRad);
        float targetClockwise = (360 - targetAngleDeg) % 360;

        float playerYaw = player.getYRot();
        playerYaw = ((playerYaw % 360) + 360) % 360;

        float relativeAngle = targetClockwise - playerYaw;
        relativeAngle = ((relativeAngle % 360) + 360) % 360;

        int frame = (int)(relativeAngle / 360.0 * 32) % 32;
        frame = (32 - frame + 16) % 32;
        int trackerX = baseX + 20;
        int trackerY = baseY;

        // 26.1.2: GuiGraphics#pose() returns a Matrix3x2fStack (pushMatrix/popMatrix, 2D transforms only).
        var pose = gui.pose();
        pose.pushMatrix();
        pose.translate(trackerX, trackerY);

        String path = COMPASS_BASE + String.format("%02d", frame) + ".png";
        Identifier compassTexture = Identifier.parse(path);
        gui.blit(RenderPipelines.GUI_TEXTURED, compassTexture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);

        pose.popMatrix();

        if (within128) {
            long time = System.currentTimeMillis() / 100;
            float hue = (time % 100) / 100.0f;
            int color = java.awt.Color.HSBtoRGB(hue, 0.8f, 0.8f);
            int alpha = 80 + (int)(60 * Math.sin(System.currentTimeMillis() / 200.0));
            int colorWithAlpha = (alpha << 24) | (color & 0x00ffffff);

            pose.pushMatrix();
            pose.translate(trackerX, trackerY);
            gui.fill(trackerX, trackerY, trackerX + 16, trackerY + 16, colorWithAlpha);
            pose.popMatrix();
        }
    }
}