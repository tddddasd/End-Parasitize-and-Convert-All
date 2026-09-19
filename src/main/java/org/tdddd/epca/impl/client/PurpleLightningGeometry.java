package org.tdddd.epca.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4fc;

/**
 * 26.1.2 渲染管线下的「紫色闪电」几何体。
 *
 * <p><b>1.20.1 原版行为。</b> {@code LightningBoltRendererMixin} 在
 * {@code LightningBoltRenderer#render(...)} 的开头把「脚下是 {@code InfestedBlockInterface}」
 * 记进一个 {@code ThreadLocal<Boolean>}，然后 {@code @Redirect} 私有的静态辅助方法
 * {@code quad(Matrix4f, VertexConsumer, float, float, int, float, float, float r, float g, float b, ...)}
 * ——把它的第 7/8/9 个参数（原版恒为 {@code 0.45F, 0.45F, 0.5F}）换成 {@code 0.8F, 0.2F, 1.0F}。
 * alpha 一直是 {@code quad} 内部的 {@code 0.3F}，从未改动；几何体完全不变。
 *
 * <p><b>为什么不能照搬 {@code @Redirect quad}。</b> 26.1.2 里 {@code submit} 只负责算出 8 个
 * 横向偏移，真正的 {@code quad} 调用被搬进
 * {@code SubmitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> ...)}
 * 这个 lambda 里，也就是合成方法 {@code lambda$submit$0}。{@code submit} 自身的方法体里只有一条
 * {@code invokedynamic}（创建 lambda），没有 {@code quad} 的调用点，所以按 {@code method = "submit"}
 * 去 {@code @Redirect quad} 是找不到注入点的。
 *
 * <p><b>本实现。</b> 由 {@code LightningBoltRendererMixin} 在 {@code submit} 里
 * {@code @Redirect} 那次 {@code submitCustomGeometry} 调用：不是紫色的闪电<b>原样调用原来的
 * {@code CustomGeometryRenderer}</b>（即 {@code LightningBoltRenderer#lambda$submit$0} 绑定的实例，
 * 逐字节等同于原版），只有紫色闪电才换成这个类。本类的 {@code render} 逐行照抄
 * {@code LightningBoltRenderer} 的 lambda 方法体与 {@code quad} 方法体，只把颜色参数化，
 * 因此几何体（8 段偏移表、4 圈 × 3 层 × 每层若干 quad、{@code RandomSource} 的重建位置与消耗顺序、
 * {@code rr1}/{@code rr2} 的缩放）与原版完全一致，唯一差别是 RGB。
 *
 * <p>{@code RandomSource.createThreadLocalInstance(seed)} 是可重放的：原版在 {@code submit} 里
 * 建一个实例算偏移表，在 lambda 里每个 {@code r} 再各建一个新实例；这里用同一个 seed 在同样的位置
 * 重建，得到同样的序列。（这也是为什么不需要把偏移表从 {@code submit} 传进来。）
 */
public final class PurpleLightningGeometry implements SubmitNodeCollector.CustomGeometryRenderer {

    /** 原版 {@code LightningBoltRenderer} 写死的闪电颜色，逐字保留。 */
    public static final float VANILLA_RED = 0.45F;
    public static final float VANILLA_GREEN = 0.45F;
    public static final float VANILLA_BLUE = 0.5F;

    /** 1.20.1 混入替换后的颜色。 */
    public static final float PURPLE_RED = 0.8F;
    public static final float PURPLE_GREEN = 0.2F;
    public static final float PURPLE_BLUE = 1.0F;

    /** 原版 {@code quad} 内部的顶点 alpha，两个分支都不动。 */
    private static final float BOLT_ALPHA = 0.3F;

    private final long seed;
    private final float red;
    private final float green;
    private final float blue;

    /** 虫染紫：{@code (0.8, 0.2, 1.0)}。 */
    public PurpleLightningGeometry(long seed) {
        this(seed, PURPLE_RED, PURPLE_GREEN, PURPLE_BLUE);
    }

    public PurpleLightningGeometry(long seed, float red, float green, float blue) {
        this.seed = seed;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public void render(PoseStack.Pose pose, VertexConsumer buffer) {
        float[] xOffs = new float[8];
        float[] zOffs = new float[8];
        float xOff = 0.0F;
        float zOff = 0.0F;
        RandomSource random = RandomSource.createThreadLocalInstance(this.seed);

        for (int segment = 7; segment >= 0; segment--) {
            xOffs[segment] = xOff;
            zOffs[segment] = zOff;
            xOff += random.nextInt(11) - 5;
            zOff += random.nextInt(11) - 5;
        }

        float finalXOff = xOff;
        float finalZOff = zOff;
        Matrix4fc poseMatrix = pose.pose();

        for (int r = 0; r < 4; r++) {
            RandomSource randomx = RandomSource.createThreadLocalInstance(this.seed);

            for (int p = 0; p < 3; p++) {
                int hs = 7;
                int ht = 0;
                if (p > 0) {
                    hs = 7 - p;
                }

                if (p > 0) {
                    ht = hs - 2;
                }

                float xo0 = xOffs[hs] - finalXOff;
                float zo0 = zOffs[hs] - finalZOff;

                for (int h = hs; h >= ht; h--) {
                    float xo1 = xo0;
                    float zo1 = zo0;
                    if (p == 0) {
                        xo0 += randomx.nextInt(11) - 5;
                        zo0 += randomx.nextInt(11) - 5;
                    } else {
                        xo0 += randomx.nextInt(31) - 15;
                        zo0 += randomx.nextInt(31) - 15;
                    }

                    float rr1 = 0.1F + r * 0.2F;
                    if (p == 0) {
                        rr1 *= h * 0.1F + 1.0F;
                    }

                    float rr2 = 0.1F + r * 0.2F;
                    if (p == 0) {
                        rr2 *= (h - 1.0F) * 0.1F + 1.0F;
                    }

                    quad(poseMatrix, buffer, xo0, zo0, h, xo1, zo1, this.red, this.green, this.blue,
                            rr1, rr2, false, false, true, false);
                    quad(poseMatrix, buffer, xo0, zo0, h, xo1, zo1, this.red, this.green, this.blue,
                            rr1, rr2, true, false, true, true);
                    quad(poseMatrix, buffer, xo0, zo0, h, xo1, zo1, this.red, this.green, this.blue,
                            rr1, rr2, true, true, false, true);
                    quad(poseMatrix, buffer, xo0, zo0, h, xo1, zo1, this.red, this.green, this.blue,
                            rr1, rr2, false, true, false, false);
                }
            }
        }
    }

    /** {@code LightningBoltRenderer#quad} 的逐字副本，只把颜色改成参数。 */
    private static void quad(Matrix4fc pose, VertexConsumer buffer,
                             float xo0, float zo0, int h, float xo1, float zo1,
                             float boltRed, float boltGreen, float boltBlue,
                             float rr1, float rr2,
                             boolean px1, boolean pz1, boolean px2, boolean pz2) {
        buffer.addVertex(pose, xo0 + (px1 ? rr2 : -rr2), (float) (h * 16), zo0 + (pz1 ? rr2 : -rr2))
                .setColor(boltRed, boltGreen, boltBlue, BOLT_ALPHA);
        buffer.addVertex(pose, xo1 + (px1 ? rr1 : -rr1), (float) ((h + 1) * 16), zo1 + (pz1 ? rr1 : -rr1))
                .setColor(boltRed, boltGreen, boltBlue, BOLT_ALPHA);
        buffer.addVertex(pose, xo1 + (px2 ? rr1 : -rr1), (float) ((h + 1) * 16), zo1 + (pz2 ? rr1 : -rr1))
                .setColor(boltRed, boltGreen, boltBlue, BOLT_ALPHA);
        buffer.addVertex(pose, xo0 + (px2 ? rr2 : -rr2), (float) (h * 16), zo0 + (pz2 ? rr2 : -rr2))
                .setColor(boltRed, boltGreen, boltBlue, BOLT_ALPHA);
    }
}
