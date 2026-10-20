#version 150

uniform sampler2D Sampler0;

uniform float LensStrength;   // radial pull
uniform float LensWinding;    // full turns of swirl
uniform float LensScale;      // ring size relative to the hole

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

const float PI = 3.14159265;

void main() {
    vec2 centred = texCoord * 2.0 - 1.0;

    // Circular ring: the hole is round, so the lens contour is a circle.
    float dist = length(centred) / LensScale;
    if (dist > 1.0) {
        discard;
    }

    float angle = atan(centred.y, centred.x);
    float pull = smoothstep(1.0, 0.0, dist);

    // Light winds around the hole several times, strongest near the horizon.
    float swirl = pull * pull * LensWinding * 2.0 * PI;
    float bentAngle = angle + swirl;
    float squeeze = 1.0 - pull * LensStrength * 0.55;

    vec2 sampleUv = vec2(cos(bentAngle), sin(bentAngle)) * dist * squeeze * 0.5 + 0.5;
    vec4 behind = texture(Sampler0, sampleUv);

    // Event horizon: a round black core with a soft edge.
    float core = smoothstep(0.52, 0.18, dist);
    vec3 rgb = mix(behind.rgb, vec3(0.0), core);

    // A faint bright rim just outside the horizon.
    float rim = smoothstep(0.62, 0.50, dist) * smoothstep(0.34, 0.50, dist);
    rgb += vec3(0.25, 0.22, 0.30) * rim * pull;

    fragColor = vec4(rgb, 1.0) * vertexColor;
}
