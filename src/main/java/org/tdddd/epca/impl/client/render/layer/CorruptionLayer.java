package org.tdddd.epca.impl.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.client.render.IItemShaderLayer;
import org.tdddd.epca.impl.client.render.ItemLayerConfig;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.client.render.ItemShaderRenderTypes;
import org.tdddd.epca.impl.client.render.shader.EpcaShaders;

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
    public ShaderInstance shader() {
        return EpcaShaders.corruptionShader;
    }

    @Override
    public ItemShaderRenderTypes renderTypes() {
        return EpcaShaders.CORRUPTION_TYPES;
    }

    @Override
    public ResourceLocation maskTexture(ItemStack stack, ItemLayerConfig config) {
        if (config.maskOverride() != null) {
            return config.maskOverride();
        }

        return ItemRenderRegistry.defaultMaskFor(stack);
    }

    @Override
    public boolean prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx, boolean lateRender) {
        Minecraft mc = Minecraft.getInstance();
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;

        float intensity = CorruptionPulse.intensity(stack, gameTime) * config.strength();
        if (intensity < 0.005f) {
            return false;
        }

        if (EpcaShaders.corruptionTimeUniform != null) {
            EpcaShaders.corruptionTimeUniform.set((float) (gameTime % Integer.MAX_VALUE));
        }
        if (EpcaShaders.corruptionIntensityUniform != null) {
            EpcaShaders.corruptionIntensityUniform.set(intensity);
        }
        if (EpcaShaders.corruptionTintUniform != null) {
            EpcaShaders.corruptionTintUniform.set(defaultTintR, defaultTintG, defaultTintB);
        }
        if (EpcaShaders.corruptionSplitUniform != null) {
            EpcaShaders.corruptionSplitUniform.set(defaultSplitStrength);
        }
        return true;
    }

    @Override
    public void applyTwitch(PoseStack poseStack, ItemStack stack, ItemLayerConfig config, long gameTime) {
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
