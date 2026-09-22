#version 150

#moj_import <fog.glsl>

// ─────────────────────────────────────────────────────────────────────
//  EPCA 崩坏（Corruption）物品着色器
//
//  移植自 RottenRuinsSplendiding 的 assets/hall/shaders/core/corruption.fsh，
//  在其基础上把两处硬编码常量参数化为 uniform，便于通过
//  ItemRenderRegistry 给不同物品配置不同的崩坏外观：
//
//    intensity     0.0–1.0  崩坏强度（0 时等价于原版贴图）
//    tint          vec3     崩坏染色（默认 0.55,0.08,0.50 品红紫）
//    splitStrength float    RGB 色散 / 错位强度倍率
//
//  该着色器只负责“颜色层面”的崩坏；几何层面的鬼畜抖动见
//  CorruptionPulse（PoseStack 变换）。
// ─────────────────────────────────────────────────────────────────────

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform float time;
uniform float intensity;
uniform vec3 tint;
uniform float splitStrength;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// ── noise / hash ──────────────────────────────────────────────────────

float hash2(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hash1(float n) { return fract(sin(n) * 43758.5453123); }

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), f.x),
               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), f.x), f.y);
}

// ── main ──────────────────────────────────────────────────────────────

void main() {
    vec2 uv = texCoord0;
    float i = clamp(intensity, 0.0, 1.0);
    float split = clamp(splitStrength, 0.0, 4.0);
    vec4 base = texture(Sampler0, uv);

    if (i < 0.005) {
        fragColor = base * vertexColor * ColorModulator;
        return;
    }

    // ── 1. RGB 色散分离（随时间缓慢旋转方向） ──
    float splitAmt = i * 0.012 * split;
    float ang = time * 0.7;
    vec2 dir = vec2(cos(ang), sin(ang));
    float r = texture(Sampler0, uv + dir * splitAmt).r;
    float g = texture(Sampler0, uv).g;
    float b = texture(Sampler0, uv - dir * splitAmt).b;
    vec3 rgb = vec3(r, g, b);

    // ── 2. 染色偏移 → 崩坏色 ──
    rgb = mix(rgb, tint, i * 0.35);

    // 去饱和到冷色调灰
    float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
    vec3 cold = vec3(gray * 0.7, gray * 0.25, gray * 0.85);
    rgb = mix(rgb, cold, i * 0.45);

    // ── 3. 扫描线 ──
    float sl = sin(uv.y * 350.0 + time * 8.0) * 0.5 + 0.5;
    float slStrength = i * 0.18;
    float sl2 = sin(uv.y * 87.0 - time * 3.0) * 0.5 + 0.5;
    rgb *= 1.0 - sl * slStrength - sl2 * slStrength * 0.4;

    // ── 4. 横向故障条带 ──
    float row = floor(uv.y * 55.0);
    float gh = hash1(row * 137.0 + floor(time * 4.0));
    float glitch = step(0.94, gh) * i;
    if (glitch > 0.5) {
        float goff = (hash1(row * 311.0 + time * 2.7) - 0.5) * 0.08 * i * split;
        rgb.r = texture(Sampler0, uv + vec2(goff, 0.0)).r;
        rgb.b = texture(Sampler0, uv - vec2(goff * 0.6, 0.0)).b;
        rgb *= 0.7 + 0.3 * hash1(row + time);
    }

    // ── 5. 颗粒噪声 ──
    float grain = noise(uv * 400.0 + time * 20.0) * i * 0.1;
    rgb -= grain;
    float grain2 = hash2(uv * 700.0 + time * 13.0) * i * 0.04;
    rgb -= grain2;

    // ── 6. 随机坏点 ──
    float dp = hash2(uv * 200.0 + vec2(time * 5.0, time * 3.0 + 71.0));
    float dpMask = step(0.975, dp) * i * 0.55;
    rgb = mix(rgb, vec3(0.0, 0.0, 0.0), dpMask);

    // 高亮闪烁像素
    float bp = hash2(uv * 300.0 + vec2(time * 11.0, time * 7.0 + 13.0));
    float bpMask = step(0.985, bp) * i * 0.3;
    rgb = mix(rgb, vec3(0.9, 0.2, 0.8), bpMask);

    // ── 7. 暗角 ──
    vec2 vig = abs(uv - 0.5) * 2.0;
    float v = 1.0 - dot(vig, vig) * 0.55 * i;
    rgb *= v;

    // ── 8. 低频脉动暗波 ──
    float wave = sin(uv.y * 6.0 + time * 2.0) * sin(uv.x * 5.0 + time * 1.7) * i * 0.08;
    rgb += wave;

    vec4 color = vec4(rgb, 1.0);
    color.a *= base.a;

    // 少量保留物品自身光照，避免完全脱离环境
    vec3 lit = mix(color.rgb, color.rgb * vertexColor.rgb, 0.2);
    fragColor = linear_fog(vec4(lit, color.a) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
