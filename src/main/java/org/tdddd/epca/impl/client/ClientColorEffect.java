package org.tdddd.epca.impl.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClientColorEffect {
    
    public static final int TYPE_PARTIAL_ADAPTATION = 0;
    
    public static final int TYPE_FULL_ADAPTATION = 1;
    
    public static final int TYPE_CONVERSION = 2;
    
    public static final int TYPE_CONVERSION_FADE = 3;

    
    private static final float RAMP_MAX_ALPHA = 0.8F;
    
    private static final float PURPLE_R = 0.9F;
    private static final float PURPLE_G = 0.2F;
    private static final float PURPLE_B = 0.9F;

    private static final ConcurrentHashMap<Integer, EffectData> ACTIVE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientColorEffect.tick();
        }
    }

    public static void setEffect(int entityId, int type, int durationTicks) {
        ACTIVE.put(entityId, new EffectData(type, durationTicks));
    }

    public static EffectData getEffect(LivingEntity entity) {
        if (entity == null) return null;
        EffectData data = ACTIVE.get(entity.getId());
        if (data == null) return null;
        if (data.remainingTicks <= 0) {
            ACTIVE.remove(entity.getId());
            return null;
        }
        return data;
    }

    public static void tick() {
        ACTIVE.values().removeIf(data -> {
            data.remainingTicks--;
            return data.remainingTicks <= 0;
        });
    }

    public static class EffectData {
        public final int type;   
        public int remainingTicks;
        
        public final int totalTicks;

        public EffectData(int type, int ticks) {
            this.type = type;
            this.remainingTicks = ticks;
            this.totalTicks = ticks;
        }

        public int getColorARGB() {
            float[] rgb = getColorRGB();
            int r = clampChannel(rgb[0]);
            int g = clampChannel(rgb[1]);
            int b = clampChannel(rgb[2]);
            int a = getAlphaChannel();
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        public float[] getColorRGB() {
            if (type == TYPE_PARTIAL_ADAPTATION) { 
                return new float[]{0.2F, 0.9F, 1.0F};
            }
            if (type == TYPE_CONVERSION || type == TYPE_CONVERSION_FADE) {
                
                float alpha = getRampAlpha();
                return new float[]{PURPLE_R * alpha, PURPLE_G * alpha, PURPLE_B * alpha};
            } else {         
                return new float[]{PURPLE_R, PURPLE_G, PURPLE_B};
            }
        }

        
        private float getRampAlpha() {
            float progress = getProgress();
            if (type == TYPE_CONVERSION) {
                return RAMP_MAX_ALPHA * progress;
            }
            return RAMP_MAX_ALPHA * (1.0F - progress);
        }

        private float getProgress() {
            if (totalTicks <= 0) {
                return 1.0F;
            }
            float progress = (float) (totalTicks - remainingTicks) / (float) totalTicks;
            if (progress < 0.0F) return 0.0F;
            if (progress > 1.0F) return 1.0F;
            return progress;
        }

        
        private int getAlphaChannel() {
            if (type == TYPE_CONVERSION || type == TYPE_CONVERSION_FADE) {
                int alpha = (int) (getRampAlpha() * 255.0F);
                if (alpha < 0) return 0;
                if (alpha > 255) return 255;
                return alpha;
            }
            return 255;
        }

        private static int clampChannel(float value) {
            int channel = (int) (value * 255.0F);
            if (channel < 0) return 0;
            if (channel > 255) return 255;
            return channel;
        }
    }
}
