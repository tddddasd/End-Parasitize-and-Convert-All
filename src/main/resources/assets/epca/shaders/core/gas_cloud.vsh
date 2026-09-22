#version 150

// EPCA gas cloud billboard shader (vertex stage).
//
// Vertex format: DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP (Position, Color, UV0, UV2).
//
// PER-CLOUD VALUES RIDE IN VERTEX ATTRIBUTES, NEVER IN UNIFORMS.
// ShaderInstance.apply() uploads uniforms once per RenderType flush, while the vertices for
// every cloud of a pass are buffered beforehand, so a per-cloud uniform would give each cloud
// the LAST cloud's value. Instead:
//   * per-cloud fade  -> Color.a
//   * per-cloud seed  -> UV2.x (fixed point), sub-quad index -> UV2.y
//
// UV2 is the lightmap slot. VertexFormatElement.ELEMENT_UV2 is Type.SHORT x2, i.e. signed
// 16-bit, so packed values must stay within 0..32767. The clouds are unlit (NO_LIGHTMAP render
// state), so this slot is free to reuse.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;
// x = packed per-cloud seed, y = sub-quad index. Constant across a quad, so interpolating the
// pair as floats is exact.
out vec2 cloudSeed;
out float vertexDistance;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    vertexColor = Color;
    texCoord0 = UV0;
    cloudSeed = UV2;
    // View-space Z is negative in front of the camera, so use the vector length.
    vertexDistance = length(viewPos.xyz);
}
