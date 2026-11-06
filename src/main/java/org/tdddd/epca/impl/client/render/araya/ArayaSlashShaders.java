package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.io.IOException;

public final class ArayaSlashShaders {

    public static final ResourceLocation SHADER_ID = new ResourceLocation(epca.MODID, "araya_slash");

    public static ShaderInstance shader;

    public static final float GLOW_STRENGTH = 1.0F;

    public static final float GLOW_RED = 1.0F;
    public static final float GLOW_GREEN = 1.0F;
    public static final float GLOW_BLUE = 1.0F;

    private static Uniform uGlowStrength;
    private static Uniform uGlowColor;
    private static Uniform uRefractionRatio;

    private ArayaSlashShaders() {
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), SHADER_ID,
                            DefaultVertexFormat.POSITION_COLOR_TEX),
                    registered -> {
                        shader = registered;
                        uGlowStrength = registered.getUniform("GlowStrength");
                        uGlowColor = registered.getUniform("GlowColor");
                        uRefractionRatio = registered.getUniform("RefractionRatio");
                        if (uGlowStrength != null) {
                            uGlowStrength.set(GLOW_STRENGTH);
                        }
                        if (uGlowColor != null) {
                            uGlowColor.set(GLOW_RED, GLOW_GREEN, GLOW_BLUE);
                        }
                        if (uRefractionRatio != null) {
                            uRefractionRatio.set(ArayaConstants.SLASH_REFRACTION_RATIO);
                        }
                        epca.LOGGER.info("[epca-araya] slash shader loaded: {}", SHADER_ID);
                    });
        } catch (IOException exception) {
            throw new RuntimeException("EPCA: could not load the araya slash shader "
                    + "assets/" + epca.MODID + "/shaders/core/araya_slash.*", exception);
        }
    }

    public static ShaderInstance arayaSlashShader() {
        return shader;
    }
}
