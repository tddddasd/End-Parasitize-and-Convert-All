package org.tdddd.epca.impl.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;

import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ClientColorEffect {
    private static final ConcurrentHashMap<Integer, EffectData> ACTIVE = new ConcurrentHashMap<>();

    
    public static final int TYPE_ADAPTATION = 0;
    
    public static final int TYPE_OTHER = 1;
    
    public static final int TYPE_CONVERSION = 2;
    
    public static final int TYPE_CONVERSION_FADE = 3;

    
    private static final float RAMP_MAX_ALPHA = 0.8F;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientColorEffect.tick();
    }

    public static void setEffect(int entityId, int type, int durationTicks) {
        ACTIVE.put(entityId, new EffectData(type, durationTicks));
    }

    public static EffectData getEffect(LivingEntity entity) {
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
            int a = getAlphaChannel();
            // Ramp types also scale RGB with the same progress, so cutout (non-blended) vanilla
            // models visibly turn purple gradually instead of switching instantly.
            float scale = (isRampType() && totalTicks > 0) ? (a / 255.0F) : 1.0F;
            int r = (int)(rgb[0] * scale * 255);
            int g = (int)(rgb[1] * scale * 255);
            int b = (int)(rgb[2] * scale * 255);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        
        public int getAlphaChannel() {
            if (!isRampType() || totalTicks <= 0) {
                return 255;
            }
            float elapsed = (float)(totalTicks - remainingTicks);
            if (elapsed < 0) elapsed = 0;
            if (elapsed > totalTicks) elapsed = totalTicks;
            float progress = elapsed / (float) totalTicks;
            float alpha = type == TYPE_CONVERSION
                    ? RAMP_MAX_ALPHA * progress
                    : RAMP_MAX_ALPHA * (1.0F - progress);
            if (alpha < 0.0F) alpha = 0.0F;
            if (alpha > RAMP_MAX_ALPHA) alpha = RAMP_MAX_ALPHA;
            return (int)(alpha * 255);
        }

        public boolean isRampType() {
            return type == TYPE_CONVERSION || type == TYPE_CONVERSION_FADE;
        }

        public float[] getColorRGB() {
            if (type == TYPE_ADAPTATION) { 
                return new float[]{0.2F, 0.9F, 1.0F};
            } else {         
                return new float[]{0.9F, 0.2F, 0.9F};
            }
        }
    }
}
