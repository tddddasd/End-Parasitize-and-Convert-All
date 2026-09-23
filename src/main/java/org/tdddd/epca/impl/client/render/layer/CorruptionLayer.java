package org.tdddd.epca.impl.client.render.layer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.client.render.IItemShaderLayer;
import org.tdddd.epca.impl.client.render.ItemLayerConfig;
import org.tdddd.epca.impl.client.render.ItemLayerPayload;
import org.tdddd.epca.impl.client.render.ItemShaderPipelines;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;

/**
 * The corruption render layer (ported from the reference project's corruption layer).
 *
 * <h2>Look</h2>
 * <ol>
 *   <li>RGB dispersion (a colour fringe whose direction rotates over time)</li>
 *   <li>tint shift + cold desaturation</li>
 *   <li>fine and coarse scanlines</li>
 *   <li>horizontal glitch band displacement</li>
 *   <li>grain noise</li>
 *   <li>random dead pixels + bright flicker pixels</li>
 *   <li>vignette</li>
 *   <li>a low frequency pulsing dark wave</li>
 * </ol>
 * plus the optional geometric twitch ({@link ItemLayerConfig#twitch()}).
 *
 * <h2>Strength</h2>
 * {@link CorruptionPulse#intensity} - a periodic burst by default, or a constant value if the item's
 * custom data carries {@code epca_corruption}. The final strength is multiplied by
 * {@link ItemLayerConfig#strength()}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION);
 *
 * ItemRenderRegistry.attach(ModItems.CLUSTER, ItemShaderLayers.CORRUPTION,
 *         ItemLayerConfig.builder().strength(0.8f).twitch(true).build());
 * }</pre>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * The per-draw {@code Uniform}s this class used to set ({@code time}, {@code intensity},
 * {@code tint}, {@code splitStrength}) are now fields of an {@link ItemLayerPayload}; see that class
 * for which vertex slot each one rides in. {@link #setDefaultTint} and
 * {@link #setDefaultSplitStrength} still work, because the payload carries the tint and the split
 * strength per draw.
 */
public final class CorruptionLayer implements IItemShaderLayer {

    public static final String NAME = "corruption";

    public static final CorruptionLayer INSTANCE = new CorruptionLayer();

    /** Default tint: magenta-purple (same as the reference project). */
    private static float defaultTintR = 0.55f;
    private static float defaultTintG = 0.08f;
    private static float defaultTintB = 0.50f;

    /** Default dispersion multiplier. */
    private static float defaultSplitStrength = 1.0f;

    private CorruptionLayer() {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isReady() {
        return ItemShaderPipelines.isPipelineRegistered();
    }

    @Override
    public Identifier maskTexture(ItemStack stack, ItemLayerConfig config) {
        if (config != null && config.maskOverride() != null) {
            return config.maskOverride();
        }
        // Default to the item's own texture, which is a 1:1 item silhouette and therefore lines up
        // with the item by construction.
        return ItemRenderRegistry.defaultMaskFor(stack);
    }

    @Override
    public ItemLayerPayload prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx,
                                    long gameTime) {
        float intensity = CorruptionPulse.intensity(stack, gameTime) * config.strength();
        if (intensity < 0.005f) {
            return null; // outside the burst window: skip the draw entirely
        }
        // The clock stays in game ticks, exactly like the 1.20.1 `time` uniform
        // ((float) (gameTime % Integer.MAX_VALUE)); it travels as a full 32-bit int split across the
        // two halves of the UV1 attribute, so no precision is lost.
        int timeTicks = (int) (gameTime % Integer.MAX_VALUE);
        return new ItemLayerPayload(timeTicks, intensity, defaultSplitStrength,
                defaultTintR, defaultTintG, defaultTintB);
    }

    @Override
    public void applyTwitch(com.mojang.blaze3d.vertex.PoseStack poseStack, ItemStack stack,
                            ItemLayerConfig config, long gameTime) {
        CorruptionPulse.applyTwitch(poseStack, stack, gameTime, config.strength());
    }

    // -- global default look --------------------------------------------------

    /** Changes the corruption's default tint (RGB, 0-1). */
    public static void setDefaultTint(float r, float g, float b) {
        defaultTintR = r;
        defaultTintG = g;
        defaultTintB = b;
    }

    /** Changes the corruption's default dispersion multiplier. */
    public static void setDefaultSplitStrength(float value) {
        defaultSplitStrength = value;
    }
}
