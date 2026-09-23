#version 330

// EPCA sky rupture (fragment stage), 26.1.2 core shader.
//
// What it draws
// ---------------------------------------------------------------------------
// The world barrier ("sky rupture") breaking apart. The timeline is
//   1. darkening: darkness spreads down from the zenith until it passes the
//      horizon. Break progress is identically 0 here, so no crack can appear.
//   2. rupture: cracks open all over the sky, the gap widens and the cosmos
//      behind the barrier becomes visible. At the end the whole sky is gone.
//
// Cracks are the F2 - F1 field of a Voronoi network. That field is 0 exactly on
// a cell boundary (a shard edge), is continuous across the boundary (F1 and F2
// swap, the difference keeps growing from 0) and therefore cannot produce the
// straight seams an earlier grid-jitter implementation suffered from. Which
// region breaks first is decided by a low frequency continuous noise field, so
// the rupture spreads continuously and never jumps.
//
// The pattern space is the stereographic projection p = dir.xz / (1 + dir.y)
// with |p| = tan(theta/2) * SKY_SCALE (0 at the zenith, SKY_SCALE at the
// horizon). An earlier p = dir.xz * const mirrored the lower hemisphere onto the
// upper one, which put a fold (a derivative flip) exactly on the horizon.
//
// Everything is computed from the world direction, so the pattern is locked to
// the sky rather than to the screen, and the depth mask keeps it on sky pixels.
//
// 1.20.1 -> 26.1.2 port notes
// ---------------------------------------------------------------------------
// * Uniforms became varyings/attributes: see sky_rupture.vsh. rayUp is
//   reconstructed with cross() because the camera basis is orthonormal
//   (right x forward = up for a right-handed, Y-up basis).
// * The rim/void/flash colours are reconstructed from breakAmount with the same
//   lerp chain the Java side used (SkyRuptureEffect.rimRed() and friends), so
//   they are still driven by the evolution stage.
// * `uniform mat2 cosmicuvs[12]` is gone entirely. 1.20.1 read the 12 animated
//   atlas rectangles out of that array; here each draw binds ONE sprite as its
//   own direct texture on Sampler0, so a sprite's UV space is simply [0,1]^2 and
//   the only residual per-sprite constants are its frame count (COSMIC_FRAMES)
//   and its full animation timeline (COSMIC_SCHEDULE_BEGIN / COSMIC_STEP_FRAME /
//   COSMIC_STEP_HOLD / COSMIC_CYCLE), because a directly bound texture is the raw
//   strip and nothing animates it. Zero vertex slots are spent on sprite
//   geometry.
// * Sampler0 is therefore one `epca:shader/cosmic_N` strip per draw, not the
//   block atlas. `cosmicBase` (from the vertex stage) names which shell - and so
//   which sprite - this draw owns.
// * The fade envelope also rides in the vertex colour alpha
//   (ColorModulator.a), which is 1.0; both are applied, matching 1.20.1 where
//   the shader applied `fade` itself.

#moj_import <minecraft:dynamictransforms.glsl>

/** Star shells rendered by one draw (mirrors SkyRuptureShaders). */
const int SPRITES_PER_PASS = 1;

in vec2 ndcPos;

in vec3 rayForward;
in vec3 rayRight;
// The 1.20.1 twin had a rayUp uniform; 26.1.2 carries the camera up vector in
// the vertex attribute instead (see sky_rupture.vsh).
in vec3 rayUp;
in float time;
in float progress;
in float breakAmount;
in float fade;
in vec2 patternOffset;
in float skyDarkProgress;
in float skyDarkOpacity;

flat in int cosmicBase;

uniform sampler2D Sampler0;
/**
 * Frame counts of the 12 star sprite strips, baked at compile time.
 *
 * 1.20.1 put all 12 sprites into the block atlas and read their animated UV rectangles from a
 * `mat2 cosmicuvs[12]` uniform. That is 24 floats, which does not fit the vertex budget here, so each
 * draw instead binds ONE sprite as its own direct texture (the same pattern the gas cloud uses): the
 * sprite's UV space is then just [0,1]^2 and the only per-sprite constant left is how many 16x16
 * frames its strip contains. Those counts are fixed by the shipped PNGs
 * (assets/epca/textures/shader/cosmic_N.png heights 64,64,80,80,64,64,96,64,112,48,112,16 divided by
 * 16), so they are baked as a const array and indexed by the shell index.
 */
const int COSMIC_COUNT = 12;
const float COSMIC_FRAMES[COSMIC_COUNT] = float[COSMIC_COUNT](
    4.0, 4.0, 5.0, 5.0, 4.0, 4.0, 6.0, 4.0, 7.0, 3.0, 7.0, 1.0
);

/**
 * Why the frame is looked up by WALKING a timeline instead of dividing the clock by one rate.
 *
 * Seven of the twelve strips carry an explicit per-entry schedule in their `.mcmeta` (band:hold, six
 * of them with several separate stretches of band 0, and cosmic_8 starts on band 1):
 *   cosmic_0  0:7 1 2 3
 *   cosmic_1  0:4 1 0:9 2 0:7 3
 *   cosmic_2  0:16 1 1 1 2 2 3 4 3 4 3 2 2 1 1 1
 *   cosmic_3  0:13 1 0:10 3 0:5 2 0:15 4
 *   cosmic_4  0:34 1 2 3
 *   cosmic_5  0:18 1 0:4 3 0:14 2
 *   cosmic_8  1 2 3 2 3 2 1 0:22 4 5 6 5 6 5 4 0:31 1 2 3 2 1 0:12
 * The other five are uniform: cosmic_6 and cosmic_11 use `frametime: 1`, cosmic_7 and cosmic_9
 * `frametime: 2`, cosmic_10 `frametime: 3`. `floor(t / rate) % frames` can express a uniform strip
 * but not a scheduled one: it under-holds band 0 and drops every later repeat of it, which is the
 * star-detail mismatch this port had. The baked tables reproduce a uniform strip as just another
 * timeline, so there is a single code path.
 *
 * Known limit (documented, not fixed here): the walk is driven by the `time` varying, which the
 * renderer fills with `(float) (gameTime % Integer.MAX_VALUE)` - world ticks, the same unit the
 * `.mcmeta` uses. A float32 represents every integer up to 2^24 = 16777216 ticks, i.e. about 9.7 days
 * of ticking; beyond that its spacing grows (2 ticks at 3.4e7, 8 ticks at 1.3e8) and the phase of the
 * walk coarsens with it. `time` already drives the crack animation and the star twinkle, so this is a
 * pre-existing property of the payload; removing it would need the renderer to reduce the phase
 * exactly before it is quantised to a float, and the 32-byte vertex format has no free slot for it.
 * The atlas ticker was immune because its counter lived in the SpriteContents.Ticker, not in a float.
 */
// >>> EPCA-BAKED-SKY-SCHEDULE
/**
 * Per-strip animation timelines, parsed from the 12 `cosmic_N.png.mcmeta` files.
 *
 * 1.20.1 put the strips in the block atlas, so SpriteContents.Ticker animated them: it
 * held list entry p for that entry's own `time` ticks and then moved to entry p+1,
 * wrapping at the end of the list, and uploaded band `index` of the strip for it. A
 * directly bound texture is the raw strip with no animation applied, so the same
 * timeline is walked here. The tables below hold it verbatim:
 *
 *   COSMIC_SCHEDULE_BEGIN[i] .. COSMIC_SCHEDULE_BEGIN[i + 1]  slice of strip i
 *   COSMIC_STEP_FRAME[s] = the band index step s shows (`index` in the .mcmeta)
 *   COSMIC_STEP_HOLD[s]  = the ticks step s is held (`time`, or `frametime`)
 *   COSMIC_CYCLE[i]      = the sum of strip i's holds, i.e. its period in ticks
 *
 * Strip 6 is the plain `frametime: 1` sequence, strips 7 and 9 are `frametime: 2`, strip
 * 10 is `frametime: 3` and strip 11 is a single frame, so the uniform and the static
 * strips fall out of the same representation as the scheduled ones. Strip 8
 * starts on band 1, not band 0, and every strip whose .mcmeta repeats `index: 0` holds
 * band 0 for several separate stretches, which is why the timeline cannot be reduced to
 * one rate per strip.
 *
 * None of the twelve files sets `interpolate`, so vanilla never cross-faded two
 * consecutive entries; no blending is reproduced here. `index: 0` repeats are held, not
 * faded.
 *
 * Parsed timelines, one line per strip (`band:hold` per step, cycle in ticks):
 *   cosmic_0  0:7 1:1 2:1 3:1 cycle=10
 *   cosmic_1  0:4 1:1 0:9 2:1 0:7 3:1 cycle=23
 *   cosmic_2  0:16 1:1 1:1 1:1 2:1 2:1 3:1 4:1 3:1 4:1 3:1 2:1 2:1 1:1 1:1 1:1 cycle=31
 *   cosmic_3  0:13 1:1 0:10 3:1 0:5 2:1 0:15 4:1 cycle=47
 *   cosmic_4  0:34 1:1 2:1 3:1 cycle=37
 *   cosmic_5  0:18 1:1 0:4 3:1 0:14 2:1 cycle=39
 *   cosmic_6  0:1 1:1 2:1 3:1 4:1 5:1 cycle=6
 *   cosmic_7  0:2 1:2 2:2 3:2 cycle=8
 *   cosmic_8  1:1 2:1 3:1 2:1 3:1 2:1 1:1 0:22 4:1 5:1 6:1 5:1 6:1 5:1 4:1 0:31 1:1 2:1 3:1 2:1 1:1 0:12 cycle=84
 *   cosmic_9  0:2 1:2 2:2 cycle=6
 *   cosmic_10 0:3 1:3 2:3 3:3 4:3 5:3 6:3 cycle=21
 *   cosmic_11 0:1 cycle=1
 */
const int COSMIC_STEP_TOTAL = 87;
const int COSMIC_STEP_MAX = 22;
const int COSMIC_SCHEDULE_BEGIN[COSMIC_COUNT + 1] = int[COSMIC_COUNT + 1](
    0, 4, 10, 26, 34, 38, 44, 50, 54, 76, 79, 86,
    87
);
const int COSMIC_STEP_FRAME[COSMIC_STEP_TOTAL] = int[COSMIC_STEP_TOTAL](
    0, 1, 2, 3, 0, 1, 0, 2, 0, 3, 0, 1,
    1, 1, 2, 2, 3, 4, 3, 4, 3, 2, 2, 1,
    1, 1, 0, 1, 0, 3, 0, 2, 0, 4, 0, 1,
    2, 3, 0, 1, 0, 3, 0, 2, 0, 1, 2, 3,
    4, 5, 0, 1, 2, 3, 1, 2, 3, 2, 3, 2,
    1, 0, 4, 5, 6, 5, 6, 5, 4, 0, 1, 2,
    3, 2, 1, 0, 0, 1, 2, 0, 1, 2, 3, 4,
    5, 6, 0
);
const int COSMIC_STEP_HOLD[COSMIC_STEP_TOTAL] = int[COSMIC_STEP_TOTAL](
    7, 1, 1, 1, 4, 1, 9, 1, 7, 1, 16, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 13, 1, 10, 1, 5, 1, 15, 1, 34, 1,
    1, 1, 18, 1, 4, 1, 14, 1, 1, 1, 1, 1,
    1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1,
    1, 22, 1, 1, 1, 1, 1, 1, 1, 31, 1, 1,
    1, 1, 1, 12, 2, 2, 2, 3, 3, 3, 3, 3,
    3, 3, 1
);
const int COSMIC_CYCLE[COSMIC_COUNT] = int[COSMIC_COUNT](
    10, 23, 31, 47, 37, 39, 6, 8, 84, 6, 21, 1
);
// <<< EPCA-BAKED-SKY-SCHEDULE

/**
 * Band index strip `sprite` shows at world tick `t`, found by walking its baked timeline.
 *
 * `t` is the tick clock (`time`), which is exactly the unit `.mcmeta` `time`/`frametime` are counted
 * in. Entry 0 of the timeline is on screen at tick 0 - vanilla's `uploadFirstFrame` uploads
 * `frames.get(0).index` before the first tick - and the timeline advances to the next entry once the
 * current entry's hold has elapsed, wrapping at `COSMIC_CYCLE`. That is what vanilla's ticker did to
 * the atlas each tick:
 *
 *   ++subFrame; if (subFrame >= frames[frame].time) { frame = (frame + 1) % frames.size(); subFrame = 0; }
 *
 * and because it then uploaded `frames[frame].index`, an entry's own `index` (not its list position)
 * names the band it shows.
 */
int cosmicFrameAt(int sprite, float t) {
    int cycle = COSMIC_CYCLE[sprite];
    // Reduce the tick clock into one period, clamped on both ends: a float division that lands on an
    // exact integer can make mod() return the cycle length (the last tick of the cycle, not an error)
    // or a tiny negative (tick 0). Integer-valued inputs make the result exact.
    float reduced = clamp(mod(t, float(cycle)), 0.0, float(cycle) - 1.0);
    int phase = int(reduced);
    int first = COSMIC_SCHEDULE_BEGIN[sprite];
    int last = COSMIC_SCHEDULE_BEGIN[sprite + 1];
    int held = 0;
    int band = COSMIC_STEP_FRAME[first];
    for (int s = 0; s < COSMIC_STEP_MAX; s++) {
        int step = first + s;
        if (step >= last) {
            break;
        }
        held += COSMIC_STEP_HOLD[step];
        if (phase < held) {
            band = COSMIC_STEP_FRAME[step];
            break;
        }
    }
    return band;
}

out vec4 fragColor;

const int cosmiccount = COSMIC_COUNT;

const float PI = 3.14159265359;
/**
 * Stereographic scale: |p| = tan(theta/2) * SKY_SCALE, so the horizon
 * (theta = 90 deg) lands on |p| = SKY_SCALE = 2.2 and the zenith on 0.
 */
const float SKY_SCALE = 2.2;
/** Spherical grid density of the star field (the reference project used 16). */
const float STAR_UVTILES = 16.0;
/** Star shell count (the reference project used 16; 12 here, one per sprite). */
const int STAR_SHELLS = 12;

/**
 * Crack network density: shard size = 1 / CRACK_SCALE.
 * 4.0 puts one shard at about 0.25 p, i.e. 100-200 px, so a dozen shards are
 * visible on screen at once.
 */
const float CRACK_SCALE = 4.0;

// -- hash / noise -----------------------------------------------------------

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

/** 2D hash, used for the Voronoi feature points. */
vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float hash31(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise3D(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash31(i + vec3(0.0, 0.0, 0.0)), hash31(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash31(i + vec3(0.0, 1.0, 0.0)), hash31(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash31(i + vec3(0.0, 0.0, 1.0)), hash31(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash31(i + vec3(0.0, 1.0, 1.0)), hash31(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

float fbm3(vec3 p) {
    float f = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 3; i++) {
        f += amp * noise3D(p);
        p *= 2.03;
        amp *= 0.5;
    }
    return f;
}

float vnoise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

/**
 * Multi-octave value noise (5 octaves, amplitude halving).
 *
 * baseFreq is "periods per p unit" and sets the pattern scale directly: a p unit
 * is large (400-800 px), so too small a baseFreq turns into one big blur. It is
 * only used for the regional difference (which shard opens first); the main
 * structure uses 1.6, i.e. 270-480 px.
 */
float fbmNoise(vec2 p, float baseFreq) {
    return vnoise2(p * baseFreq)        * 0.500
         + vnoise2(p * baseFreq * 2.0)  * 0.250
         + vnoise2(p * baseFreq * 4.0)  * 0.125
         + vnoise2(p * baseFreq * 8.0)  * 0.062
         + vnoise2(p * baseFreq * 16.0) * 0.031;
}

/**
 * Voronoi F1 / F2: distance to the nearest and second nearest feature point,
 * returned as vec2(f1, f2).
 *
 * Their difference f2 - f1 is the crack field: 0 on a shard boundary and rising
 * linearly away from it. The difference is a continuous field (across a boundary
 * f1 and f2 swap and the difference keeps growing from 0), so the shards never
 * show a jump or a straight seam.
 */
vec2 voronoiEdge(vec2 x) {
    vec2 n = floor(x);
    vec2 f = fract(x);

    float f1 = 8.0;
    float f2 = 8.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 g = vec2(float(i), float(j));
            vec2 o = hash22(n + g);
            vec2 r = g + o - f;
            float d = dot(r, r);
            if (d < f1) {
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f2 = d;
            }
        }
    }
    return vec2(sqrt(f1), sqrt(f2));
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

/** Rotation about an arbitrary axis (each star shell uses its own, giving parallax). */
mat3 rotAxis(vec3 axis, float angle) {
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;
    return mat3(
        oc * axis.x * axis.x + c,          oc * axis.x * axis.y - axis.z * s, oc * axis.z * axis.x + axis.y * s,
        oc * axis.x * axis.y + axis.z * s, oc * axis.y * axis.y + c,          oc * axis.y * axis.z - axis.x * s,
        oc * axis.z * axis.x - axis.y * s, oc * axis.y * axis.z + axis.x * s, oc * axis.z * axis.z + c
    );
}

/** Source-over compositing. */
void over(inout vec3 dst, inout float da, vec3 src, float sa) {
    sa = clamp(sa, 0.0, 1.0);
    float na = sa + da * (1.0 - sa);
    if (na > 0.0001) {
        dst = (src * sa + dst * da * (1.0 - sa)) / na;
    }
    da = na;
}

/**
 * Inverse stereographic projection: back from the zenith plane parameter to a
 * world direction. |q| = tan(theta/2) * SKY_SCALE -> theta = 2 * atan(|q| /
 * SKY_SCALE). Both hemispheres are restored correctly, so there is no
 * "mirrored lower hemisphere" distortion.
 */
vec3 dirFromSkyPlane(vec2 q) {
    vec2 v = q / SKY_SCALE;
    float r = length(v);
    float theta = 2.0 * atan(r);
    vec2 xz = r > 1.0e-5 ? v / r : vec2(1.0, 0.0);
    return vec3(xz * sin(theta), cos(theta));
}

/** ACES approximation: highlights roll off instead of clipping to white. */
vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

// -- nebula (ported from the reference project's cosmic.fsh getFbmNebula,
//    evaluated on the world direction instead of on screen space) ------------

vec3 nebula(vec3 d, float t) {
    float n1 = fbm3(d * 1.7 + vec3(t * 0.03, 0.0, t * 0.02));
    float n2 = fbm3(d * 3.1 + vec3(33.7, -17.2, t * 0.04));
    float k = smoothstep(0.08, 0.85, n1 * 0.65 + n2 * 0.30);

    vec3 c0 = vec3(0.020, 0.005, 0.050);
    vec3 c1 = vec3(0.070, 0.015, 0.170);
    vec3 c2 = vec3(0.140, 0.030, 0.300);
    vec3 c3 = vec3(0.210, 0.075, 0.400);
    vec3 col = k < 0.3 ? mix(c0, c1, k / 0.3)
             : (k < 0.65 ? mix(c1, c2, (k - 0.3) / 0.35) : mix(c2, c3, (k - 0.65) / 0.35));

    col += hsv2rgb(vec3(0.78 + n2 * 0.06 - 0.03, 0.5 + n1 * 0.3, k * 0.30)) * 0.18;
    return col;
}

// -- star field (ported from the reference project's cosmic renderer) --------

/**
 * Samples the sprite strip bound as this draw's Sampler0 at sprite-local (ru, rv).
 *
 * The strip is `frames` texels tall, so the requested frame is selected by mapping rv into its band.
 * `Sampler0` is one single sprite texture per draw (see SkyRuptureShaders), which is what removes the
 * need to carry any atlas rectangle in the vertex stream.
 *
 * `rv` is clamped just below 1 so the last texel of the band is selected instead of the first texel of
 * the next band: the atlas had the same edge (orv = 1 sampled the top row of whatever sprite followed
 * in the atlas), and NEAREST filtering with the default REPEAT wrap would otherwise read across bands.
 */
vec4 sampleCosmicSprite(float ru, float rv, int sprite, float t) {
    float frames = COSMIC_FRAMES[sprite];
    // The band comes from the strip's own timeline (see cosmicFrameAt); band 0 is the top band of the
    // PNG, which is the band the atlas uploaded into the sprite's v0..v1 rect for frame index 0.
    float band = float(cosmicFrameAt(sprite, t));
    return texture(Sampler0, vec2(ru, (band + clamp(rv, 0.0, 0.999)) / frames));
}

/**
 * Accumulates the star sprites of the shells carried by this draw.
 *
 * `firstSprite` is the shell index of this draw and, because the shell-to-sprite
 * mapping is the identity, also the sprite whose strip this draw's Sampler0 binds.
 * Each draw renders SPRITES_PER_PASS shells.
 */
vec3 cosmicStars(vec3 dir, float t, int firstSprite) {
    vec3 acc = vec3(0.0);

    for (int k = 0; k < SPRITES_PER_PASS; k++) {
        int i = firstSprite + k;

        int mult = STAR_SHELLS - i;
        int j = i + 7;
        float rand1 = (float(j * j * 4321 + j * 8)) * 2.0;
        float rand2 = (float((j + 1) * (j + 1) * (j + 1) * 239 + (j + 1) * 37)) * 3.6;
        float rand3 = rand1 * 347.4 + rand2 * 63.4;

        vec3 axis = normalize(vec3(sin(rand1), sin(rand2), cos(rand3)) + vec3(0.001));
        vec3 ray = rotAxis(axis, mod(rand3, 6.2831853)) * dir;

        float rawu = 0.5 + atan(ray.z, ray.x) * 0.1591549431;
        float rawv = 0.5 + asin(clamp(ray.y, -1.0, 1.0)) * 0.3183098862;

        float scale = float(mult) * 0.5 + 2.75;
        float u = rawu * scale;
        float v = (rawv + t * 0.004) * scale * 0.6;

        int tu = int(mod(floor(u * STAR_UVTILES), STAR_UVTILES));
        int tv = int(mod(floor(v * STAR_UVTILES), STAR_UVTILES));
        // The 1.20.1 twin derived the sprite index from the same hash chain
        // (`position = (171*tu + 489*tv + 303*(i+31) + 17209) ^ 10;
        //  symbol = position % cosmicoutof`). That hash chain is deterministic in
        // the shell index, so the index can be supplied directly instead; the
        // per-shell randomness below is unchanged.
        int symbol = firstSprite + k;

        // Rotation / flip come from a hash rather than pow(tu, tv); pow(0, 0) is
        // undefined in GLSL.
        float rotH = hash21(vec2(float(tu) + 0.5, float(tv) + 0.5) + float(i) * 17.3);
        int rotation = int(floor(rotH * 8.0));
        bool flip = false;
        if (rotation >= 4) {
            rotation -= 4;
            flip = true;
        }

        // `symbol` is the shell index by construction, so the 1.20.1
        // `symbol >= 0 && symbol < cosmiccount` guard is provably true here and
        // is kept only as documentation of the original bound.
        if (symbol >= 0 && symbol < cosmiccount) {
            float ru = clamp(mod(u, 1.0) * STAR_UVTILES - float(tu), 0.0, 1.0);
            float rv = clamp(mod(v, 1.0) * STAR_UVTILES - float(tv), 0.0, 1.0);
            if (flip) {
                ru = 1.0 - ru;
            }
            float oru = ru;
            float orv = rv;
            if (rotation == 1) {
                oru = 1.0 - rv;
                orv = ru;
            } else if (rotation == 2) {
                oru = 1.0 - ru;
                orv = 1.0 - rv;
            } else if (rotation == 3) {
                oru = rv;
                orv = 1.0 - ru;
            }

            vec4 texel = sampleCosmicSprite(oru, orv, i, t);

            // The sprite texture's red channel is the brightness; fade out near
            // the poles, where the spherical parameterisation crowds together.
            float a = texel.r * (0.5 + 1.0 / float(mult))
                    * (1.0 - smoothstep(0.15, 0.48, abs(rawv - 0.5)));

            // Cold white temperature (the reference project's DEEP_SPACE branch).
            vec3 starC = vec3(fract(rand1 * 0.123) * 0.4 + 0.6,
                              fract(rand2 * 0.456) * 0.3 + 0.7,
                              fract(rand3 * 0.789) * 0.3 + 0.7);
            starC *= vec3(1.0 + mod(rand1, 20.0) / 500.0,
                          1.0 + mod(rand2, 20.0) / 500.0,
                          1.0 + mod(rand3, 20.0) / 500.0);

            float twinkle = sin(t * 1.6 + rand1 * 0.1) * sin(t * 1.1 + rand2 * 0.15) * 0.4 + 0.6;
            float distFade = 1.0 - float(i) / float(STAR_SHELLS + 4);
            starC = starC * twinkle * distFade;
            starC += vec3(0.12, 0.06, 0.30) * (1.0 - distFade) * 0.3;

            acc += starC * a;
        }
    }
    return acc;
}

void main() {
    // -- world direction -> stereographic parameter (no fold at the horizon) --
    vec3 dir = normalize(rayForward + rayRight * ndcPos.x + rayUp * ndcPos.y);
    vec2 p = dir.xz / (1.0 + dir.y) * SKY_SCALE;
    float t = time;

    // Hard gate: during the darkening phase progress is identically 0, so not a
    // single crack may appear.
    float erupt = smoothstep(0.0, 0.02, progress);

    // -- opening time per region: a low frequency CONTINUOUS noise field (not a
    //    per-cell hash, which would jump) ------------------------------------
    float ignField = fbmNoise(p + patternOffset, 1.6);
    float ign = 0.10 + 0.55 * ignField;
    float localProg = clamp((progress - ign) / max(1.0 - ign, 1.0e-3), 0.0, 1.0) * erupt;

    // -- crack network: Voronoi F2 - F1 (continuous field -> no straight seams)
    vec2 ve = voronoiEdge(p * CRACK_SCALE + patternOffset * 0.37);
    float edge = ve.y - ve.x;                       // 0 = shard boundary

    // Gap width = a thin line that is always visible + a hole that opens with
    // progress:
    //   thin line 0.06 Voronoi units ~ 9 px
    //   hole      finally 1.75 units, which must exceed the maximum of edge
    //             (about 1.0-1.2, plus the +/-30% widthJitter), so that in the
    //             last phase the whole sky really does break up into cosmos
    float widthJitter = 0.70 + 0.60 * vnoise2(p * 5.0 + patternOffset);
    float stageScale = mix(0.10, 1.0, breakAmount);   // lower stages open less
    float lineW = 0.060 * smoothstep(0.0, 0.12, localProg);
    float holeW = 1.75 * pow(localProg, 2.2) * stageScale;
    float wGap = (lineW + holeW) * widthJitter;

    // The antialiasing bandwidth has to follow the gap width, and the whole mask
    // is multiplied by the erupt gate: with a fixed aa of 0.022 a +/-0.022 band
    // would still exist at wGap = 0, leaving an alpha ~0.5 "star line" about
    // 3 px wide on every shard boundary, i.e. the whole shard net would show the
    // instant the effect triggers, before the sky has even finished darkening.
    float aa = max((lineW + holeW) * 0.30, 1.0e-4);
    float gapMask = (1.0 - smoothstep(wGap - aa, wGap + aa, edge)) * erupt;

    // Barrier energy rim on the shard side. The trailing factor keeps unbroken
    // regions dark; without it the whole shard net lights up at once and reads
    // as a mesh instead of as regions opening one after another.
    float rimW = clamp(wGap * 0.55 + 0.06, 0.02, 0.30);
    float rimBand = (smoothstep(wGap, wGap + aa * 2.0, edge)
                   * (1.0 - smoothstep(wGap, wGap + rimW, edge))
                   * smoothstep(0.0, 0.02, wGap)) * erupt;

    // -- the cosmos behind the gap (parallax from a smooth direction field, not
    //    from a discrete rupture point) --------------------------------------
    vec2 warp = vec2(vnoise2(p * 0.6 + patternOffset), vnoise2(p * 0.6 + patternOffset + 31.0)) - 0.5;
    vec3 cosmosDir = dirFromSkyPlane(p + warp * 0.20 * breakAmount);

    // One draw per star shell: this draw owns the shell named by cosmicBase, and
    // its Sampler0 is that sprite's own texture strip.
    vec3 starAcc = cosmicStars(cosmosDir, t, cosmicBase);
    vec3 cosmos = nebula(cosmosDir, t) * 0.85
                + starAcc * mix(0.95, 1.20, breakAmount);

    // The colours the 1.20.1 twin pushed as rimColor/voidColor/flashColor
    // uniforms, reconstructed from breakAmount with the same lerp chain as
    // SkyRuptureEffect (rimRed/rimGreen/..., voidRed/..., flashRed/...).
    vec3 rimColor = vec3(mix(0.55, 0.85, breakAmount),
                         mix(0.85, 0.25, breakAmount),
                         1.00);
    vec3 voidColor = vec3(mix(0.02, 0.07, breakAmount), 0.0, mix(0.05, 0.11, breakAmount));
    vec3 flashColor = vec3(mix(0.85, 0.92, breakAmount),
                           mix(0.95, 0.85, breakAmount),
                           1.0);

    // -- composite, bottom to top --------------------------------------------
    vec3 col = vec3(0.0);
    float alpha = 0.0;

    // (1) bottom layer: the devoured dark sky, i.e. the shards themselves
    {
        float frontY = mix(1.06, -1.06, clamp(skyDarkProgress, 0.0, 1.0));
        float frontSoft = mix(0.38, 0.05, clamp(skyDarkProgress, 0.0, 1.0));
        over(col, alpha, voidColor * 0.55,
             smoothstep(frontY - frontSoft, frontY + frontSoft, dir.y)
                     * clamp(skyDarkOpacity, 0.0, 1.0));
    }

    // (2) the opened gaps reveal the cosmos
    over(col, alpha, cosmos, gapMask);

    // (3) cold energy light on the shard rims (the barrier still glowing)
    over(col, alpha, rimColor * 1.25, rimBand * 0.55);

    // (4) a very short, very weak lift at the moment of breaking, just enough to
    //     read as a snap
    float flash = (1.0 - smoothstep(0.0, 0.05, progress)) * erupt;
    over(col, alpha, flashColor, flash * 0.07);

    alpha *= clamp(fade, 0.0, 1.0);
    if (alpha < 0.002) {
        discard;
    }

    fragColor = vec4(aces(col * 1.15), clamp(alpha, 0.0, 1.0));
}
