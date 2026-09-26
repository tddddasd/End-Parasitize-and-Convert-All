package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.io.IOException;

/**
 * Loading and uniform handles of the Alayavijnana slash shader.
 *
 * <p>Unlike the sky-rupture and gas-cloud shaders next to this one, this shader needs no per-cloud,
 * per-quad or per-frame payload: the geometry carries the ribbon coordinate and the refraction offset in
 * its own vertices (see {@code ArayaSlashRenderType}), and the only uniforms are the two matrices every
 * core shader gets plus the two constants below. They exist as uniforms rather than as literals inside
 * the GLSL so the look can be retuned without recompiling a shader, and so
 * {@code build/javac-check/check-araya-glsl.py} can cross-check them against the Java side.</p>
 */
public final class ArayaSlashShaders {

    /** Shader id; resolves to {@code assets/epca/shaders/core/araya_slash.vsh/.fsh}. */
    public static final ResourceLocation SHADER_ID = new ResourceLocation(epca.MODID, "araya_slash");

    /** The loaded shader, or {@code null} until {@link #onRegisterShaders} ran. */
    public static ShaderInstance shader;

    /** Strength of the emissive white term; 1.0 is the shipped look. */
    public static final float GLOW_STRENGTH = 1.0F;

    /** Colour of the glowing line ("一道白色...剑痕"): pure white. */
    public static final float GLOW_RED = 1.0F;
    public static final float GLOW_GREEN = 1.0F;
    public static final float GLOW_BLUE = 1.0F;

    private static Uniform uGlowStrength;
    private static Uniform uGlowColor;
    private static Uniform uRefractionRatio;

    private ArayaSlashShaders() {
    }

    /** Mod-bus handler for {@code RegisterShadersEvent}; call it from the client shader registration. */
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

    /** The loaded shader, or {@code null} while it is not ready. */
    public static ShaderInstance arayaSlashShader() {
        return shader;
    }
}
