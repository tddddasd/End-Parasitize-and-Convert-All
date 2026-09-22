#version 330

// EPCA gas cloud billboard shader (vertex stage), 26.1.2 core shader.
//
// Pipeline : epca:pipeline/gas_cloud   (see GasCloudRenderType)
// Vertex   : Position, Color, UV0, UV1 (see GasCloudRenderType.GAS_CLOUD_VERTEX_FORMAT)
//
// The billboard orientation is already baked into the vertex positions on the CPU, so this stage
// only performs the standard Minecraft MVP transform. The cloud's lifetime fade travels in the
// vertex colour alpha and the per-cloud noise seed travels in the UV1 slot, because 26.1.2 has no
// reachable per-draw uniform for geometry submitted through SubmitNodeCollector.

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;

out vec4 vertexColor;
out vec2 texCoord0;
// Distance from the camera, used by the fragment stage for the far-distance fade.
out float vertexDistance;
// Integer varyings must be flat. All four vertices of a sub-quad carry the same values, so the
// provoking vertex is representative.
flat out ivec2 gasSeed;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    // ColorModulator is always (1,1,1,1) for this render type; the alpha is the cloud fade.
    vertexColor = Color * ColorModulator;
    texCoord0 = UV0;
    // length() instead of viewPos.z: the raw view-space Z is NEGATIVE in front of the camera, so a
    // fade written against it is dead code (smoothstep(48, 96, negative) is always 0). length() is
    // the true camera distance and does not depend on the sign convention of the projection. This
    // mirrors the fix applied to the 1.20.1 twin.
    vertexDistance = length(viewPos.xyz);
    gasSeed = UV1;
}
