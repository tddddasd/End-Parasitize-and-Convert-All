package org.tdddd.epca.impl.client.render;

public final class ItemLayerPayload {

    private final int timeTicks;
    private final float intensity;
    private final float splitStrength;
    private final float tintRed;
    private final float tintGreen;
    private final float tintBlue;

    public ItemLayerPayload(int timeTicks, float intensity, float splitStrength,
                            float tintRed, float tintGreen, float tintBlue) {
        this.timeTicks = timeTicks;
        this.intensity = intensity;
        this.splitStrength = splitStrength;
        this.tintRed = tintRed;
        this.tintGreen = tintGreen;
        this.tintBlue = tintBlue;
    }

    public int timeTicks() {
        return timeTicks;
    }

    public float intensity() {
        return intensity;
    }

    public float splitStrength() {
        return splitStrength;
    }

    public float tintRed() {
        return tintRed;
    }

    public float tintGreen() {
        return tintGreen;
    }

    public float tintBlue() {
        return tintBlue;
    }

    public int packedIntensity() {
        return quantise(intensity, 1.0f);
    }

    public int packedSplitStrength() {
        return quantise(splitStrength, 4.0f);
    }

    private static int quantise(float value, float range) {
        float clamped = Math.max(0.0f, Math.min(range, value));
        return Math.round(clamped / range * 65535.0f);
    }
}
