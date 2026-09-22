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
//
// The same shader draws three further looks, selected by the style channel in gasSeed.y (UV1.y,
// written by the CPU helper that submits the quad):
//   * GAS_SPEC_STYLE_CHANNEL (250): the dark-red micro rectangle of epca:contaminated_water,
//   * HEART_STYLE_CHANNEL (252): the golden plasma/flame column of epca:soul_protection,
//   * HEART_MOTE_STYLE_CHANNEL (253): the tiny golden embers beside that flame.
// The two soul-protection styles are fully procedural: they sample no texture and use no gas noise
// mask, and the flame reuses gasFbm for its structure only.

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

// =================================================================================================
//  Soul-protection style (epca:soul_protection) - SINGLE TUNABLE BLOCK
// =================================================================================================
// The "Heart" style (HEART_STYLE_CHANNEL) draws the golden plasma/flame column of the reference
// image, and the "mote" style (HEART_MOTE_STYLE_CHANNEL) draws the tiny golden embers beside it.
// Every shape, colour and animation number of both styles is a plain literal in this one block, and
// the two look functions soulFlameColor() / soulMoteColor() below are the only consumers, so
// retuning the look means editing this block and its Java mirror while replacing the look entirely
// means replacing those two functions (and, if they need different inputs, this block).
// The block is mirrored one-for-one (same names, same values) by the HEART_* block of
// impl/client/entity/heart/SoulProtectionHeartRenderer, and build/javac-check/check-glsl-26.py
// fails if the two ever drift apart. It is byte-identical to the 1.20.1 twin's block.
//
// Quad space: 1 unit = the quad width, x to the right and y up, the quad spanning x in [-0.5, 0.5]
// and y in [-HEART_ASPECT_HEIGHT/2, +HEART_ASPECT_HEIGHT/2]. The CPU puts the animation phases into
// the vertex colour of each flame/mote quad, which these styles therefore read instead of a tint:
//   vertexColor.r -> flame boil / upward advection phase, 0..1
//   vertexColor.g -> flame sway and horizontal jitter phase, 0..1
//   vertexColor.b -> flame brightness flicker phase, 0..1
//   vertexColor.a -> fade-in / fade-out of the quad (flame and motes alike)
// UV1.x stays unused (the seed slot) and UV1.y carries the style channel.

// -- submission geometry and fade timing (owned by the CPU helper; mirrored, not read here) --------
// Style channels of the flame quad and of the ember quads. Mirrored by
// GasCloudRenderType.HEART_STYLE_CHANNEL / HEART_MOTE_STYLE_CHANNEL (ints, compared exactly).
const int HEART_STYLE_CHANNEL = 252;
const int HEART_MOTE_STYLE_CHANNEL = 253;
// Aspect ratio width : height = 1 : 2.1, and the hitbox padding the CPU applies to the quad height.
const float HEART_ASPECT_HEIGHT = 2.1;
const float HEART_SIZE_PADDING = 1.10;
// Fade in / fade out in ticks (the fade itself rides in vertexColor.a).
const int HEART_FADE_IN_TICKS = 8;
const int HEART_FADE_OUT_TICKS = 15;

// -- column silhouette -----------------------------------------------------------------------------
// Fraction of the quad half extent the column spans (the rest is headroom for the wispy edges). The
// half extents are derived, so the column is always exactly 1 : HEART_ASPECT_HEIGHT.
const float HEART_FIT = 0.76;
const float HEART_HALF_HEIGHT = HEART_FIT * HEART_ASPECT_HEIGHT * 0.5;
const float HEART_HALF_WIDTH = HEART_HALF_HEIGHT / HEART_ASPECT_HEIGHT;
// Spine: an S-curved axis (amplitude in quad units, frequency over the -1..1 vertical coordinate) and
// a taper that narrows the column towards both ends.
const float HEART_BEND = 0.07;
const float HEART_BEND_FREQ = 2.2;
const float HEART_TAPER = 0.45;
const float HEART_CROWN_TAPER = 0.45;
const float HEART_VERTICAL_FADE_START = 0.62;
// Noise fields. gasFbm is the sum of three octaves with amplitudes 0.5 / 0.25 / 0.125, so it spans
// [0, 0.875] around HEART_NOISE_CENTRE; subtracting that centre makes both fields symmetric around
// zero. The coarse field shapes the outline, the fine one (much higher across than up, so it streaks
// vertically) breaks the interior into filaments.
const float HEART_NOISE_CENTRE = 0.4375;
const float HEART_NOISE_SCALE_X = 2.6;
const float HEART_NOISE_SCALE_Y = 1.05;
const float HEART_NOISE_SEED = 2.3;
const float HEART_DETAIL_SCALE_X = 3.4;
const float HEART_DETAIL_SCALE_Y = 0.7;
const float HEART_DETAIL_SEED = 11.9;
// How far the coarse field pushes the outline in and out (1.0 would be a whole half width), and how
// far the fine field modulates the interior brightness (+-HEART_BREAK_STRENGTH around 0.5).
const float HEART_EDGE_NOISE = 0.50;
const float HEART_CROWN_WISP = 0.90;
const float HEART_BREAK_STRENGTH = 0.68;
// Gain that turns the horizontal distance into the 0..1 density ramp: 1.0 keeps the spine at the
// value the noise gives it instead of saturating the whole interior.
const float HEART_EDGE_GAIN = 1.0;

// -- core, ramp, colours and opacities -------------------------------------------------------------
// Bright near-white core on the spine (width, feather, how much the filaments may dim it, and how
// much it boosts the intensity), the four-step ramp core -> mid -> outer -> wisp, and the opacity
// ramp from the faint wisps to the near-opaque core.
const float HEART_CORE_WIDTH = 0.09;
const float HEART_CORE_FEATHER = 0.08;
const float HEART_CORE_MIN = 0.35;
const float HEART_CORE_BOOST = 0.55;
const float HEART_RAMP_WISP = 0.10;
const float HEART_RAMP_OUTER = 0.30;
const float HEART_RAMP_MID = 0.55;
const float HEART_RAMP_CORE = 0.85;
const float HEART_WISP_ALPHA = 0.15;
const float HEART_CORE_ALPHA = 0.95;
// #FFF7CC core, #FFD24A mid, #E08A18 outer, #8A4B08 deepest wisp.
const vec3 HEART_COLOR_CORE = vec3(1.0, 0.9686275, 0.8);
const vec3 HEART_COLOR_MID = vec3(1.0, 0.8235294, 0.2901961);
const vec3 HEART_COLOR_OUTER = vec3(0.8784314, 0.5411765, 0.0941176);
const vec3 HEART_COLOR_WISP = vec3(0.5411765, 0.2941176, 0.0313726);

// -- flame motion ----------------------------------------------------------------------------------
// All motion is a bounded sine/cosine of a 0..1 phase the CPU sends, so the field boils, sways and
// flickers without the jump an unbounded, wrapping offset would cause: how far the noise is
// advected upward and sideways, the column's own sway and jitter, the flicker amplitude and the
// frequency ratio used to break up the pure sine.
const float HEART_FLOW = 1.10;
const float HEART_FLOW_SIDE = 0.26;
const float HEART_SWAY = 0.055;
const float HEART_JITTER = 0.022;
const float HEART_JITTER_RATIO = 3.7;
const float HEART_FLICKER_AMPLITUDE = 0.18;
const float HEART_FLICKER_RATIO = 2.7;
// One full turn, the conversion factor from a 0..1 phase to the angle of the sines above.
const float HEART_TWO_PI = 6.2831853;

// -- embers ----------------------------------------------------------------------------------------
// Gold of the little embers (#FFE27A) and the radius (in the quad's 0..1 UV space) inside which they
// are at full brightness.
const vec3 HEART_MOTE_COLOR = vec3(1.0, 0.8862745, 0.4784314);
const float HEART_MOTE_INNER = 0.25;

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

// =================================================================================================
//  LOOK FUNCTIONS - the single swap points of the two soul-protection styles
// =================================================================================================
// soulFlameColor() gets the isotropic quad coordinate and the three animation phases the CPU packs
// into the vertex colour, and returns the un-premultiplied colour and the opacity of the flame
// column at that point. soulMoteColor() does the same for one ember. Replacing the body of either
// function (plus, if it needs different inputs, the tunable block above) replaces the look: the
// branches in main(), the fade, the additive blending and the gas/speck styles stay untouched.

// Tall, irregular golden plasma column: an S-curved spine, a taper towards both ends, a
// noise-eroded wispy outline with detached tongues, a bright near-white core and a four-step golden
// ramp from that core out to the faint dark-amber wisps.
vec4 soulFlameColor(vec2 s, vec3 phases) {
    float boilAngle = phases.r * HEART_TWO_PI;
    float swayAngle = phases.g * HEART_TWO_PI;
    float flickerAngle = phases.b * HEART_TWO_PI;

    // The spine sways slowly and jitters slightly; both are bounded sines of the phase, so they stay
    // continuous however the phase wraps.
    float sway = HEART_SWAY * sin(swayAngle) + HEART_JITTER * sin(swayAngle * HEART_JITTER_RATIO);

    // The noise field is advected upward by a bounded cosine travel and pushed sideways, so the
    // flame boils and licks without the jump an unbounded, wrapping offset would produce. The coarse
    // field shapes the outline; the fine one streaks vertically and breaks the interior up.
    vec2 flow = vec2(HEART_FLOW_SIDE * sin(boilAngle), -HEART_FLOW * (0.5 - 0.5 * cos(boilAngle)));
    vec2 structureUv = vec2(s.x * HEART_NOISE_SCALE_X, s.y * HEART_NOISE_SCALE_Y) + flow;
    vec2 detailUv = vec2(structureUv.x * HEART_DETAIL_SCALE_X + sway,
                         structureUv.y * HEART_DETAIL_SCALE_Y);
    float structure = gasFbm(structureUv, HEART_NOISE_SEED) - HEART_NOISE_CENTRE;
    float filaments = gasFbm(detailUv, HEART_DETAIL_SEED) - HEART_NOISE_CENTRE;

    // Column profile: v is -1 at the bottom and +1 at the top, the spine is S-curved and the taper
    // narrows the column. The crown term narrows the upper half much harder, and the coarse noise
    // erodes harder towards the top, so the crown pinches off and breaks into detached tongues while
    // the foot stays broad.
    float v = clamp(s.y / HEART_HALF_HEIGHT, -1.0, 1.0);
    float upward = max(v, 0.0);
    float axis = sway + HEART_BEND * sin(v * HEART_BEND_FREQ);
    float taper = 1.0 - HEART_TAPER * v * v - HEART_CROWN_TAPER * upward;
    float halfWidth = max(HEART_HALF_WIDTH * taper, 0.0001);
    float u = abs(s.x - axis) / halfWidth;
    float outline = 1.0 - u + structure * HEART_EDGE_NOISE * (1.0 + HEART_CROWN_WISP * upward);
    float vertical = 1.0 - smoothstep(HEART_VERTICAL_FADE_START, 1.0, abs(v));
    // The fine filaments cut the interior of the column into bright streaks and darker lanes, so the
    // flame never reads as a solid slab.
    float breakup = clamp(0.5 + HEART_BREAK_STRENGTH * filaments / HEART_NOISE_CENTRE, 0.0, 1.0);
    float density = clamp(outline * HEART_EDGE_GAIN, 0.0, 1.0) * breakup * vertical;

    // Bright core: a narrow band on the spine, broken up by the same filaments so it reads as a
    // flickering filament rather than a uniform stripe.
    float coreBand = 1.0 - smoothstep(HEART_CORE_WIDTH, HEART_CORE_WIDTH + HEART_CORE_FEATHER, abs(s.x - axis));
    float coreFilaments = clamp(HEART_CORE_MIN + (1.0 - HEART_CORE_MIN) * (0.5 + filaments / HEART_NOISE_CENTRE), 0.0, 1.0);
    float core = coreBand * vertical * coreFilaments;
    float intensity = clamp(density + core * HEART_CORE_BOOST, 0.0, 1.0);

    // Four-step golden ramp, from the faintest wisp outwards in to the near-white core.
    vec3 color = HEART_COLOR_WISP;
    color = mix(color, HEART_COLOR_OUTER, smoothstep(HEART_RAMP_WISP, HEART_RAMP_OUTER, intensity));
    color = mix(color, HEART_COLOR_MID, smoothstep(HEART_RAMP_OUTER, HEART_RAMP_MID, intensity));
    color = mix(color, HEART_COLOR_CORE, smoothstep(HEART_RAMP_MID, HEART_RAMP_CORE, intensity));

    // Brightness flicker of about +-HEART_FLICKER_AMPLITUDE, and the opacity ramp that keeps the
    // wisps faint (HEART_WISP_ALPHA) and the core near opaque (HEART_CORE_ALPHA) while fading
    // completely to zero outside the column.
    float flicker = 0.6 * sin(flickerAngle) + 0.4 * sin(flickerAngle * HEART_FLICKER_RATIO);
    color *= 1.0 + HEART_FLICKER_AMPLITUDE * flicker;
    float alpha = mix(HEART_WISP_ALPHA, HEART_CORE_ALPHA, intensity) * smoothstep(0.0, HEART_RAMP_WISP, intensity);
    return vec4(color, clamp(alpha, 0.0, 1.0));
}

// One tiny golden ember: a soft round blob in the quad's 0..1 UV space. The CPU has already placed
// the quad and put the ember's own fade into the vertex colour alpha.
vec4 soulMoteColor(vec2 uv) {
    float distanceFromCentre = length(uv - vec2(0.5)) * 2.0;
    float falloff = 1.0 - smoothstep(HEART_MOTE_INNER, 1.0, distanceFromCentre);
    return vec4(HEART_MOTE_COLOR, falloff);
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

    // SOUL PROTECTION FLAME: gasSeed.y == HEART_STYLE_CHANNEL marks the single flame quad the CPU
    // helper submits for a living entity that carries epca:soul_protection. vertexColor.rgb carries
    // the three animation phases (boil, sway, flicker) and vertexColor.a the quad's fade; UV1.x is
    // unused here. The look itself lives in soulFlameColor(), the single swap point of this style.
    // 250, 252 and 253 are all far above every real gas sub-quad index (0..8), so the styles cannot
    // collide.
    if (gasSeed.y == HEART_STYLE_CHANNEL) {
        // Isotropic quad space: 1 unit = the quad width, x right and y up, so a feature has the same
        // size in both directions.
        vec2 s = vec2(texCoord0.x - 0.5, (0.5 - texCoord0.y) * HEART_ASPECT_HEIGHT);
        vec4 flame = soulFlameColor(s, vertexColor.rgb);
        fragColor = vec4(flame.rgb * ColorModulator.rgb,
                         flame.a * vertexColor.a * edgeFade * ColorModulator.a);
        return;
    }

    // SOUL PROTECTION EMBER: one tiny ember per quad, placed by the CPU helper; vertexColor.a is
    // that ember's own fade.
    if (gasSeed.y == HEART_MOTE_STYLE_CHANNEL) {
        vec4 mote = soulMoteColor(texCoord0);
        fragColor = vec4(mote.rgb * ColorModulator.rgb,
                         mote.a * vertexColor.a * edgeFade * ColorModulator.a);
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
