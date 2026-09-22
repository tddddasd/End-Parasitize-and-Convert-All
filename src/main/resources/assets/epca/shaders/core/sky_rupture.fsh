#version 150

// ═══════════════════════════════════════════════════════════════════════
//  EPCA 世界结界破损（Sky Rupture）
//
//  演出顺序
//  ─────────────────────────────────────────────────────────────────────
//  ① 天黑阶段（独立前置，时长加在最前面）
//     黑暗从天空顶部扩散下来直到地平线以下。此时破裂进度恒为 0
//     —— 这里有一道硬门 erupt，保证天黑期间**一丝裂纹都不会出现**。
//  ② 破裂阶段
//     天空盒十面八方陆续出现破裂点 → 裂纹沿碎片边界扩散 →
//     缝隙张开，露出**宇宙渲染**（星云 + 参考项目的星点粒子）。
//     最后阶段整片天空都碎掉，只剩宇宙。
//
//  破裂是怎么做的（★ 已从"火焰燃烧"改回"结界碎裂"）
//  ─────────────────────────────────────────────────────────────────────
//  曾经用过 "Burning Texture Fade" 那套噪声阈值溶解来表现燃烧，但观感不行：
//  多倍频噪声的频率一旦标定到"有质感"的尺度，整片天空就变成一片翻腾的
//  火焰 + 星空噪点，**什么都看不清**。所以退回结构化的做法：
//
//    裂纹 = Voronoi 的 **F2 − F1** 场。
//
//  为什么是这个场：
//    · 它 = 0 的等值线正好是一张**碎片边界网**（就是玻璃裂开的样子）
//    · 它是**连续**的 —— 跨过边界时 f1 与 f2 互换，差值仍然从 0 继续长，
//      所以不可能出现"直线接缝"（这正是之前抖动网格 + 比谁水位高踩的坑）
//    · 缝隙宽度随进度变宽 → 碎片缩小 → 最后整片天空消失
//
//  区域差异
//  ─────────────────────────────────────────────────────────────────────
//  哪一片先裂，由一个**低频连续噪声场** ignField 决定（不是逐格哈希）：
//  各处开裂时刻不同 → 破裂点十面八方陆续出现，而且不会跳变。
//
//  ⚠️ 尺度必须按"像素"标定
//  ─────────────────────────────────────────────────────────────────────
//  |p| = tan(θ/2) × SKY_SCALE —— 1 个 p 单位 ≈ 26°（地平线）～52°（天顶）的天空，
//  在 FOV 70 / 1080p 下约等于 400 ～ 800 像素。所以：
//    · 碎片尺寸 = 1/CRACK_SCALE = 0.25 p ≈ 100 ～ 200 px（一块碎片的尺度）
//    · 裂纹线宽 ≈ 0.06（Voronoi 单位）≈ 9 px（细亮线，看得清）
//  频率/尺度给错了，图案要么是一大团糊，要么是看不见的噪点。
//
//  地平线为什么不再畸变
//  ─────────────────────────────────────────────────────────────────────
//  图案空间用**球极投影** p = dir.xz / (1 + dir.y)：
//  |p| = tan(θ/2)，天顶 0、地平线 SKY_SCALE，往下继续光滑增大。
//  早期的 p = dir.xz × const 会把下半球**镜像**到上半球，
//  地平线正好是折痕（导数翻转）→ 那一条畸变线就是这么来的。
//
//  其他
//  ─────────────────────────────────────────────────────────────────────
//  · 所有图案都基于世界方向，配合深度遮罩只画在天空像素上。
//  · 亮度刻意压低：宇宙是"深空"而不是"灯火"，全部走 ACES 滚降。
// ═══════════════════════════════════════════════════════════════════════

in vec2 ndcPos;

uniform vec3 rayForward;
uniform vec3 rayRight;
uniform vec3 rayUp;

uniform float time;
uniform float progress;      // 破裂推进（天黑阶段恒为 0）
uniform float breakAmount;   // 0..1 阶段破损程度
uniform float fade;          // 0..1 总不透明度包络
uniform float seed;          // 每次触发随机
uniform vec2  patternOffset; // 裂纹场 / 噪声场的随机偏移（每次触发不同）

uniform vec3  rimColor;      // 结界自身的冷色能量光（阶段越高越紫）
uniform vec3  voidColor;     // 虚空色（黑暗/碎片本体）
uniform vec3  flashColor;    // 破碎瞬间的提亮色

// 黑暗吞噬（天空部分）：从顶部扩散下来的黑暗
uniform float skyDarkProgress;
uniform float skyDarkOpacity;

// 方块图集（12 张星点在里面）与它们的 UV 矩形（参考项目的 cosmicuvs）
uniform sampler2D Sampler0;
uniform mat2 cosmicuvs[12];

// 由 RenderType 机制自动上传，这里不直接用
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 ColorModulator;

out vec4 fragColor;

const int cosmiccount = 12;
const int cosmicoutof = 101;

const float PI = 3.14159265359;
/**
 * 球极投影缩放：|p| = tan(θ/2) × SKY_SCALE（θ 为距天顶的夹角）。
 * 地平线（θ = 90°）落在 |p| = SKY_SCALE = 2.2，天顶为 0。
 */
const float SKY_SCALE = 2.2;
/** 星场的球面网格密度（参考项目用 16） */
const float STAR_UVTILES = 16.0;
/** 星场壳层数（参考项目用 16 层，这里 12 层：与 12 张星点一一对应） */
const int STAR_SHELLS = 12;

/**
 * 裂纹网络的密度：碎片尺寸 = 1 / CRACK_SCALE。
 * 4.0 → 一块碎片约 0.25 p ≈ 100 ～ 200 px，屏幕上一眼看得到十几块碎片。
 */
const float CRACK_SCALE = 4.0;

// ── hash / 噪声 ───────────────────────────────────────────────────────

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

/** 二维哈希（Voronoi 特征点用） */
vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float hash31(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise3D(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash31(i + vec3(0.0, 0.0, 0.0)), hash31(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash31(i + vec3(0.0, 1.0, 0.0)), hash31(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash31(i + vec3(0.0, 0.0, 1.0)), hash31(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash31(i + vec3(0.0, 1.0, 1.0)), hash31(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

float fbm3(vec3 p) {
    float f = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 3; i++) {
        f += amp * noise3D(p);
        p *= 2.03;
        amp *= 0.5;
    }
    return f;
}

float vnoise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

/**
 * 多倍频值噪声（5 个倍频，振幅依次减半）。
 *
 * <p>{@code baseFreq} 是"每 p 单位多少个周期"，直接决定图案的尺度 ——
 * p 单位很大（400～800 px），所以 baseFreq 给小了就会变成一大团糊。
 * 这里只用来做**区域差异**（哪一片先裂），主结构取 1.6 ≈ 270–480 px。</p>
 */
float fbmNoise(vec2 p, float baseFreq) {
    return vnoise2(p * baseFreq)        * 0.500
         + vnoise2(p * baseFreq * 2.0)  * 0.250
         + vnoise2(p * baseFreq * 4.0)  * 0.125
         + vnoise2(p * baseFreq * 8.0)  * 0.062
         + vnoise2(p * baseFreq * 16.0) * 0.031;
}

/**
 * Voronoi 的 F1 / F2：到最近、次近特征点的距离（返回 {@code vec2(f1, f2)}）。
 *
 * <p>取两者的**差** {@code f2 - f1} 就得到裂纹场：它在碎片边界上恰好为 0，
 * 离开边界线性增大。这个差值是连续场（跨边界时 f1 与 f2 互换、差值仍从 0
 * 继续长），所以碎片之间<b>不会出现跳变、也不会出现直线接缝</b>。</p>
 */
vec2 voronoiEdge(vec2 x) {
    vec2 n = floor(x);
    vec2 f = fract(x);

    float f1 = 8.0;
    float f2 = 8.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 g = vec2(float(i), float(j));
            vec2 o = hash22(n + g);
            vec2 r = g + o - f;
            float d = dot(r, r);
            if (d < f1) {
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f2 = d;
            }
        }
    }
    return vec2(sqrt(f1), sqrt(f2));
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

/** 绕任意轴旋转（星场每层壳用不同轴 → 多层视差） */
mat3 rotAxis(vec3 axis, float angle) {
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;
    return mat3(
        oc * axis.x * axis.x + c,          oc * axis.x * axis.y - axis.z * s, oc * axis.z * axis.x + axis.y * s,
        oc * axis.x * axis.y + axis.z * s, oc * axis.y * axis.y + c,          oc * axis.y * axis.z - axis.x * s,
        oc * axis.z * axis.x - axis.y * s, oc * axis.y * axis.z + axis.x * s, oc * axis.z * axis.z + c
    );
}

/** 源覆盖合成 */
void over(inout vec3 dst, inout float da, vec3 src, float sa) {
    sa = clamp(sa, 0.0, 1.0);
    float na = sa + da * (1.0 - sa);
    if (na > 0.0001) {
        dst = (src * sa + dst * da * (1.0 - sa)) / na;
    }
    da = na;
}

/**
 * 由天顶平面参数反推世界方向（球极投影的逆）：
 * |q| = tan(θ/2) × SKY_SCALE → θ = 2·atan(|q| / SKY_SCALE)
 * 上下半球都能正确还原，所以不会出现"下半球镜像"的畸变。
 */
vec3 dirFromSkyPlane(vec2 q) {
    vec2 v = q / SKY_SCALE;
    float r = length(v);
    float theta = 2.0 * atan(r);
    vec2 xz = r > 1.0e-5 ? v / r : vec2(1.0, 0.0);
    return vec3(xz * sin(theta), cos(theta));
}

/** ACES 近似 tone mapping：亮部滚降，不糊成白块 */
vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

// ── 星云（移植 cosmic.fsh 的 getFbmNebula，改成在世界方向上算） ────────

vec3 nebula(vec3 d, float t) {
    float n1 = fbm3(d * 1.7 + vec3(t * 0.03, 0.0, t * 0.02));
    float n2 = fbm3(d * 3.1 + vec3(33.7, -17.2, t * 0.04));
    float k = smoothstep(0.08, 0.85, n1 * 0.65 + n2 * 0.30);

    vec3 c0 = vec3(0.020, 0.005, 0.050);
    vec3 c1 = vec3(0.070, 0.015, 0.170);
    vec3 c2 = vec3(0.140, 0.030, 0.300);
    vec3 c3 = vec3(0.210, 0.075, 0.400);
    vec3 col = k < 0.3 ? mix(c0, c1, k / 0.3)
             : (k < 0.65 ? mix(c1, c2, (k - 0.3) / 0.35) : mix(c2, c3, (k - 0.65) / 0.35));

    col += hsv2rgb(vec3(0.78 + n2 * 0.06 - 0.03, 0.5 + n1 * 0.3, k * 0.30)) * 0.18;
    return col;
}

// ── 星场：移植参考项目 RottenRuinsSplendiding 宇宙渲染的星点粒子 ──────

vec3 cosmicStars(vec3 dir, float t) {
    vec3 acc = vec3(0.0);

    for (int i = 0; i < STAR_SHELLS; i++) {
        int mult = STAR_SHELLS - i;
        int j = i + 7;
        float rand1 = (float(j * j * 4321 + j * 8)) * 2.0;
        float rand2 = (float((j + 1) * (j + 1) * (j + 1) * 239 + (j + 1) * 37)) * 3.6;
        float rand3 = rand1 * 347.4 + rand2 * 63.4;

        vec3 axis = normalize(vec3(sin(rand1), sin(rand2), cos(rand3)) + vec3(0.001));
        vec3 ray = rotAxis(axis, mod(rand3, 6.2831853)) * dir;

        float rawu = 0.5 + atan(ray.z, ray.x) * 0.1591549431;
        float rawv = 0.5 + asin(clamp(ray.y, -1.0, 1.0)) * 0.3183098862;

        float scale = float(mult) * 0.5 + 2.75;
        float u = rawu * scale;
        float v = (rawv + t * 0.004) * scale * 0.6;

        int tu = int(mod(floor(u * STAR_UVTILES), STAR_UVTILES));
        int tv = int(mod(floor(v * STAR_UVTILES), STAR_UVTILES));
        int position = (171 * tu + 489 * tv + 303 * (i + 31) + 17209) ^ 10;
        int symbol = position % cosmicoutof;

        // 旋转/翻转用哈希算，不用 pow(tu, tv)（pow(0,0) 在 GLSL 里未定义）
        float rotH = hash21(vec2(float(tu) + 0.5, float(tv) + 0.5) + float(i) * 17.3);
        int rotation = int(floor(rotH * 8.0));
        bool flip = false;
        if (rotation >= 4) {
            rotation -= 4;
            flip = true;
        }

        if (symbol >= 0 && symbol < cosmiccount) {
            float ru = clamp(mod(u, 1.0) * STAR_UVTILES - float(tu), 0.0, 1.0);
            float rv = clamp(mod(v, 1.0) * STAR_UVTILES - float(tv), 0.0, 1.0);
            if (flip) {
                ru = 1.0 - ru;
            }
            float oru = ru;
            float orv = rv;
            if (rotation == 1) {
                oru = 1.0 - rv;
                orv = ru;
            } else if (rotation == 2) {
                oru = 1.0 - ru;
                orv = 1.0 - rv;
            } else if (rotation == 3) {
                oru = rv;
                orv = 1.0 - ru;
            }

            float umin = cosmicuvs[symbol][0][0];
            float umax = cosmicuvs[symbol][1][0];
            float vmin = cosmicuvs[symbol][0][1];
            float vmax = cosmicuvs[symbol][1][1];
            vec2 spriteUv = vec2(umin * (1.0 - oru) + umax * oru,
                                 vmin * (1.0 - orv) + vmax * orv);

            vec4 texel = texture(Sampler0, spriteUv);

            // 星点贴图的红色通道是亮度；极点附近淡出（球面参数化会挤在一起）
            float a = texel.r * (0.5 + 1.0 / float(mult))
                    * (1.0 - smoothstep(0.15, 0.48, abs(rawv - 0.5)));

            // 冷白色温（参考项目 DEEP_SPACE 分支）
            vec3 starC = vec3(fract(rand1 * 0.123) * 0.4 + 0.6,
                              fract(rand2 * 0.456) * 0.3 + 0.7,
                              fract(rand3 * 0.789) * 0.3 + 0.7);
            starC *= vec3(1.0 + mod(rand1, 20.0) / 500.0,
                          1.0 + mod(rand2, 20.0) / 500.0,
                          1.0 + mod(rand3, 20.0) / 500.0);

            float twinkle = sin(t * 1.6 + rand1 * 0.1) * sin(t * 1.1 + rand2 * 0.15) * 0.4 + 0.6;
            float distFade = 1.0 - float(i) / float(STAR_SHELLS + 4);
            starC = starC * twinkle * distFade;
            starC += vec3(0.12, 0.06, 0.30) * (1.0 - distFade) * 0.3;

            acc += starC * a;
        }
    }
    return acc;
}

void main() {
    // ── 世界方向 → 球极投影参数（跨地平线无折痕）────────────────────
    vec3 dir = normalize(rayForward + rayRight * ndcPos.x + rayUp * ndcPos.y);
    vec2 p = dir.xz / (1.0 + dir.y) * SKY_SCALE;
    float t = time;

    // 硬门：天黑阶段 progress 恒为 0 → 一丝裂纹都不会出现
    float erupt = smoothstep(0.0, 0.02, progress);

    // ── 各处开裂时刻：低频**连续**噪声场（不是逐格哈希 → 不会跳变）────
    float ignField = fbmNoise(p + patternOffset, 1.6);
    float ign = 0.10 + 0.55 * ignField;
    float localProg = clamp((progress - ign) / max(1.0 - ign, 1.0e-3), 0.0, 1.0) * erupt;

    // ── 裂纹网络：Voronoi 的 F2 − F1（连续场 → 没有直线接缝）────────
    vec2 ve = voronoiEdge(p * CRACK_SCALE + patternOffset * 0.37);
    float edge = ve.y - ve.x;                       // 0 = 碎片边界

    // 缝宽 = 一条始终看得见的细线 + 随进度张开的豁口
    //   细线  0.06 Voronoi 单位 ≈ 9 px
    //   豁口  最终 1.75 单位 —— 必须大于 edge 的最大值（约 1.0～1.2，
    //         再叠上 ±30% 的 widthJitter 也还在它之上），
    //         这样最后阶段整片天空才会真的碎光，只剩宇宙
    float widthJitter = 0.70 + 0.60 * vnoise2(p * 5.0 + patternOffset);
    float stageScale = mix(0.10, 1.0, breakAmount);   // 阶段越低，豁口越小
    float lineW = 0.060 * smoothstep(0.0, 0.12, localProg);
    float holeW = 1.75 * pow(localProg, 2.2) * stageScale;
    float wGap = (lineW + holeW) * widthJitter;

    // 抗锯齿带宽必须**跟着缝宽走**，而且整块遮罩还要乘 erupt 硬门：
    //   aa 固定成 0.022 时，wGap = 0 也仍然存在一圈 ±0.022 的带子 ——
    //   碎片边界上（edge ≈ 0）会留下一条约 3 px、alpha ≈ 0.5 的"星空细线"，
    //   于是整张碎片网在触发的一瞬间就显出来了（天黑都还没黑完）★★ 这曾经是个真 bug
    float aa = max((lineW + holeW) * 0.30, 1.0e-4);
    float gapMask = (1.0 - smoothstep(wGap - aa, wGap + aa, edge)) * erupt;

    // 碎片边缘的结界能量边（冷色，贴在碎片一侧）
    // 末尾那项保证"还没裂开的地方不发光" —— 否则整张碎片网也会在开始的一瞬间
    // 全部亮起来，看起来像一层网／格子，而不是"陆续裂开"
    float rimW = clamp(wGap * 0.55 + 0.06, 0.02, 0.30);
    float rimBand = (smoothstep(wGap, wGap + aa * 2.0, edge)
                   * (1.0 - smoothstep(wGap, wGap + rimW, edge))
                   * smoothstep(0.0, 0.02, wGap)) * erupt;

    // ── 缝隙里露出的宇宙（视差用一张平滑方向场，不用"某个破裂点"）────
    vec2 warp = vec2(vnoise2(p * 0.6 + patternOffset), vnoise2(p * 0.6 + patternOffset + 31.0)) - 0.5;
    vec3 cosmosDir = dirFromSkyPlane(p + warp * 0.20 * breakAmount);
    vec3 cosmos = nebula(cosmosDir, t) * 0.85
                + cosmicStars(cosmosDir, t) * mix(0.95, 1.20, breakAmount);

    // ── 合成（从底到顶）──────────────────────────────────────────
    vec3 col = vec3(0.0);
    float alpha = 0.0;

    // ① 最底层：被吞噬后的黑暗天空 = 碎片本体
    {
        float frontY = mix(1.06, -1.06, clamp(skyDarkProgress, 0.0, 1.0));
        float frontSoft = mix(0.38, 0.05, clamp(skyDarkProgress, 0.0, 1.0));
        over(col, alpha, voidColor * 0.55,
             smoothstep(frontY - frontSoft, frontY + frontSoft, dir.y)
                     * clamp(skyDarkOpacity, 0.0, 1.0));
    }

    // ② 裂开的缝隙：露出宇宙渲染
    over(col, alpha, cosmos, gapMask);

    // ③ 碎片边缘的冷色能量光（结界还亮着的部分）
    over(col, alpha, rimColor * 1.25, rimBand * 0.55);

    // ④ 破碎瞬间的一点点提亮（极短、极弱，只为"断裂感"）
    float flash = (1.0 - smoothstep(0.0, 0.05, progress)) * erupt;
    over(col, alpha, flashColor, flash * 0.07);

    alpha *= clamp(fade, 0.0, 1.0);
    if (alpha < 0.002) {
        discard;
    }

    fragColor = vec4(aces(col * 1.15), clamp(alpha, 0.0, 1.0));
}
