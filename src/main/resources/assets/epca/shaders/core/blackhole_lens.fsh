#version 150

uniform sampler2D Sampler0;
uniform vec3  HoleView;
uniform float HoleRadius;
uniform float TanHalfFov;
uniform float Aspect;
uniform float DeflectK;
uniform vec4  ColorModulator;

in vec2 texCoord;
out vec4 fragColor;

vec3 rotAxis(vec3 v, vec3 u, float a) {
    float c = cos(a);
    float s = sin(a);
    return v * c + cross(u, v) * s + u * dot(u, v) * (1.0 - c);
}

void main() {
    vec3 scene = texture(Sampler0, texCoord).rgb;
    if (HoleView.z > -0.05) {
        fragColor = vec4(scene, 1.0) * ColorModulator;
        return;
    }

    vec2 ndc = texCoord * 2.0 - 1.0;
    vec3 ro = vec3(0.0);
    vec3 rd = normalize(vec3(ndc.x * TanHalfFov * Aspect, ndc.y * TanHalfFov, -1.0));
    vec3 bh = HoleView;

    float t = dot(rd, bh - ro);
    vec3 v = ro + rd * t - bh;
    float r = length(v);

    // The event horizon itself is drawn by the billboard shader; the lens only bends light.
    float horizon = max(HoleRadius, 1.0e-3);

    vec3 nml = normalize(cross(v, rd));
    float ang = DeflectK * HoleRadius / max(r, horizon);
    ang = clamp(ang, -1.4, 1.4);
    vec3 bent = rotAxis(rd, nml, ang);

    float z = max(-bent.z, 1.0e-4);
    vec2 bentNdc = vec2(bent.x / z / (TanHalfFov * Aspect), bent.y / z / TanHalfFov);
    vec2 bentUv = clamp(bentNdc * 0.5 + 0.5, 0.002, 0.998);

    vec3 col = texture(Sampler0, bentUv).rgb;

    // Lens size follows the hole's apparent size on screen.
    float holeDist = length(HoleView);
    float screenR = (HoleRadius / max(holeDist, 1.0e-3)) / max(TanHalfFov, 1.0e-4);
    float inner = max(screenR * 0.6, 0.015);
    float outer = max(screenR * 2.0, 0.05);
    // mask is centred on the hole itself, not on the middle of the screen
    vec2 holeNdc = vec2(HoleView.x / -HoleView.z / (TanHalfFov * max(Aspect, 1.0e-4)),
                        HoleView.y / -HoleView.z / TanHalfFov);
    vec2 holeOffset = ndc - holeNdc;
    float holeDelta = length(vec2(holeOffset.x * max(Aspect, 1.0e-4), holeOffset.y));
    float edge = 1.0 - smoothstep(inner, outer, holeDelta);
    fragColor = vec4(mix(scene, col, edge), 1.0) * ColorModulator;
}