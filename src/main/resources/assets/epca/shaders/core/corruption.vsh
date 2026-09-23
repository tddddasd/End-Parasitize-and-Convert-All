#version 330

// EPCA item corruption (vertex stage), 26.1.2 core shader.
//
// Pipeline : epca:pipeline/item_corruption   (see ItemShaderPipelines)
// Vertex   : Position, Color, Uv, Params, AnimClock
//            (see ItemShaderPipelines.ITEM_LAYER_VERTEX_FORMAT, 32 bytes)
//
// 1.20.1 -> 26.1.2 port note
// ---------------------------------------------------------------------------
// The 1.20.1 twin was `corruption.vsh` at #version 150 and declared its uniforms in the sibling
// corruption.json, of which 26.1.2 has no equivalent: the render state lives in a RenderPipeline now. Its
// payload was four per-draw uniforms (`time`, `intensity`, `tint`, `splitStrength`), and there is no
// reachable per-draw uniform on the submit path any more (see SkyRuptureShaders for the full argument),
// so the payload travels in the vertex attributes:
//
//   Color.rgb      tint red, green, blue   (the 1.20.1 `tint` uniform)
//   Color.a        unused, pinned to 1.0
//   UV0            mask sprite UV          (what the 1.20.1 `texCoord0` varying carried)
//   UV1 (ivec2).x  intensity, 16-bit fixed point over 0..1
//   UV1 (ivec2).y  splitStrength, 16-bit fixed point over 0..4
//   LINE_WIDTH     the animation clock in game ticks, as a full-precision float
//                  (the 1.20.1 `time` uniform, which was (float) (gameTime % Integer.MAX_VALUE))
//
// Why this exact set: VertexFormat.Builder#build() requires the vertex size to be a multiple of 4, and the
// element sizes are POSITION 12, COLOR 4, UV0 8, UV1 4, UV2 4, NORMAL 3, LINE_WIDTH 4. The
// POSITION/COLOR/UV0/UV1/UV2/NORMAL set that VertexConsumer#putBakedQuad writes sums to 35 bytes and
// throws, which is why the geometry is emitted by hand and NORMAL/UV2 are not declared. The sum here is
// 32.
//
// Because UV2 is gone, intensity and split strength share UV1 (two raw 16-bit shorts, recovered below) and
// the clock uses LINE_WIDTH. That gives the clock *more* precision than the earlier 16-bit packing and
// keeps intensity and split strength at 16 bits.
//
// `vertexColor` (the lightmap-modulated item colour) is not available any more: the COLOR element carries
// the tint. The 1.20.1 fragment stage mixed only 20% of it back in, and that term is dropped. Fog is
// dropped too, because the Fog UBO is not part of the MATRICES_PROJECTION_SNIPPET this pipeline uses. Both
// are documented approximations in corruption.fsh.

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;      // tint rgb
layout(location = 2) in vec2 Uv;         // mask sprite UV
layout(location = 3) in ivec2 Params;    // x = intensity, y = splitStrength (16-bit fixed point)
layout(location = 4) in float AnimClock; // game ticks

out vec4 vertexTint;
out vec2 texCoord0;
out float time;
out float intensity;
out float splitStrength;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexTint = Color;
    texCoord0 = Uv;

    time = AnimClock;
    intensity = float(Params.x & 0xFFFF) / 65535.0;
    // The 1.20.1 splitStrength range was 0..4 and the fragment stage clamps it there.
    splitStrength = float(Params.y & 0xFFFF) / 65535.0 * 4.0;
}
