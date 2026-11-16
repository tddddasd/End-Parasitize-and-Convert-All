package org.tdddd.epca.impl.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side tint色层 for the YNA adaptation flash only.
 * <p>
 * The old "conversion purple" types ({@code TYPE_CONVERSION} / {@code TYPE_CONVERSION_FADE}) were removed
 * together with the conversion delay: entity conversion is now instant and no longer paints the entity purple.
 * Only the YNA damage-adaptation colours (plus the {@code AdaptationColorConfig} override) remain here.
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClientColorEffect {

    public static final int TYPE_PARTIAL_ADAPTATION = 0;

    public static final int TYPE_FULL_ADAPTATION = 1;

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

        int override = 0;
        if (org.tdddd.yawning_neko_api.data.AdaptationColorConfig.applies(entity)) {
            int eventType = org.tdddd.yawning_neko_api.data.DamageAdaptation.getActiveAdaptationEvent(entity);
            int argb = org.tdddd.yawning_neko_api.data.AdaptationColorConfig.colorFor(eventType);
            if (argb != -1) {
                override = argb;
            }
        }
        data.colorOverride = override;
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

        public int colorOverride = 0;

        public final int totalTicks;

        public EffectData(int type, int ticks) {
            this.type = type;
            this.remainingTicks = ticks;
            this.totalTicks = ticks;
        }

        public int getColorARGB() {
            if (this.colorOverride != 0) {
                return this.colorOverride;
            }
            float[] rgb = getColorRGB();
            int r = clampChannel(rgb[0]);
            int g = clampChannel(rgb[1]);
            int b = clampChannel(rgb[2]);
            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        }

        public float[] getColorRGB() {
            if (type == TYPE_PARTIAL_ADAPTATION) {
                return new float[]{0.2F, 0.9F, 1.0F};
            }
            return new float[]{PURPLE_R, PURPLE_G, PURPLE_B};
        }

        private static int clampChannel(float value) {
            int channel = (int) (value * 255.0F);
            if (channel < 0) return 0;
            if (channel > 255) return 255;
            return channel;
        }
    }
}
