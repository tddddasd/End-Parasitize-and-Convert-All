#version 330

// EPCA Alayavijnana slash (fragment stage), 26.1.2 core shader.
//
// One program draws both halves of the effect:
//
//   * the glowing strips (line + halo) carry a zero scene offset in their middle and an offset of at
//     most a fraction of a pixel at their edges, so they read the frame copy essentially at their own
//     pixel and keep the emissive white term;
//   * the two border strips are the transparent part that bends the scene
//     ("剑痕周围0.05格的部分为透明，但会折射透明部分后面的景象"): their offset grows to the full refraction
//     shift at the outer edge.
//
// UV0.x is the distance from the blade's centreline in blocks and UV0.y the half width of the strip it
// belongs to, so the same profile normalisation serves all four strips. The glowing line is white; the
// tint is a constant here because 26.1.2 has no per-draw uniform on this path (see the vertex stage).

in vec4 vertexColor;
in vec2 ribbonCoord;
in vec2 screenUv;
in vec2 sceneOffset;

uniform sampler2D Sampler0;

out vec4 fragColor;

const float GLOW_STRENGTH = 1.0;
const vec3 GLOW_COLOR = vec3(1.0, 1.0, 1.0);

void main() {
    // The frame as it was a moment ago, sampled at the pixel this fragment covers plus this strip's
    // offset. The offset is zero on the blade's centreline and largest at the outer edge of the border.
    vec4 scene = texture(Sampler0, screenUv + sceneOffset);

    // Profile of the strip: full on the centreline, zero at its edge. Squared, so the falloff reads as a
    // glow rather than a linear gradient.
    float t = 1.0 - clamp(abs(ribbonCoord.x) / max(ribbonCoord.y, 0.0001), 0.0, 1.0);
    float alpha = t * t * vertexColor.a * GLOW_STRENGTH;

    vec3 color = mix(scene.rgb, GLOW_COLOR, clamp(alpha, 0.0, 1.0));

    fragColor = vec4(color, alpha);
}
