#version 330

// EPCA sky rupture (vertex stage), 26.1.2 core shader.
//
// Pipeline : epca:pipeline/sky_rupture    (see SkyRuptureRenderType / SkyRuptureShaders)
// Vertex   : the five attributes below     (see SkyRuptureShaders.SKY_RUPTURE_VERTEX_FORMAT)
//
// 1.20.1 -> 26.1.2 port note
// ---------------------------------------------------------------------------
// The 1.20.1 twin bound a ShaderInstance and pushed its whole payload through per-draw uniforms
// (rayForward/rayRight/rayUp, time, progress, breakAmount, fade, seed, patternOffset,
// rim/void/flash colours and mat2 cosmicuvs[12]). 26.1.2 has neither ShaderInstance nor
// the core shader json, and RenderType#draw always writes ColorModulator = (1,1,1,1) into the shared
// DynamicTransforms buffer, so a mod cannot reach a per-draw uniform for geometry submitted through
// SubmitNodeCollector#submitCustomGeometry.
//
// 26.1.2's VertexConsumer exposes only a handful of float writers (verified with javap -c on the
// patched jar: addVertex, setColor, setUv, setUv1, setUv2, setNormal, setLineWidth; the last two
// exist but are not needed here), which is a hard budget of 11 usable float slots per vertex in this
// format. Anything heavier therefore has to be derived rather than carried:
//
//   * the camera basis (9 floats in 1.20.1) is reconstructed from the pipeline's own Projection and
//     DynamicTransforms UBOs, which MATRICES_PROJECTION_SNIPPET always binds. Three rays are taken
//     from the inverse projection at three NDC corners and rotated into world space by ModelViewMat's
//     rotation; that is both exact (it picks up the real FOV) and free;
//   * the 12 cosmic atlas rectangles (24 floats) are not sent at all: each draw binds one sprite as
//     its own direct texture, so the sprite's UV space *is* [0,1]^2 and the only residual per-sprite
//     constant is the animation frame count, which is baked in the fragment stage as COSMIC_FRAMES;
//   * the rim/void/flash colours are rebuilt in the fragment stage from breakAmount with the same
//     lerp chain the Java side used;
//   * the 1.20.1 `seed` uniform is dropped: the fragment stage never consumed it (it only reached the
//     diagnostic log line).
//
// What is left is genuinely per-frame: time, progress, breakAmount, fade, the crack field offset and
// the two sky-darkening values.

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec3 Position;      // xy = 0..1 screen quad, z = star shell index
layout(location = 1) in vec4 Color;         // breakAmount, fade, unused, unused
layout(location = 2) in vec2 TimeProgress;  // time (seconds), rupture progress 0..1
layout(location = 3) in ivec2 SkyDark;      // skyDarkProgress, skyDarkOpacity (16-bit fixed point)
layout(location = 4) in ivec2 Pattern;      // crack field offset X, Y (16-bit fixed point, x1023)

out vec2 ndcPos;

out vec3 rayForward;
out vec3 rayRight;
out vec3 rayUp;
out float time;
out float progress;
out float breakAmount;
out float fade;
out vec2 patternOffset;
out float skyDarkProgress;
out float skyDarkOpacity;

/** Index of the star shell this draw owns; the shell -> sprite mapping is the identity. */
flat out int cosmicBase;

/**
 * The world-space direction of the pixel at the given NDC position.
 *
 * inverse(ProjMat) maps NDC back to view space; because the quad sits on the far plane (z = 1) the
 * result is a point on that plane and its direction from the eye is the view ray. Rotating by
 * ModelViewMat with w = 0 applies only the rotation, turning it into a world direction. This replaces
 * the three 1.20.1 ray uniforms and is if anything more accurate, because it uses the real projection
 * matrix instead of a tan(fov/2) reconstruction.
 */
vec3 worldRay(vec2 ndc) {
    vec4 farPoint = inverse(ProjMat) * vec4(ndc, 1.0, 1.0);
    return normalize((ModelViewMat * vec4(farPoint.xyz / farPoint.w, 0.0)).xyz);
}

void main() {
    ndcPos = Position.xy * 2.0 - 1.0;
    // z = 1.0 is the far plane: with a LESS_THAN_OR_EQUAL depth test this quad only survives where
    // the depth buffer is still at the clear value, i.e. on sky pixels. Terrain, entities and clouds
    // wrote closer depth and mask it off.
    gl_Position = vec4(ndcPos, 1.0, 1.0);

    // Three corner rays give the centre direction plus the two screen-space tangents the fragment
    // stage needs. Interpolating them across the quad reproduces the per-pixel ray with the same
    // linear approximation the 1.20.1 twin used (which passed tan-scaled right/up vectors).
    vec3 ray00 = worldRay(vec2(-1.0, -1.0));
    vec3 ray10 = worldRay(vec2(1.0, -1.0));
    vec3 ray01 = worldRay(vec2(-1.0, 1.0));
    rayRight = (ray10 - ray00) * 0.5;
    rayUp = (ray01 - ray00) * 0.5;
    rayForward = normalize(ray00 + rayRight + rayUp);

    breakAmount = Color.r;
    fade = Color.g;
    time = TimeProgress.x;
    progress = TimeProgress.y;
    skyDarkProgress = float(SkyDark.x) / 65535.0;
    skyDarkOpacity = float(SkyDark.y) / 65535.0;
    // The crack field offset covers 0..64, so it is quantised at 1/1023 and scaled back here; one
    // step is 0.063 world units of a low frequency offset, which is far below anything visible.
    patternOffset = vec2(float(Pattern.x) / 1023.0, float(Pattern.y) / 1023.0);

    cosmicBase = int(Position.z + 0.5);
}
