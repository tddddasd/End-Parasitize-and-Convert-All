#version 150

// 全屏四边形：顶点位置就是 0..1 的 UV，直接映射到 NDC 的最远平面（z = 1）。
//
// 关键：深度测试用 LEQUAL，所以这个四边形**只会画在深度缓冲仍然是清空值的地方**，
// 也就是"什么都没有画"的像素 —— 天空。地形、实体、云写在更近的深度上，
// 会自动把结界挡掉。于是它看起来就是"贴在天空上的裂缝"，而不是一层盖在屏幕上的滤镜。
//
// z = 1.0（最远平面）配合 LEQUAL：深度 == 1.0 的地方通过，任何更近的几何体都不通过。
in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 ndcPos;

void main() {
    ndcPos = Position.xy * 2.0 - 1.0;
    gl_Position = vec4(ndcPos, 1.0, 1.0);
}
