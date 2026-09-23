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
//     DynamicTransforms UBOs, which MATRICES_PROJECTION_SNIPPET always binds. The world-space axes are
//     the columns of the inverse of ModelViewMat's rotation, so only the 3x3 block is inverted: for an
//     affine matrix the upper-left 3x3 of the inverse IS the inverse of the upper-left 3x3, so this is
//     the same rotation without the 4x4 cofactor work (and the axes are normalised either way). This
//     stage runs four vertices per frame, so it is negligible, but there is no per-draw uniform left to
//     hoist it into;
//   * the 12 cosmic atlas rectangles (24 floats) are not sent at all: all twelve strips are packed into
//     one sheet texture plus a phase -> band lookup table, both baked in the fragment stage (see
//     COSMIC_SHEET_BASE / COSMIC_CYCLE there). Nothing extra has to cross this stage for them: the
//     fragment stage already receives the tick clock as the `time` varying, and it loops all twelve
//     shells itself, so the vertex stage and the 32-byte format are untouched by the star animation;
//   * the rim/void/flash colours are rebuilt in the fragment stage from breakAmount with the same
//     lerp chain the Java side used;
//   * the 1.20.1 `seed` uniform is dropped: the fragment stage never consumed it (it only reached the
//     diagnostic log line).
//
// What is left is genuinely per-frame: time, progress, breakAmount, fade, the crack field offset and
// the two sky-darkening values.
//
// 16-bit payload slots are UNSIGNED
// ---------------------------------------------------------------------------
// UV1/UV2 are `2xShort` elements and `BufferBuilder.setUv1/setUv2` narrow their arguments with a
// `(short)` cast, while VertexArrayCache binds them with glVertexAttribIFormat(GL_SHORT), so the
// fragment stage reads a negative int for any intended value >= 0x8000. Masking with 0xFFFF on the
// decode restores the full unsigned 0..65535 range; without the mask every packed value >= 0.5 (i.e.
// exactly half of the sky-darkening envelope, and two thirds of the 0..64 crack offset) came back
// negative and was clamped to zero - which is why the dark sky vanished halfway through its own
// darkening phase and was absent for the whole rupture.

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec3 Position;      // xy = 0..1 screen quad, z = unused (format padding)
layout(location = 1) in vec4 Color;         // breakAmount, fade, unused, unused
layout(location = 2) in vec2 TimeProgress;  // time (world ticks), rupture progress 0..1
layout(location = 3) in ivec2 SkyDark;      // skyDarkProgress, skyDarkOpacity (unsigned 16-bit)
layout(location = 4) in ivec2 Pattern;      // crack field offset X, Y (unsigned 16-bit, x1023)

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



void main() {
    ndcPos = Position.xy * 2.0 - 1.0;
    // z = 1.0 is the far plane: with a LESS_THAN_OR_EQUAL depth test this quad only survives where
    // the depth buffer is still at the clear value, i.e. on sky pixels. Terrain, entities and clouds
    // wrote closer depth and mask it off.
    gl_Position = vec4(ndcPos, 1.0, 1.0);

    // ---------------------------------------------------------------------------------------------
    // Camera basis, replicated from 1.20.1.
    //
    // 1.20.1's SkyRuptureRenderer pushed WORLD-space axes as uniforms:
    //     forward = camera.getLookVector(); up = camera.getUpVector(); left = camera.getLeftVector();
    //     rayForward = forward;  rayRight = -left * tanX;  rayUp = up * tanY;
    // with tanY = 1 / m11, tanX = 1 / m00 taken from the projection matrix and clamped to [0.05, 10].
    // The fragment stage then does normalize(rayForward + rayRight * ndc.x + rayUp * ndc.y), and because
    // the pattern is derived from that WORLD direction the crack network stays anchored to the sky.
    //
    // The three camera axes in world space are the columns of inverse(ModelViewMat), which maps view ->
    // world: column 0 is the camera right, column 1 the camera up, and column 2 the camera backward,
    // since view space looks down -Z. Only that 3x3 block is needed, and inverting it directly is both
    // cheaper than the 4x4 and exactly the same rotation: for an affine matrix [[R, t], [0, 1]] the
    // upper-left block of the inverse is R^-1, independent of the translation. (mat3(mat4) is the
    // upper-left 3x3.) This runs once per vertex - four per frame - so the saving is small, but there
    // is no per-draw uniform in 26.1.2 to hoist it into.
    //
    // DEFECT FIXED HERE: the previous revision built rays from inverse(ProjMat) (a VIEW-space ray) and
    // then multiplied by ModelViewMat, which maps world -> view. That applied the camera rotation a
    // second time, so the "world" direction the fragment stage received actually rotated with the
    // camera - the reported "crack pattern turns with the view" bug. It also used (r10 - r00) * 0.5 as
    // the tangent, which is algebraically the same quantity as tanX, so the only real error was the
    // space. The basis below is literally 1.20.1's.
    //
    // 1.20.1 applied no celestial/sky rotation here, so none is applied: the anchoring comes purely from
    // using the world-space camera orientation.
    // ---------------------------------------------------------------------------------------------
    mat3 viewToWorld = inverse(mat3(ModelViewMat));
    vec3 cameraRight = normalize(viewToWorld[0]);
    vec3 cameraUp = normalize(viewToWorld[1]);
    vec3 cameraForward = normalize(-viewToWorld[2]);

    // Same derivation and same clamps as 1.20.1's renderer.
    float tanY = clamp(1.0 / ProjMat[1][1], 0.05, 10.0);
    float tanX = clamp(1.0 / ProjMat[0][0], 0.05, 10.0);

    rayForward = cameraForward;
    rayRight = cameraRight * tanX;
    rayUp = cameraUp * tanY;

    breakAmount = Color.r;
    fade = Color.g;
    time = TimeProgress.x;
    progress = TimeProgress.y;
    // `& 0xFFFF` undoes the `(short)` narrowing on the Java side (see the header): the attribute is
    // bound as GL_SHORT, so a packed value >= 0x8000 would otherwise arrive sign-extended, i.e. as a
    // negative number that the consumer clamps to zero. The intended ranges are 0..1 here.
    skyDarkProgress = float(SkyDark.x & 0xFFFF) / 65535.0;
    skyDarkOpacity = float(SkyDark.y & 0xFFFF) / 65535.0;
    // The crack field offset covers 0..64, so it is quantised at 1/1023 and scaled back here; one
    // step is 0.063 world units of a low frequency offset, which is far below anything visible.
    patternOffset = vec2(float(Pattern.x & 0xFFFF) / 1023.0, float(Pattern.y & 0xFFFF) / 1023.0);
}
