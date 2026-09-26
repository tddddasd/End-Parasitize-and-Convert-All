package org.tdddd.epca.impl.client.render.araya;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;

/**
 * The Alayavijnana slash shader's identity and its one tunable, on 26.1.2.
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>The 1.20.1 twin loads a {@code ShaderInstance} through {@code RegisterShadersEvent} and reads
 * {@code GlowStrength}, {@code GlowColor} and {@code RefractionRatio} off it as {@code Uniform} handles.
 * 26.1.2 has neither a core shader JSON nor a reachable per-draw uniform on this path, so the shader is
 * bound through {@code ArayaSlashRenderType.SLASH_PIPELINE} and its constants live in the GLSL itself.
 * This class therefore keeps only the two things the Java side still owns: the shader id the pipeline
 * resolves, and the mirror of the refraction constant - which
 * {@code build/javac-check/check-glsl-26.py} compares against {@code REFRACTION_RATIO} in
 * {@code araya_slash.vsh}, exactly the way it compares the gas and heart constants, so the two versions
 * cannot drift apart.</p>
 */
public final class ArayaSlashShaders {

    /** Shader id; {@code FileToIdConverter} resolves it to {@code assets/epca/shaders/core/araya_slash.*}. */
    public static final Identifier SHADER_ID =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/araya_slash");

    /** Mirror of {@code REFRACTION_RATIO} in {@code araya_slash.vsh}; see {@link ArayaConstants}. */
    public static final float REFRACTION_RATIO = ArayaConstants.SLASH_REFRACTION_RATIO;

    /** Strength of the emissive white term, mirror of {@code GLOW_STRENGTH} in the fragment stage. */
    public static final float GLOW_STRENGTH = 1.0F;

    /** Colour of the glowing line ("一道白色...剑痕"): pure white, mirror of {@code GLOW_COLOR}. */
    public static final float GLOW_RED = 1.0F;
    public static final float GLOW_GREEN = 1.0F;
    public static final float GLOW_BLUE = 1.0F;

    private ArayaSlashShaders() {
    }
}
