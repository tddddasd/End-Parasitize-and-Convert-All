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

        public EffectData(int type, int ticks) {
            this.type = type;
            this.remainingTicks = ticks;
        }

        public int getColorARGB() {
            float[] rgb = getColorRGB();
            int r = (int)(rgb[0] * 255);
            int g = (int)(rgb[1] * 255);
            int b = (int)(rgb[2] * 255);
            int a = 255; 
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        public float[] getColorRGB() {
            if (type == 0) { 
                return new float[]{0.2F, 0.9F, 1.0F};
            } else {         
                return new float[]{0.9F, 0.2F, 0.9F};
            }
        }
    }
}