#version 330

// EPCA Alayavijnana slash (vertex stage), 26.1.2 core shader.
//
// Pipeline : epca:pipeline/araya_slash      (see ArayaSlashRenderType)
// Vertex   : Position, Color, UV0           (DefaultVertexFormat.POSITION_COLOR_TEX)
// Sampler0 : the per-frame scene copy       (see ArayaSceneCopy)
//
// 1.20.1 -> 26.1.2 port note
// ---------------------------------------------------------------------------
// The 1.20.1 twin is assets/epca/shaders/core/araya_slash.vsh with the same three attributes and the
// same two values packed into UV0 (the signed distance from the blade's centreline in blocks, and the
// half width of the strip it belongs to). The differences are the GLSL version and the fact that the
// refraction ratio is a compile-time constant here instead of a core-shader JSON uniform: 26.1.2 has no
// core shader JSON, and RenderType#draw always writes ColorModulator = (1,1,1,1) into the shared
// DynamicTransforms buffer, so a per-draw uniform is not reachable from this path. The constant is the
// twin of ArayaConstants.SLASH_REFRACTION_RATIO and is cross-checked against it by
// build/javac-check/check-glsl-26.py.

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 ribbonCoord;
out vec2 screenUv;
out vec2 sceneOffset;

const float REFRACTION_RATIO = 0.0625;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // The frame copy has exactly this render target's dimensions, so the normalised device position is
    // already the texture coordinate of the pixel this vertex lands on. The perspective divide has to
    // happen here: interpolating the pre-divide clip position would interpolate w along with xy and give
    // the wrong screen position for every fragment.
    screenUv = gl_Position.xy / gl_Position.w * 0.5 + 0.5;

    vertexColor = Color;
    ribbonCoord = UV0;

    // A fixed diagonal shift in screen space, growing with the distance from the blade's centreline: the
    // glowing line shifts by nothing at all, and the border's outer edge shifts by REFRACTION_RATIO times
    // its distance. The diagonal (rather than a pure horizontal or vertical shift) is what makes the
    // border read as a bend instead of a smeared copy of one row or column of the frame.
    sceneOffset = UV0.x * REFRACTION_RATIO * vec2(0.8, -0.5);
}
