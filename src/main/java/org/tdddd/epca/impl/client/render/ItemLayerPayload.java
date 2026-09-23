package org.tdddd.epca.impl.client.render;

/**
 * The per-draw payload a {@link IItemShaderLayer} produces in
 * {@link IItemShaderLayer#prepare}.
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * In 1.20.1 this information was pushed straight into per-draw {@code Uniform}s
 * ({@code time}, {@code intensity}, {@code tint}, {@code splitStrength}). 26.1.2 has no reachable
 * per-draw uniform for geometry submitted through
 * {@code SubmitNodeCollector#submitCustomGeometry}, and the geometry itself is emitted by
 * {@code VertexConsumer#putBakedQuad} (which writes POSITION, COLOR, UV0, UV1, UV2 and NORMAL and
 * nothing else), so the payload is carried by the two slots that method lets a caller set through
 * {@code QuadInstance} plus the vertex colour:
 *
 * <ul>
 *   <li>{@code QuadInstance#setColor} -&gt; the COLOR element,
 *       {@link #tintRed()}/{@link #tintGreen()}/{@link #tintBlue()};</li>
 *   <li>{@code QuadInstance#setOverlayCoords} -&gt; the UV1 element, which a 2xShort attribute reads
 *       as an {@code ivec2}; the two 16-bit halves reconstruct a full 32-bit
 *       {@link #timeTicks()} (this is how a seconds/tick counter survives without a float slot);</li>
 *   <li>{@code QuadInstance#setLightCoords} -&gt; the UV2 element, likewise an {@code ivec2}, holding
 *       {@link #intensity()} and {@link #splitStrength()} as 16-bit fixed point.</li>
 * </ul>
 *
 * <p>{@code ColorModulator} stays (1,1,1,1), and the lightmap sampler is not declared, so the UV2
 * value never reaches a lightmap - it is pure data.</p>
 */
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

    /**
     * Animation clock in game ticks. Kept as a full 32-bit int and split across the two halves of the
     * UV1 attribute, which is what the 1.20.1 shader's {@code time} uniform carried
     * ({@code (float) (gameTime % Integer.MAX_VALUE)}).
     */
    public int timeTicks() {
        return timeTicks;
    }

    /** Decay strength, 0..1. */
    public float intensity() {
        return intensity;
    }

    /** RGB split / displacement multiplier, 0..4 (the 1.20.1 {@code splitStrength} range). */
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

    /** Packs the 0..1 intensity onto the 16-bit fixed point scale the shader divides by 65535. */
    public int packedIntensity() {
        return quantise(intensity, 1.0f);
    }

    /** Packs the 0..4 split strength onto the same 16-bit scale. */
    public int packedSplitStrength() {
        return quantise(splitStrength, 4.0f);
    }

    private static int quantise(float value, float range) {
        float clamped = Math.max(0.0f, Math.min(range, value));
        return Math.round(clamped / range * 65535.0f);
    }
}
