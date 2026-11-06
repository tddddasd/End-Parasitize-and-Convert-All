package org.tdddd.epca.impl.client.render.layer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.client.render.IItemShaderLayer;
import org.tdddd.epca.impl.client.render.ItemLayerConfig;
import org.tdddd.epca.impl.client.render.ItemLayerPayload;
import org.tdddd.epca.impl.client.render.ItemShaderPipelines;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;

public final class CorruptionLayer implements IItemShaderLayer {

    public static final String NAME = "corruption";

    public static final CorruptionLayer INSTANCE = new CorruptionLayer();

    private static float defaultTintR = 0.55f;
    private static float defaultTintG = 0.08f;
    private static float defaultTintB = 0.50f;

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

        return ItemRenderRegistry.defaultMaskFor(stack);
    }

    @Override
    public ItemLayerPayload prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx,
                                    long gameTime) {
        float intensity = CorruptionPulse.intensity(stack, gameTime) * config.strength();
        if (intensity < 0.005f) {
            return null;
        }

        int timeTicks = (int) (gameTime % Integer.MAX_VALUE);
        return new ItemLayerPayload(timeTicks, intensity, defaultSplitStrength,
                defaultTintR, defaultTintG, defaultTintB);
    }

    @Override
    public void applyTwitch(com.mojang.blaze3d.vertex.PoseStack poseStack, ItemStack stack,
                            ItemLayerConfig config, long gameTime) {
        CorruptionPulse.applyTwitch(poseStack, stack, gameTime, config.strength());
    }

    public static void setDefaultTint(float r, float g, float b) {
        defaultTintR = r;
        defaultTintG = g;
        defaultTintB = b;
    }

    public static void setDefaultSplitStrength(float value) {
        defaultSplitStrength = value;
    }
}
