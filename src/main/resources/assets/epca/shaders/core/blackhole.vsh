#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 vViewPos;
out vec2 vUv;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    vViewPos = viewPos.xyz;
    vUv = UV0;
    gl_Position = ProjMat * viewPos;
}
