#version 330

// EPCA item corruption (fragment stage), 26.1.2 core shader.
//
// Ported from the 1.20.1 `corruption.fsh`, which itself came from the reference project
// RottenRuinsSplendiding's `assets/hall/shaders/core/corruption.fsh`. Every visual constant and every
// step of the maths is unchanged; only the plumbing around it moved.
//
//   1. RGB dispersion (the fringe direction rotates over time)
//   2. tint shift, then desaturation towards a cold grey
//   3. fine and coarse scanlines
//   4. horizontal glitch band displacement
//   5. grain noise (two octaves)
//   6. random dead pixels and bright flicker pixels
//   7. vignette
//   8. a low frequency pulsing dark wave
//
// 1.20.1 -> 26.1.2 differences, all documented approximations:
//   * `intensity`, `tint` and `splitStrength` arrive as varyings instead of uniforms (see
//     corruption.vsh for which vertex slot carries which value);
//   * the 1.20.1 stage ended with
//     `linear_fog(vec4(lit, a) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor)` and
//     mixed in `vertexColor` (the lightmap-modulated item colour) at 20%. Both are dropped: this
//     pipeline uses MATRICES_PROJECTION_SNIPPET, which does not bring the Fog std140 block, and the
//     COLOR element is repurposed for the tint. `ColorModulator` is applied, and it is always
//     (1,1,1,1) on this path anyway.
//   * the reference project's default tint (0.55, 0.08, 0.50) is no longer a JSON `values` default but
//     comes from java.util per draw (CorruptionLayer.defaultTint*).

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexTint;
in vec2 texCoord0;
in float time;
in float intensity;
in float splitStrength;

out vec4 fragColor;

// -- noise / hash -------------------------------------------------------------
// Unchanged from 1.20.1.

float hash2(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hash1(float n) {
    return fract(sin(n) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), f.x),
               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), f.x), f.y);
}

// -- main --------------------------------------------------------------------

void main() {
    vec2 uv = texCoord0;
    float i = clamp(intensity, 0.0, 1.0);
    float split = clamp(splitStrength, 0.0, 4.0);
    vec3 tint = vertexTint.rgb;
    vec4 base = texture(Sampler0, uv);

    if (i < 0.005) {
        fragColor = base * vertexTint * ColorModulator;
        return;
    }

    // -- 1. RGB dispersion, rotating slowly over time --
    float splitAmt = i * 0.012 * split;
    float ang = time * 0.7;
    vec2 dir = vec2(cos(ang), sin(ang));
    float r = texture(Sampler0, uv + dir * splitAmt).r;
    float g = texture(Sampler0, uv).g;
    float b = texture(Sampler0, uv - dir * splitAmt).b;
    vec3 rgb = vec3(r, g, b);

    // -- 2. tint shift -> corruption colour --
    rgb = mix(rgb, tint, i * 0.35);

    // Desaturate towards a cold grey.
    float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
    vec3 cold = vec3(gray * 0.7, gray * 0.25, gray * 0.85);
    rgb = mix(rgb, cold, i * 0.45);

    // -- 3. scanlines --
    float sl = sin(uv.y * 350.0 + time * 8.0) * 0.5 + 0.5;
    float slStrength = i * 0.18;
    float sl2 = sin(uv.y * 87.0 - time * 3.0) * 0.5 + 0.5;
    rgb *= 1.0 - sl * slStrength - sl2 * slStrength * 0.4;

    // -- 4. horizontal glitch bands --
    float row = floor(uv.y * 55.0);
    float gh = hash1(row * 137.0 + floor(time * 4.0));
    float glitch = step(0.94, gh) * i;
    if (glitch > 0.5) {
        float goff = (hash1(row * 311.0 + time * 2.7) - 0.5) * 0.08 * i * split;
        rgb.r = texture(Sampler0, uv + vec2(goff, 0.0)).r;
        rgb.b = texture(Sampler0, uv - vec2(goff * 0.6, 0.0)).b;
        rgb *= 0.7 + 0.3 * hash1(row + time);
    }

    // -- 5. grain noise --
    float grain = noise(uv * 400.0 + time * 20.0) * i * 0.1;
    rgb -= grain;
    float grain2 = hash2(uv * 700.0 + time * 13.0) * i * 0.04;
    rgb -= grain2;

    // -- 6. random dead pixels --
    float dp = hash2(uv * 200.0 + vec2(time * 5.0, time * 3.0 + 71.0));
    float dpMask = step(0.975, dp) * i * 0.55;
    rgb = mix(rgb, vec3(0.0, 0.0, 0.0), dpMask);

    // Bright flicker pixels.
    float bp = hash2(uv * 300.0 + vec2(time * 11.0, time * 7.0 + 13.0));
    float bpMask = step(0.985, bp) * i * 0.3;
    rgb = mix(rgb, vec3(0.9, 0.2, 0.8), bpMask);

    // -- 7. vignette --
    vec2 vig = abs(uv - 0.5) * 2.0;
    float v = 1.0 - dot(vig, vig) * 0.55 * i;
    rgb *= v;

    // -- 8. low frequency pulsing dark wave --
    float wave = sin(uv.y * 6.0 + time * 2.0) * sin(uv.x * 5.0 + time * 1.7) * i * 0.08;
    rgb += wave;

    vec4 color = vec4(rgb, 1.0);
    color.a *= base.a;

    // The 1.20.1 stage kept a little of the item's own lighting here
    // (mix(color.rgb, color.rgb * vertexColor.rgb, 0.2)). Dropped: COLOR carries the tint now.
    fragColor = vec4(color.rgb, color.a) * ColorModulator;
}
