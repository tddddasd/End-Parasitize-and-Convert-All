#version 150

// EPCA gas cloud billboard shader (fragment stage).
//
// Produces a soft, irregular red gas puff:
//   1. sample frame 0 of the shared INFESTIVE_GAS texture (Sampler0),
//   2. build a soft irregular alpha mask from a procedural multi-frequency noise field
//      (no noise texture asset is required),
//   3. multiply by the red tint, the mask and the per-cloud fade.
//
// PER-CLOUD VALUES COME FROM VERTEX ATTRIBUTES, NOT UNIFORMS. ShaderInstance.apply() uploads
// uniforms once per RenderType flush (one draw for every buffered cloud), so a per-cloud uniform
// would give all clouds the last cloud's value:
//   * per-cloud fade   -> vertexColor.a (written by the render layer as Color.a)
//   * per-cloud seed   -> cloudSeed.x / cloudSeed.y (from UV2; see gas_cloud.vsh)
// The only uniforms are the automatically uploaded ModelViewMat / ProjMat / ColorModulator plus
// the Sampler0 sampler.
//
// Sampler0 is epca:textures/particle/infestive_gas.png, a 16x144 vertical strip of nine 16x16
// animation frames. The render layer emits v in [0, 1] across the quad, so v is DIVIDED by the
// frame count (plus GAS_FRAME_INDEX) here: that maps the whole quad onto one band of the strip,
// stretched to a square without distortion. (Multiplying instead would sample all nine frames at
// once; the 26.1.2 twin divides as well.)

// Number of vertically packed frames in the INFESTIVE_GAS strip.
const float GAS_TEXTURE_FRAMES = 9.0;
// Which frame of the strip is sampled. Frame 0 (the top band) is the most opaque band of this
// texture: measured mean alpha 108.6/255 = 0.426, 42.6% of its texels covered and every covered
// texel at alpha 255, so per-texel alpha is effectively binary there while later frames are
// strictly fainter (band 1: 0.355, band 2: 0.258, ... band 8: 0.008). Frame 0 is therefore the
// right choice; a higher value only selects a fainter frame of the same animation.
const float GAS_FRAME_INDEX = 0.0;
// Red tint, mirrored in Java by GasCloudRenderType.TINT_RED / TINT_GREEN / TINT_BLUE.
const vec3 GAS_TINT_RGB = vec3(1.0, 0.33, 0.34);
// Mirrored by GasCloudRenderType.TINT_STRENGTH.
const float GAS_TINT_STRENGTH = 0.85;
// Mirrored by GasCloudRenderType.MASK_STRENGTH.
const float GAS_MASK_STRENGTH = 1.0;
// Alpha floors and overall scale. Because the texture alpha is binary, the old chain
// (tex.a * radial * mask * fade) landed around 0.05-0.15 and read as invisible. Every factor now
// has a floor and the radial term has a plateau, so the silhouette comes from the procedural mask
// instead of from the few fully opaque texels of the sprite:
//   alpha = Color.a * GAS_ALPHA_BOOST * max(tex.a, GAS_MIN_TEXTURE_ALPHA)
//                     * radial * max(mask, GAS_MIN_NOISE_MASK) * GAS_MASK_STRENGTH
// Fresh longarms active cloud and yelloweye clouds (Color.a = 0.90, same opacity by spec):
//   peak  = 0.90 * 0.85 * 1.00 * 1.00 * 1.00 = 0.765
//   floor = 0.90 * 0.85 * 0.65 * 0.60 * 1.00 = 0.298
// Fresh longarms passive cloud (Color.a = 0.45):
//   peak  = 0.45 * 0.85 * 1.00 * 1.00 * 1.00 = 0.383
//   floor = 0.45 * 0.85 * 0.65 * 0.60 * 1.00 = 0.149
const float GAS_ALPHA_BOOST = 0.85;
const float GAS_MIN_TEXTURE_ALPHA = 0.65;
const float GAS_MIN_NOISE_MASK = 0.60;
// Mirrored by GasCloudRenderType.MASK_DEPTH (how strongly the noise cuts into the mask; gentler).
const float GAS_MASK_DEPTH = 0.50;
// Mirrored by GasCloudRenderType.RADIAL_PLATEAU: the radial falloff stays at 1.0 over the inner
// GAS_RADIAL_PLATEAU of the quad and only then eases to 0 at the quad edge.
const float GAS_RADIAL_PLATEAU = 0.55;
// Mirrored by GasCloudRenderType.NOISE_UV_SCALE (noise frequency in quad space).
const float GAS_NOISE_UV_SCALE = 2.4;
// Mirrored by GasCloudRenderType.DISTANCE_FADE_START / DISTANCE_FADE_END.
const float GAS_DISTANCE_FADE_START = 48.0;
const float GAS_DISTANCE_FADE_END = 96.0;
// Mirrored by GasCloudRenderType.SEED_SCALE / SUBQUAD_SEED_SCALE: expands the packed fixed-point
// seed plus the sub-quad index back into a per-cloud noise seed.
const float GAS_SEED_SCALE = 0.004;
const float GAS_SUBQUAD_SEED_SCALE = 13.73;
// POSITIVE CONTROL #1 for the "clouds are created and submitted but nothing is visible" hunt.
// It was switched on for one test round and proved that the geometry, pose, camera space,
// pipeline and submission are all correct (the clouds appeared as solid red blobs), so it is
// back OFF for normal runs. Set to true to bypass texture/tint/mask/fade and output opaque red.
const bool GAS_DEBUG_OPAQUE = false;
// Second quad style: the packed per-quad channel (UV2.y, cloudSeed.y) carries 0..8 for the sub-quads
// of a gas cloud and GAS_SPEC_STYLE_CHANNEL for a water speck. A speck is drawn as a crisp dark-red
// rectangle, so its branch below skips the texture, the radial falloff and the noise mask entirely.
// Mirrored by GasCloudRenderType.SPEC_STYLE_CHANNEL (integer, compared exactly).
const int GAS_SPEC_STYLE_CHANNEL = 250;
// Base factor of the spec alpha chain: a hard-edged rectangle has no texture alpha to reduce it, so
// the chain is just Color.a * GAS_SPEC_BASE_ALPHA * GAS_ALPHA_BOOST * edgeFade.
// Mirrors GasCloudRenderType.SPEC_BASE_ALPHA.
const float GAS_SPEC_BASE_ALPHA = 1.0;

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 cloudSeed;
in float vertexDistance;

out vec4 fragColor;

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
    float edgeFade = 1.0 - smoothstep(GAS_DISTANCE_FADE_START, GAS_DISTANCE_FADE_END, vertexDistance);

    // ---- style branch: dark-red micro rectangle (water speck) --------------------------------
    // cloudSeed.y is the packed per-quad channel, constant across the quad. A value of
    // GAS_SPEC_STYLE_CHANNEL means "hard-edged rectangle": no texture sample, no radial falloff and
    // no noise mask, just the vertex colour (which carries the speck's dark red and alpha) times
    // the shader's red tint, exactly as the spec asks for. The gas style below is untouched.
    if (int(cloudSeed.y) == GAS_SPEC_STYLE_CHANNEL) {
        float specAlpha = clamp(GAS_SPEC_BASE_ALPHA * GAS_ALPHA_BOOST * vertexColor.a * edgeFade,
                                0.0, 1.0);
        fragColor = vec4(GAS_TINT_RGB * vertexColor.rgb * ColorModulator.rgb,
                         specAlpha * ColorModulator.a);
        return;
    }

    // ---- style: gas puff ---------------------------------------------------------------------
    // The quad spans v in [0, 1]; dividing by the frame count and adding GAS_FRAME_INDEX selects
    // exactly one band of the animation strip (frame 0 by default), so the sampled frame is the
    // untouched original 16x16 sprite.
    vec2 frameUv = vec2(texCoord0.x, (GAS_FRAME_INDEX + texCoord0.y) / GAS_TEXTURE_FRAMES);
    vec4 tex = texture(Sampler0, frameUv);
    vec4 tinted = mix(tex, vec4(GAS_TINT_RGB, tex.a), GAS_TINT_STRENGTH);

    // The radial falloff and the procedural mask are evaluated in normalised quad space
    // (0..1 across the sampled frame), not in strip space. The falloff has a wide plateau so the
    // inner part of the cloud stays at full density and only the rim eases out.
    vec2 centered = texCoord0 - vec2(0.5);
    float radius = clamp(length(centered) * 2.0, 0.0, 1.0);
    float radial = 1.0 - smoothstep(GAS_RADIAL_PLATEAU, 1.0, radius);

    float seed = cloudSeed.x * GAS_SEED_SCALE + cloudSeed.y * GAS_SUBQUAD_SEED_SCALE;
    float mask = gasFbm(texCoord0 * GAS_NOISE_UV_SCALE, seed);
    mask = mix(1.0, mask, GAS_MASK_DEPTH);
    mask = max(mask, GAS_MIN_NOISE_MASK);

    if (GAS_DEBUG_OPAQUE) {
        // POSITIVE CONTROL #1: solid opaque output; the cloud must read as a hard red shape.
        fragColor = vec4(1.0, 0.1, 0.1, 1.0);
    } else {
        // The per-cloud fade rides in the vertex colour alpha, so each cloud fades on its own.
        // See GAS_ALPHA_BOOST for the peak/floor arithmetic of both cloud kinds.
        float base = max(tex.a, GAS_MIN_TEXTURE_ALPHA);
        float alpha = base * radial * mask * GAS_MASK_STRENGTH * vertexColor.a * GAS_ALPHA_BOOST;
        alpha = clamp(alpha, 0.0, 1.0);

        fragColor = vec4(tinted.rgb * vertexColor.rgb * ColorModulator.rgb,
                         alpha * ColorModulator.a * edgeFade);
    }
}
