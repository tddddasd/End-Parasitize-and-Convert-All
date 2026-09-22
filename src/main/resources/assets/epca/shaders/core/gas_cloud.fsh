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
// The same shader draws three further looks, selected by the style channel in cloudSeed.y (UV2.y,
// written by the CPU helper that submits the quad):
//   * GAS_SPEC_STYLE_CHANNEL (250): the dark-red micro rectangle of epca:contaminated_water,
//   * HEART_STYLE_CHANNEL (252): the golden plasma/flame column of epca:soul_protection,
//   * HEART_MOTE_STYLE_CHANNEL (253): the tiny golden embers beside that flame.
// The two soul-protection styles are fully procedural: they sample no texture and use no gas noise
// mask, and the flame reuses gasFbm for its structure only.
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
// impl/client/entity/heart/SoulProtectionHeartRenderer, and build/javac-check/check-glsl.py fails if
// the two ever drift apart.
//
// Quad space: 1 unit = the quad width, x to the right and y up, the quad spanning x in [-0.5, 0.5]
// and y in [-HEART_ASPECT_HEIGHT/2, +HEART_ASPECT_HEIGHT/2]. The CPU puts the animation phases into
// the vertex colour of each flame/mote quad, which these styles therefore read instead of a tint:
//   vertexColor.r -> flame boil / upward advection phase, 0..1
//   vertexColor.g -> flame sway and horizontal jitter phase, 0..1
//   vertexColor.b -> flame brightness flicker phase, 0..1
//   vertexColor.a -> fade-in / fade-out of the quad (flame and motes alike)
// UV2.x stays unused (the seed slot) and UV2.y carries the style channel.

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
// the taper that narrows the column. HEART_TAPER narrows both ends symmetrically, HEART_CROWN_TAPER
// additionally narrows the upper half much harder, so the crown pinches off and dissolves into wisps
// while the foot stays broad.
const float HEART_BEND = 0.07;
const float HEART_BEND_FREQ = 2.2;
const float HEART_TAPER = 0.45;
const float HEART_CROWN_TAPER = 0.45;
const float HEART_VERTICAL_FADE_START = 0.62;
// Noise fields. gasFbm is the sum of three octaves with amplitudes 0.5 / 0.25 / 0.125, so it spans
// [0, 0.875] around HEART_NOISE_CENTRE; subtracting that centre makes both fields symmetric around
// zero. The coarse field shapes the outline, the fine one breaks the interior into filaments. Both Y
// scales are deliberately far below their X scales: that is what stretches the features into long
// vertical tongues.
const float HEART_NOISE_CENTRE = 0.4375;
const float HEART_NOISE_SCALE_X = 2.6;
const float HEART_NOISE_SCALE_Y = 1.05;
const float HEART_NOISE_SEED = 2.3;
const float HEART_DETAIL_SCALE_X = 3.4;
const float HEART_DETAIL_SCALE_Y = 0.7;
const float HEART_DETAIL_SEED = 11.9;
// How far the coarse field pushes the outline in and out (1.0 would be a whole half width), how much
// harder it erodes the crown (the factor grows with the upward coordinate), and how far the fine
// field modulates the interior brightness (+-HEART_BREAK_STRENGTH around 0.5).
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
// frequency ratio used to break up the pure sine. HEART_FLOW is the upward travel of the whole noise
// field over one boil cycle and is the constant that makes the flame read as rising.
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

    // ---- style branch: golden soul-protection flame --------------------------------------------
    // cloudSeed.y == HEART_STYLE_CHANNEL marks the single flame quad the CPU helper submits for a
    // living entity that carries epca:soul_protection. vertexColor.rgb carries the three animation
    // phases (boil, sway, flicker) and vertexColor.a the quad's fade; UV2.x is unused here. The look
    // itself lives in soulFlameColor(), which is the single swap point of this style.
    if (int(cloudSeed.y) == HEART_STYLE_CHANNEL) {
        // Isotropic quad space: 1 unit = the quad width, x right and y up, so a feature has the same
        // size in both directions.
        vec2 s = vec2(texCoord0.x - 0.5, (0.5 - texCoord0.y) * HEART_ASPECT_HEIGHT);
        vec4 flame = soulFlameColor(s, vertexColor.rgb);
        fragColor = vec4(flame.rgb * ColorModulator.rgb,
                         flame.a * vertexColor.a * edgeFade * ColorModulator.a);
        return;
    }

    // ---- style branch: golden soul-protection ember --------------------------------------------
    // One tiny ember per quad, placed by the CPU helper; vertexColor.a is that ember's own fade.
    if (int(cloudSeed.y) == HEART_MOTE_STYLE_CHANNEL) {
        vec4 mote = soulMoteColor(texCoord0);
        fragColor = vec4(mote.rgb * ColorModulator.rgb,
                         mote.a * vertexColor.a * edgeFade * ColorModulator.a);
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
