#version 150

// Fragment stage of the Alayavijnana slash. One program draws both halves of the effect:
//
//   * the glowing strips (line + halo) carry a zero scene offset in their middle and an offset of at
//     most a fraction of a pixel at their edges, so they read the frame copy essentially at their own
//     pixel and keep the emissive white term;
//   * the two border strips are the transparent part that bends the scene
//     ("剑痕周围0.05格的部分为透明，但会折射透明部分后面的景象"): their offset grows to the full refraction
//     shift at the outer edge.
//
// UV0.x is the distance from the blade's centreline in blocks and UV0.y the half width of the strip it
// belongs to, so the same profile normalisation serves all four strips.

in vec4 vertexColor;
in vec2 ribbonCoord;
in vec2 screenUv;
in vec2 sceneOffset;

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GlowStrength;
uniform vec3 GlowColor;

out vec4 fragColor;

void main() {
    // The frame as it was a moment ago, sampled at the pixel this fragment covers plus this strip's
    // offset. The offset is zero on the blade's centreline and largest at the outer edge of the border.
    vec4 scene = texture(Sampler0, screenUv + sceneOffset);

    // Profile of the strip: full on the centreline, zero at its edge. Squared, so the falloff reads as a
    // glow rather than a linear gradient.
    float t = 1.0 - clamp(abs(ribbonCoord.x) / max(ribbonCoord.y, 0.0001), 0.0, 1.0);
    float alpha = t * t * vertexColor.a * GlowStrength;

    vec3 color = mix(scene.rgb, GlowColor, clamp(alpha, 0.0, 1.0));

    fragColor = vec4(color, alpha) * ColorModulator;
}
