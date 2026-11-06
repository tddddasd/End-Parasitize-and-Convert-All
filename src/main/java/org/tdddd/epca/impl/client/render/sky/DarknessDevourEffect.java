package org.tdddd.epca.impl.client.render.sky;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.util.Mth;

public final class DarknessDevourEffect {

    private static final float GROUND_RISE_START = 0.28f;
    private static final float GROUND_RISE_END = 0.72f;
    private static final float GROUND_FALL_START = 0.84f;
    private static final float GROUND_FALL_END = 1.00f;

    private DarknessDevourEffect() {
    }

    public static float skyProgress() {
        return SkyRuptureEffect.darkProgress();
    }

    public static float skyOpacity() {
        return Mth.clamp(skyProgress() * 1.15f, 0f, 1f);
    }

    public static float devour() {
        return window(GROUND_RISE_START, GROUND_RISE_END, GROUND_FALL_START, GROUND_FALL_END);
    }

    public static void applyFog(FogData fog, float renderDistance) {
        float d = devour();
        if (d <= 0.002f) {

            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.isUnderWater() || mc.player.isInLava())) {

            return;
        }

        float far = Math.max(renderDistance, 24.0f);

        float front = Mth.lerp(d, far * 1.05f, -8.0f);

        float softness = Mth.lerp(d, far * 0.30f, 10.0f);

        float start = front;
        float end = start + Math.max(softness, 2.5f);

        fog.environmentalStart = start;
        fog.environmentalEnd = end;
        fog.renderDistanceStart = start;
        fog.renderDistanceEnd = end;

        fog.color.set(SkyRuptureEffect.voidRed(), SkyRuptureEffect.voidGreen(),
                SkyRuptureEffect.voidBlue(), fog.color.w);
    }

    private static float window(float riseStart, float riseEnd, float fallStart, float fallEnd) {
        if (!SkyRuptureEffect.isActive()) {
            return 0f;
        }
        float u = SkyRuptureEffect.progress();
        float rise = smooth01((u - riseStart) / (riseEnd - riseStart));
        float fall = smooth01((u - fallStart) / (fallEnd - fallStart));
        return rise * (1.0f - fall);
    }

    private static float smooth01(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }
}
