#version 330

// EPCA gas cloud billboard shader (fragment stage), 26.1.2 core shader.
//
// Produces a soft, irregular red gas puff:
//   1. sample frame 0 of the shared INFESTIVE_GAS texture (Sampler0),
//   2. build a soft irregular alpha mask from a procedural multi-frequency noise field
//      (no noise texture asset is required),
//   3. multiply by the red tint, the per-cloud fade carried in the vertex colour alpha, and the
//      per-cloud seed carried in the UV1 vertex slot.
//
// Dissipation is therefore smooth in time (fade), in space (radial falloff times the noise mask)
// and in depth (the far-distance fade below), instead of a hard cut-off.

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
in float vertexDistance;
flat in ivec2 gasSeed;

out vec4 fragColor;

// Red tint derived from the INFESTIVE_GAS particle texture: the alpha weighted average of its
// visible pixels is RGB (137, 45, 45) = (0.54, 0.18, 0.18); it is lifted to a readable red so the
// cloud stays visible once the mask and the fade are applied.
const vec3 GAS_TINT = vec3(1.00, 0.33, 0.34);
// How strongly GAS_TINT replaces the texture colour (1.0 = ignore the texture RGB).
const float GAS_TINT_STRENGTH = 0.85;
// Overall mask strength; 0 would erase the cloud entirely.
const float GAS_MASK_STRENGTH = 1.0;
// Scale of the procedural noise field in texture space.
const float GAS_NOISE_SCALE = 2.4;
// How strongly the noise cuts into the mask (0 = flat disc, 1 = fully noise shaped). Mirrors
// GasCloudRenderType.MASK_DEPTH; same name and value as the 1.20.1 twin so the two shaders diff.
const float GAS_MASK_DEPTH = 0.50;
// Floor for the noise mask. The sprite alpha is binary (see GAS_MIN_TEXTURE_ALPHA), so the cloud's
// silhouette has to come from this procedural mask rather than from the few opaque texels.
const float GAS_MIN_NOISE_MASK = 0.60;
// The INFESTIVE_GAS texture is a 16x144 strip of nine 16x16 frames; frame 0 is the top band.
const float GAS_FRAME_COUNT = 9.0;
// Floor for the sampled sprite alpha. Frame 0 is the most opaque band of the sheet (measured mean
// alpha 108.6/255 = 0.426, 42.6% of its texels covered and every covered texel at alpha 255) but its
// MEDIAN alpha is 0, so multiplying tex.a straight into the chain collapsed the cloud to nothing.
// Floored at 0.65 the sprite's irregular silhouette still shows through while the cloud stays
// visible. Mirrors GasCloudRenderType.MIN_TEXTURE_ALPHA.
const float GAS_MIN_TEXTURE_ALPHA = 0.65;
// Overall scale of the finished alpha chain. Mirrors GasCloudRenderType.ALPHA_BOOST.
// Fresh longarms active cloud and yelloweye clouds (Color.a = 0.90, same opacity by spec):
//   peak  = 0.90 * 0.85 * 1.00 * 0.9375 * 1.00 = 0.717
//   floor = 0.90 * 0.85 * 0.65 * 0.60   * 1.00 = 0.298
// Fresh longarms passive cloud (Color.a = 0.45):
//   peak  = 0.45 * 0.85 * 1.00 * 0.9375 * 1.00 = 0.359
//   floor = 0.45 * 0.85 * 0.65 * 0.60   * 1.00 = 0.149
const float GAS_ALPHA_BOOST = 0.85;
// Fraction of the quad's half-extent over which the radial term stays at full strength. Without a
// plateau the radial term falls off from the very centre and the cloud has no solid core.
// Mirrors GasCloudRenderType.RADIAL_PLATEAU.
const float GAS_RADIAL_PLATEAU = 0.55;
// Fixed point scale of the packed per-cloud seed (mirrors GasCloudRenderType.SEED_FIXED_POINT_SCALE).
const float GAS_SEED_FIXED_POINT_SCALE = 100.0;
// Spread applied to the sub-quad index so the two seed channels differ per sub-quad.
const float GAS_SUB_QUAD_INDEX_SCALE = 0.37;
// Far-distance fade: clouds fade out between these two camera distances, in blocks.
const float GAS_DISTANCE_FADE_START = 48.0;
const float GAS_DISTANCE_FADE_END = 96.0;

// Hard-edged "spec" style (contaminated water micro rectangles). The second channel of the UV1
// vertex slot carries GAS_SPEC_STYLE_CHANNEL instead of a sub-quad index; every real sub-quad index
// is far below it. Mirrors GasCloudRenderType.SPEC_STYLE_CHANNEL.
const int GAS_SPEC_STYLE_CHANNEL = 250;
// Base factor of the spec alpha chain: a hard-edged rectangle has no texture alpha to reduce it.
// Mirrors GasCloudRenderType.SPEC_BASE_ALPHA.
const float GAS_SPEC_BASE_ALPHA = 1.0;

// Positive-control switch (diagnostics only). While true the fragment stage outputs a solid opaque
// red for every style, so geometry/space/pipeline can be checked independently of the alpha mask
// math; it must stay false in normal play.
const bool GAS_DEBUG_OPAQUE = false;

// Cheap deterministic pseudo-random hash, stable for a fixed seed.
float gasHash(vec2 p, float seed) {
    return fract(sin(dot(p, vec2(127.1, 311.7)) + seed * 74.7) * 43758.5453123);
}

// Smooth 2D value noise built from the hash above.
float gasNoise(vec2 p, float seed) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = gasHash(i, seed);
    float b = gasHash(i + vec2(1.0, 0.0), seed);
    float c = gasHash(i + vec2(0.0, 1.0), seed);
    float d = gasHash(i + vec2(1.0, 1.0), seed);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// Three-octave fbm used for both the silhouette and the interior density.
float gasFbm(vec2 p, float seed) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += amplitude * gasNoise(p, seed + float(i) * 17.0);
        p *= 2.03;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    // vertexDistance is the true camera distance (see gas_cloud.vsh). It must stay positive, or
    // this fade silently becomes dead code - the bug the 1.20.1 twin had with its raw viewPos.z.
    float edgeFade = 1.0 - smoothstep(GAS_DISTANCE_FADE_START, GAS_DISTANCE_FADE_END, vertexDistance);

    // Positive-control switch (diagnostics only): a solid opaque red for every style, so geometry,
    // space and the pipeline can be checked independently of the alpha mask math. It must stay
    // false in normal play; the real paths are untouched behind it.
    if (GAS_DEBUG_OPAQUE) {
        fragColor = vec4(1.0, 0.1, 0.1, 1.0);
        return;
    }

    // SPEC STYLE: a solid, hard-edged micro rectangle. Selected by the packed per-quad channel in
    // UV1.y (the gas style puts the sub-quad index there), so no texture, radial falloff or noise
    // mask is applied. The dark red arrives in the vertex colour and is tinted like the gas colour.
    if (gasSeed.y == GAS_SPEC_STYLE_CHANNEL) {
        float specAlpha = clamp(GAS_SPEC_BASE_ALPHA * GAS_ALPHA_BOOST * vertexColor.a * edgeFade,
                0.0, 1.0);
        fragColor = vec4(GAS_TINT * vertexColor.rgb * ColorModulator.rgb, specAlpha);
        return;
    }

    vec4 tex = texture(Sampler0, vec2(texCoord0.x, texCoord0.y / GAS_FRAME_COUNT));
    vec4 tinted = mix(tex, vec4(GAS_TINT, tex.a), GAS_TINT_STRENGTH);
    // The sprite's own alpha is floored here: frame 0 has median alpha 0, so using it raw (as a
    // plain multiplier) collapsed the finished alpha to nearly nothing.
    float base = max(tex.a, GAS_MIN_TEXTURE_ALPHA);

    // Per-cloud seed: channel 0 is the packed float seed, channel 1 the sub-quad index.
    float seed = float(gasSeed.x) / GAS_SEED_FIXED_POINT_SCALE
            + float(gasSeed.y) * GAS_SUB_QUAD_INDEX_SCALE;

    // Radial term with a flat core: 1.0 out to GAS_RADIAL_PLATEAU of the half-extent, then a
    // smoothstep down to 0 at the quad edge, so radial stays 1 over the inner ~55% of the quad.
    vec2 centered = texCoord0 - vec2(0.5);
    float radius = length(centered) * 2.0;
    float radialT = max((radius - GAS_RADIAL_PLATEAU) / (1.0 - GAS_RADIAL_PLATEAU), 0.0);
    float radial = 1.0 - clamp(radialT, 0.0, 1.0);
    radial = radial * radial * (3.0 - 2.0 * radial);

    // Shape noise, floored so it modulates the fill instead of erasing it.
    float mask = gasFbm(texCoord0 * GAS_NOISE_SCALE, seed);
    mask = mix(1.0, mask, GAS_MASK_DEPTH);
    mask = max(mask, GAS_MIN_NOISE_MASK);

    float fade = vertexColor.a;
    float alpha = base * radial * mask * GAS_MASK_STRENGTH * GAS_ALPHA_BOOST * fade * edgeFade;
    alpha = clamp(alpha, 0.0, 1.0);

    fragColor = vec4(tinted.rgb * vertexColor.rgb * ColorModulator.rgb, alpha);
}
