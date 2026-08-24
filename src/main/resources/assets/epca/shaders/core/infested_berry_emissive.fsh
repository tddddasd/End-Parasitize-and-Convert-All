#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

bool isEmissiveColor(vec3 color) {

    vec3 targetColors[5];
    targetColors[0] = vec3(0.3098, 0.3569, 0.0706);
    targetColors[1] = vec3(0.5137, 0.5882, 0.1569);
    targetColors[2] = vec3(0.7020, 0.7765, 0.3529);
    targetColors[3] = vec3(0.9216, 0.9569, 0.4980);
    targetColors[4] = vec3(0.9843, 1.0,   0.8275);

    float thresholdSq = 0.04;

    for (int i = 0; i < 5; i++) {
        vec3 diff = color - targetColors[i];
        float distSq = dot(diff, diff);
        if (distSq < thresholdSq) {
            return true;
        }
    }
    return false;
}

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) discard;

    vec4 litColor = color * vertexColor * ColorModulator;

    bool emissive = isEmissiveColor(color.rgb);

    vec3 emissiveColor = vec3(0.25, 0.25, 0.25) * (emissive ? 0.3 : 0.0);

    vec3 finalRGB = litColor.rgb + emissiveColor;

    float fogFactor = linear_fog_fade(vertexDistance, FogStart, FogEnd);
    finalRGB *= fogFactor;

    fragColor = vec4(finalRGB, litColor.a);
}