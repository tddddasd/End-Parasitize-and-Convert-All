#version 330

// EPCA item corruption (vertex stage), 26.1.2 core shader.
//
// Pipeline : epca:pipeline/item_corruption   (see ItemShaderPipelines)
// Vertex   : Position, Color, Uv, TimeData, Params, Normal
//            (see ItemShaderPipelines.ITEM_LAYER_VERTEX_FORMAT)
//
// 1.20.1 -> 26.1.2 port note
// ---------------------------------------------------------------------------
// The 1.20.1 twin was `corruption.vsh` at #version 150 and declared its uniforms in the sibling
// corruption.json, of which 26.1.2 has no equivalent: the render state now lives in a
// RenderPipeline. Its payload was four per-draw uniforms (`time`, `intensity`, `tint`,
// `splitStrength`), and there is no reachable per-draw uniform on the submit path any more (see
// SkyRuptureShaders for the full argument), so the payload travels in the vertex attributes:
//
//   Color.rgb      tint red, green, blue      (the 1.20.1 `tint` uniform)
//   Color.a        unused, 1.0
//   UV0            mask sprite UV             (what the 1.20.1 `texCoord0` varying carried)
//   UV1 (ivec2)    the tick clock             (the 1.20.1 `time` uniform, as a full 32-bit int)
//   UV2 (ivec2)    intensity, splitStrength   (16-bit fixed point over 0..1 and 0..4)
//   Normal         the quad normal, unused by the fragment stage
//
// The geometry is emitted by VertexConsumer#putBakedQuad, which is what fixes this exact element set:
// it writes POSITION, COLOR, UV0, UV1, UV2 and NORMAL and nothing else, and a declared element that is
// never written makes the vertex short (which 26.1.2 rejects). The two ivec2 slots exist because
// setUv1/setUv2 store raw shorts rather than normalized floats.
//
// `vertexColor` (the lightmap-modulated item colour) is not available any more: the COLOR element is
// repurposed for the tint. The 1.20.1 fragment stage only mixed 20% of it back in, and that term is
// dropped; see corruption.fsh. Fog is dropped too, because the Fog UBO is not part of the
// MATRICES_PROJECTION_SNIPPET this pipeline uses. Both are documented approximations.

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;      // tint rgb
layout(location = 2) in vec2 Uv;         // mask sprite UV
layout(location = 3) in ivec2 TimeData;  // low 16 bits, high 16 bits of the tick clock
layout(location = 4) in ivec2 Params;    // x = intensity, y = splitStrength (both 16-bit fixed point)
layout(location = 5) in vec3 Normal;     // unused

out vec4 vertexTint;
out vec2 texCoord0;
out float time;
out float intensity;
out float splitStrength;

/** Recovers the 32-bit tick clock from the two 16-bit halves. */
float unpackTime(ivec2 halves) {
    int low = halves.x & 0xFFFF;
    int high = halves.y & 0xFFFF;
    return float((high << 16) | low);
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexTint = Color;
    texCoord0 = Uv;
    time = unpackTime(TimeData);
    intensity = float(Params.x & 0xFFFF) / 65535.0;
    // The 1.20.1 splitStrength range was 0..4 and the shader clamped it there.
    splitStrength = float(Params.y & 0xFFFF) / 65535.0 * 4.0;
}
