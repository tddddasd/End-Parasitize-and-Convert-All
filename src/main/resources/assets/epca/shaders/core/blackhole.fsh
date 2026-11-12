#version 150

in vec3 vViewPos;
in vec2 vUv;

uniform vec3  uHole;    // hole centre, view space
uniform vec3  uUp;      // world up, view space (accretion disk normal)
uniform float uRadius;  // Schwarzschild radius, world units
uniform float uTime;
uniform float uSpin;    // per entity disk phase
uniform float uDisk;    // disk brightness
uniform float uStars;   // background star brightness
uniform vec4  ColorModulator;

out vec4 fragColor;

const float INNER = 2.35;
const float OUTER = 7.10;

// --------------------------------------------------------------- hashing --
float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hash31(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

// ---------------------------------------------------------------- noise ----
float vnoise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i);
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 3; i++) {
        sum += amp * vnoise(p);
        p = p * 2.03 + vec3(1.7, 9.2, 3.4);
        amp *= 0.5;
    }
    return sum;
}

// ------------------------------------------------------------- star field --
vec3 starLayer(vec3 dir, float scale, float thresh) {
    vec3 p = dir * scale;
    vec3 id = floor(p);
    vec3 f = fract(p);
    float h = hash31(id);
    if (h < thresh) return vec3(0.0);
    vec3 off = hash33(id + vec3(11.3, 5.7, 2.9)) * 0.55 + 0.225;
    float d = length(f - off);
    float core = smoothstep(0.16, 0.0, d);
    float halo = smoothstep(0.48, 0.0, d) * 0.12;
    float mag = (h - thresh) / max(1.0 - thresh, 1.0e-4);
    vec3 tint = mix(vec3(0.72, 0.58, 1.0), vec3(0.98, 0.70, 1.0), hash31(id + vec3(3.1, 8.4, 1.2)));
    return tint * (core + halo) * mag;
}

vec3 sky(vec3 dir) {
    vec3 c = starLayer(dir, 110.0, 0.965) * 1.6;
    c += starLayer(dir + vec3(4.7, 1.3, 8.1), 210.0, 0.982) * 0.9;
    float n = fbm(dir * 2.4 + vec3(2.4, 0.5, 7.1));
    c += vec3(0.080, 0.030, 0.140) * smoothstep(0.45, 1.05, n);
    return c;
}

// ------------------------------------------------------------------ disk ---
float diskShape(float r) {
    if (r < INNER || r > OUTER) return 0.0;
    float t = (r - INNER) / (OUTER - INNER);
    float band = smoothstep(0.0, 0.10, t) * (1.0 - smoothstep(0.60, 1.0, t));
    return band * mix(1.0, 0.46, t);
}

vec3 diskTemperature(float t, float beaming) {
    // inner: white hot -> amber -> deep red
    vec3 c = mix(vec3(0.96, 0.92, 1.00), vec3(0.74, 0.44, 1.00), smoothstep(0.00, 0.45, t));
    c = mix(c, vec3(0.46, 0.16, 0.88), smoothstep(0.40, 1.00, t));
    // blue shifted (approaching) material
    c = mix(c, vec3(0.82, 0.70, 1.00), clamp((beaming - 1.0) * 0.35, 0.0, 0.55));
    return c;
}

// ==================================================================== main ==
void main() {
    vec3 rd = normalize(vViewPos);

    // camera position relative to the hole, in Schwarzschild radii
    vec3 p = -uHole / max(uRadius, 1.0e-4);
    vec3 v = rd;

    vec3 N = normalize(uUp);
    vec3 helper = abs(N.z) > 0.85 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 0.0, 1.0);
    vec3 T = normalize(cross(N, helper));
    vec3 B = cross(N, T);

    float h2 = dot(cross(p, v), cross(p, v));

    vec3  col = vec3(0.0);
    float alpha = 0.0;
    bool  captured = false;
    float minR = 1.0e9;

    for (int i = 0; i < 160; i++) {
        float r = length(p);
        minR = min(minR, r);

        if (r < 1.0) { captured = true; break; }
        if (r > 70.0 && dot(p, v) > 0.0) break;

        float dt  = clamp(0.11 * r, 0.008, 2.0);
        float r2  = r * r;
        vec3  acc = -1.5 * h2 * p / (r2 * r2 * r);
        vec3  pn  = p + v * dt + 0.5 * acc * dt * dt;
        vec3  vn  = normalize(v + acc * dt);

        // thin accretion disk: analytic crossing of the equatorial plane
        float s0 = dot(p, N);
        float s1 = dot(pn, N);
        if (s0 * s1 < 0.0) {
            float tq = s0 / (s0 - s1);
            vec3  q  = mix(p, pn, tq);
            float rr = length(q);
            float dens = diskShape(rr);

            if (dens > 0.0) {
                // differential rotation shears the turbulence into spiral filaments
                float rot = uSpin * 3.4 * pow(max(rr, 1.0), -1.5);
                float cs  = cos(rot);
                float sn  = sin(rot);
                vec3  loc = vec3(dot(q, T), dot(q, B), 0.0);
                vec3  rl  = vec3(loc.x * cs - loc.y * sn, loc.x * sn + loc.y * cs, 0.0);

                float n1 = fbm(rl * 0.78 + vec3(0.0, 0.0, rr * 0.26 + uTime * 0.012));
                float n2 = vnoise(rl * 3.10 + vec3(3.1, 7.7, rr * 0.85 - uTime * 0.035));
                dens *= clamp(0.34 + 1.25 * n1 * (0.55 + 0.45 * n2), 0.0, 2.3);

                // orbital motion -> relativistic beaming and doppler colour
                vec3  orbital = normalize(cross(N, q));
                float beta = 0.50 * inversesqrt(max(rr, 1.0));
                float mu   = dot(orbital, -v);
                float beam = clamp(pow(max(1.0 - beta * mu, 0.12), -1.7), 0.28, 2.4);

                float t01 = clamp((rr - INNER) / (OUTER - INNER), 0.0, 1.0);
                vec3  emit = diskTemperature(t01, beam) * beam;
                float gain = dens * uDisk * 0.80;

                col   += emit * gain * (1.0 - alpha);
                alpha += clamp(dens * 0.80, 0.0, 0.90) * (1.0 - alpha);
            }
        }

        p = pn;
        v = vn;
    }

    if (captured) {
        // event horizon: opaque black, whatever already piled up stays in front
        col   *= (1.0 - 0.0);
        alpha = 1.0;
    } else {
        // photon ring shimmer just outside the shadow
        float ring = exp(-abs(minR - 1.28) * 6.5) * 0.22;
        col   += vec3(0.80, 0.68, 1.00) * ring * uDisk * (1.0 - alpha);
        alpha += ring * (1.0 - alpha);

        vec3  bg  = sky(normalize(v)) * uStars;
        float bgA = clamp(length(bg) * 1.15, 0.0, 0.85);
        col   += bg * (1.0 - alpha);
        alpha += bgA * (1.0 - alpha);
    }

    // Reinhard style tone map so the disk keeps its colour instead of clipping
    col = col / (1.0 + col) * 1.75;

    // soft square border so the billboard never shows a hard edge
    float edge = 1.0 - smoothstep(0.62, 0.94, length(vUv * 2.0 - 1.0));

    col   *= ColorModulator.rgb;
    alpha *= ColorModulator.a;

    if (alpha <= 0.0015 && max(col.r, max(col.g, col.b)) <= 0.0015) discard;

    fragColor = vec4(col * edge, alpha * edge);
}
