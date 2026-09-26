#version 150

// Vertex stage of the Alayavijnana slash.
//
// The slash is built as coplanar strips in the blade's own plane (see ArayaSlashRenderer): one strip for
// the glowing line, one wider dimmer strip for its halo, and two narrow strips beside it for the
// refracting border. Each strip carries its own two numbers in the single UV0 the vanilla
// POSITION_COLOR_TEX format provides:
//
//   UV0.x  the signed distance from the blade's centreline, in BLOCKS.
//   UV0.y  the half width of this strip, in BLOCKS.
//
// Those two are everything the effect needs: the profile is UV0.x normalised by UV0.y, and the frame
// copy offset is UV0.x scaled by the refraction ratio (zero in the middle of the blade, largest at the
// border's outer edge). There is no per-slash uniform and no mode flag, and no second UV attribute is
// needed, which is what keeps this usable on 1.20.1 where the vertex format elements are private.

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float RefractionRatio;

out vec4 vertexColor;
out vec2 ribbonCoord;
out vec2 screenUv;
out vec2 sceneOffset;

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
    // glowing line shifts by nothing at all, and the border's outer edge shifts by RefractionRatio times
    // its distance. The diagonal (rather than a pure horizontal or vertical shift) is what makes the
    // border read as a bend instead of a smeared copy of one row or column of the frame.
    sceneOffset = UV0.x * RefractionRatio * vec2(0.8, -0.5);
}
