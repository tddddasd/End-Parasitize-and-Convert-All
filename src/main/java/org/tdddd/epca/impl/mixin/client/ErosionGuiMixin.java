package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public abstract class ErosionGuiMixin {

    @Unique
    private static final ResourceLocation EROSION_HEARTS = new ResourceLocation("epca", "textures/gui/ender_erosion_hearts.png");

    @Unique
    private static Map<String, Object> heartTypeCache = new HashMap<>();

    @Unique
    private static Object getHeartType(String name) {
        if (heartTypeCache.containsKey(name)) {
            return heartTypeCache.get(name);
        }

        try {
            Class<?> heartTypeClass = Class.forName("net.minecraft.client.gui.Gui$HeartType");
            Field[] fields = heartTypeClass.getDeclaredFields();

            for (Field field : fields) {
                if (field.isEnumConstant() && field.getName().equals(name)) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    heartTypeCache.put(name, value);
                    return value;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Unique
    private static boolean hasErosionEffect(Player player) {
        for (MobEffectInstance effect : player.getActiveEffects()) {
            String effectId = effect.getEffect().getDescriptionId();
            if (effectId.contains("ender_erosion") || effectId.contains("EnderErosion")) {
                return true;
            }
        }
        return false;
    }

    @Inject(
            method = "renderHearts",
            at = @At("HEAD")
    )
    private void onRenderHeartsPre(GuiGraphics guiGraphics, Player player, int x, int y,
                                   int height, int renderHealth, float healthMax,
                                   int healthLast, int absorption, int regenTickCounter, boolean highlight,
                                   CallbackInfo ci) {

        
        if (hasErosionEffect(player)) {
            
            savePlayerRenderInfo(player, x, y, regenTickCounter, absorption);
        }
    }

    @Inject(
            method = "renderHearts",
            at = @At("TAIL")
    )
    private void onRenderHeartsPost(GuiGraphics guiGraphics, Player player, int x, int y,
                                    int height, int renderHealth, float healthMax,
                                    int healthLast, int absorption, int regenTickCounter, boolean highlight,
                                    CallbackInfo ci) {

        if (!hasErosionEffect(player)) {
            return; 
        }

        
        
        renderErosionOverlay(guiGraphics, player, x, y, height, renderHealth,
                healthMax, absorption, regenTickCounter, highlight);
    }

    @Unique
    private void savePlayerRenderInfo(Player player, int x, int y, int regenTickCounter, int absorption) {
        
        
    }

    @Unique
    private void renderErosionOverlay(GuiGraphics guiGraphics, Player player, int x, int y,
                                      int height, int renderHealth, float healthMax,
                                      int absorption, int regenTickCounter, boolean highlight) {

        
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorptionAmount = player.getAbsorptionAmount();
        boolean isHardcore = player.level().getLevelData().isHardcore();

        
        int currentHealth = (int) Math.ceil(health);
        int maxHearts = (int) Math.ceil(maxHealth / 2.0F);
        int absorptionHearts = (int) Math.ceil(absorptionAmount / 2.0F);
        int totalHearts = maxHearts + absorptionHearts;
        int totalRows = (int) Math.ceil(totalHearts / 10.0);

        
        int healthRows = (int) Math.ceil(maxHearts / 10.0);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        
        
        int adjustedRowHeight = rowHeight;
        if (totalRows > 1) {
            adjustedRowHeight = Math.max(rowHeight - 1, 3);
        }

        
        
        for (int row = 0; row < totalRows; row++) {
            int rowTop = y;
            if (row > 0) {
                int heightReduction = row;
                rowTop = y - rowHeight - (row - 1) * adjustedRowHeight + heightReduction;
            }

            int heartsInThisRow = (row == totalRows - 1) ?
                    (totalHearts % 10 == 0 ? 10 : totalHearts % 10) : 10;

            for (int heart = 0; heart < heartsInThisRow; heart++) {
                int heartIndex = row * 10 + heart;
                int heartX = x + heart * 8;
                int heartY = rowTop;

                
                boolean isAbsorption = heartIndex >= maxHearts;

                if (isAbsorption) {
                    
                    int absorptionIndex = heartIndex - maxHearts;
                    float remainingAbsorption = absorptionAmount - (absorptionIndex * 2);

                    
                    if (remainingAbsorption > 0.0F) {
                        boolean halfHeart = remainingAbsorption <= 1.0F;

                        
                        if (currentHealth + (int)absorptionAmount <= 4) {
                            heartY += player.level().random.nextInt(2);
                        }

                        
                        renderErosionHeart(guiGraphics, heartX, heartY, true, isHardcore, halfHeart);
                    }
                } else {
                    
                    
                    boolean renderHeart = false;
                    boolean halfHeart = false;

                    
                    if (heartIndex * 2 + 1 < currentHealth) {
                        
                        renderHeart = true;
                        halfHeart = false;
                    } else if (heartIndex * 2 + 1 == currentHealth) {
                        
                        renderHeart = true;
                        halfHeart = true;
                    } else if (health > heartIndex * 2 && health < heartIndex * 2 + 1) {
                        
                        renderHeart = true;
                        halfHeart = true;
                    }

                    if (renderHeart) {
                        
                        if (currentHealth + (int)absorptionAmount <= 4) {
                            heartY += player.level().random.nextInt(2);
                        }

                        
                        renderErosionHeart(guiGraphics, heartX, heartY, false, isHardcore, halfHeart);
                    }
                }
            }
        }
    }

    @Unique
    private void renderErosionHeart(GuiGraphics guiGraphics, int x, int y,
                                    boolean isAbsorbing, boolean hardcore, boolean half) {

        int u;
        if (isAbsorbing) {
            
            if (hardcore) {
                u = half ? 63 : 54; 
            } else {
                u = half ? 27 : 18; 
            }
        } else {
            
            if (hardcore) {
                u = half ? 45 : 36; 
            } else {
                u = half ? 9 : 0; 
            }
        }

        guiGraphics.blit(EROSION_HEARTS, x, y, u, 0, 9, 9, 72, 9);
    }

    @Unique
    private void renderHeartWithOriginalShake(GuiGraphics guiGraphics, Object heartType, int x, int y,
                                              int vOffset, boolean hardcore, boolean half) {
        try {
            
            Class<?> guiClass = Gui.class;
            Method renderHeartMethod = guiClass.getDeclaredMethod(
                    "renderHeart",
                    GuiGraphics.class,
                    Class.forName("net.minecraft.client.gui.Gui$HeartType"),
                    int.class, int.class, int.class, boolean.class, boolean.class
            );
            renderHeartMethod.setAccessible(true);
            renderHeartMethod.invoke((Gui)(Object)this, guiGraphics, heartType, x, y, vOffset, hardcore, half);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}