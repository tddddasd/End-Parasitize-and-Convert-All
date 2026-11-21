package org.tdddd.epca.impl.client.entity;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.BoneSnapshots;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Optional;

/**
 * 一条「残影」记录：生成时的位置 / 朝向 + 生成那一刻的<b>骨骼姿态快照</b>。
 *
 * <p>对应 1.20.1 的 {@code org.tdddd.epca.impl.client.entity.AfterimageData}
 * （那里快照是 {@code Map<String, BoneSnapshot>}，取值自 {@code CoreGeoBone}）。
 * 26.1.2 的 GeckoLib 5 里骨骼本身不再持有姿态（{@code GeoBone} 只有 pivot / baseRot 等静态数据），
 * 一帧的姿态完全由 {@code GeoBone.frameSnapshot} 指向的 {@link BoneSnapshot} 决定，
 * 而 {@code BoneSnapshot} 只在一次渲染 pass 内由 {@code RenderPassInfo#addBoneUpdater} 组装、
 * 用完即 {@code cleanup()}。因此快照不能再从骨骼对象上直接读，必须</p>
 * <ol>
 *   <li>在<b>渲染 pass 内</b>（有 {@link BoneSnapshots} 的地方）把每个骨骼的数值抄进本类；
 *       见 {@link BonePose#capture(BakedGeoModel, BoneSnapshots)}；</li>
 *   <li>渲染残影时再把这些数值写回<b>另一次</b> pass 的 {@link BoneSnapshots} 里，
 *       见 {@link BonePose#applyTo(BoneSnapshots)}（由 {@code EndermanAfterimageLayer} 注册为
 *       该 pass 的最后一个 {@code BoneUpdater}，从而覆盖动画/朝向等所有动态来源）。</li>
 * </ol>
 *
 * <p>数据结构刻意做成「扁平数组」而不是 {@code Map<String, BoneSnapshot>}：</p>
 * <ul>
 *   <li>每条残影只有一份，{@code bones × 9 float + bones × 1 byte}（末影人约 400B），
 *       远小于 map + 每骨骼一个对象的开销；</li>
 *   <li>寿命 20 tick，最多 12 条/实体，随 {@code AFTERIMAGES} 一起回收，不需要额外池化；</li>
 *   <li>名称用 {@code GeoBone.name()} 的既有字符串实例，不额外分配。</li>
 * </ul>
 */
public class AfterimageData {
    public final Vec3 position;

    public final float yRot;

    public final float yBodyRot;
    public final float yHeadRot;
    public final float yRotO;
    public final float yBodyRotO;
    public final float yHeadRotO;
    public final int spawnTick;
    public final int lifetime;

    /** 生成那一刻冻结下来的骨骼姿态；{@code null} 表示抓取失败（此时退化为「跟随本体实时姿态」）。 */
    public final BonePose bonePose;

    public AfterimageData(Vec3 position, float yRot, float yBodyRot, float yHeadRot,
                          float yRotO, float yBodyRotO, float yHeadRotO,
                          int spawnTick, int lifetime,
                          BonePose bonePose) {
        this.position = position;
        this.yRot = yRot;
        this.yBodyRot = yBodyRot;
        this.yHeadRot = yHeadRot;
        this.yRotO = yRotO;
        this.yBodyRotO = yBodyRotO;
        this.yHeadRotO = yHeadRotO;
        this.spawnTick = spawnTick;
        this.lifetime = lifetime;
        this.bonePose = bonePose;
    }

    public float getAlpha(int currentTick) {
        int age = currentTick - spawnTick;
        if (age < 0 || age >= lifetime) return 0.0F;
        return 1.0F - (float) age / (float) lifetime;
    }

    public boolean isAlive(int currentTick) {
        int age = currentTick - spawnTick;
        return age >= 0 && age < lifetime;
    }

    /**
     * 一份冻结的骨骼姿态：每根骨骼的 rotation / translation / scale 以及
     * GeckoLib 5 才有的 hidden / childrenHidden。
     *
     * <p>等价于 1.20.1 的 {@code AfterimageData.BoneSnapshot}（rotX/Y/Z、posX/Y/Z、scaleX/Y/Z）
     * 加上 26 的 {@code skipRender}/{@code skipChildrenRender} 两个开关。</p>
     */
    public static final class BonePose {

        /** 每根骨骼 9 个 float：rotX, rotY, rotZ, translateX, translateY, translateZ, scaleX, scaleY, scaleZ。 */
        private static final int STRIDE = 9;

        private static final byte FLAG_HIDDEN = 1;
        private static final byte FLAG_CHILDREN_HIDDEN = 2;

        private final String[] boneNames;
        private final float[] values;
        private final byte[] flags;

        private BonePose(String[] boneNames, float[] values, byte[] flags) {
            this.boneNames = boneNames;
            this.values = values;
            this.flags = flags;
        }

        public int boneCount() {
            return this.boneNames.length;
        }

        /**
         * 把冻结姿态写回一次渲染 pass 的骨骼快照。
         *
         * <p>必须作为该 pass 的<b>最后一个</b> {@code BoneUpdater} 注册：这样它跑在
         * {@code applyAnimationControllers}（动画）之后，写在快照上的就是"生成那一帧"的数值，
         * 而不是本帧动画算出来的数值 —— 这正是 1.20.1 里
         * {@code renderModelWithAlphaFrozenPose} → {@code FrozenGhostRender} 在
         * {@code preRender} 阶段二次覆写骨骼所做的事。</p>
         */
        public void applyTo(BoneSnapshots snapshots) {
            for (int i = 0; i < this.boneNames.length; i++) {
                int base = i * STRIDE;
                byte flag = this.flags[i];
                boolean hidden = (flag & FLAG_HIDDEN) != 0;
                boolean childrenHidden = (flag & FLAG_CHILDREN_HIDDEN) != 0;
                float rotX = this.values[base];
                float rotY = this.values[base + 1];
                float rotZ = this.values[base + 2];
                float posX = this.values[base + 3];
                float posY = this.values[base + 4];
                float posZ = this.values[base + 5];
                float scaleX = this.values[base + 6];
                float scaleY = this.values[base + 7];
                float scaleZ = this.values[base + 8];
                snapshots.ifPresent(this.boneNames[i], snapshot -> snapshot
                        .setRotation(rotX, rotY, rotZ)
                        .setTranslation(posX, posY, posZ)
                        .setScale(scaleX, scaleY, scaleZ)
                        .skipRender(hidden)
                        .skipChildrenRender(childrenHidden));
            }
        }

        /**
         * 在渲染 pass 内抓取当前帧的完整骨骼姿态。
         *
         * <p>{@code snapshots.get(name)} 会为尚未创建快照的骨骼补一个（数值为默认值 0/0/1），
         * 所以遍历整棵骨骼树就能拿到"动画 + 渲染器修正"之后、真正被用于绘制的那套数值。</p>
         *
         * @return 快照；模型里一根骨骼都没有时返回 {@code null}（调用方据此退化为实时姿态）
         */
        public static BonePose capture(BakedGeoModel model, BoneSnapshots snapshots) {
            if (model == null) return null;

            Builder builder = new Builder();
            for (GeoBone root : model.topLevelBones()) {
                captureRecursive(root, snapshots, builder);
            }
            return builder.size == 0 ? null : builder.build();
        }

        private static void captureRecursive(GeoBone bone, BoneSnapshots snapshots, Builder builder) {
            Optional<BoneSnapshot> snapshot = snapshots.get(bone.name());
            if (snapshot.isPresent()) {
                builder.add(bone.name(), snapshot.get());
            }
            for (GeoBone child : bone.children()) {
                captureRecursive(child, snapshots, builder);
            }
        }

        /** 只在生成残影时用一次的构建器（不参与每帧路径）。 */
        private static final class Builder {

            private String[] names = new String[16];
            private float[] values = new float[16 * STRIDE];
            private byte[] flags = new byte[16];
            private int size;

            private void add(String name, BoneSnapshot snapshot) {
                if (this.size == this.names.length) {
                    int grown = this.names.length * 2;
                    this.names = Arrays.copyOf(this.names, grown);
                    this.values = Arrays.copyOf(this.values, grown * STRIDE);
                    this.flags = Arrays.copyOf(this.flags, grown);
                }

                int index = this.size++;
                int base = index * STRIDE;
                this.names[index] = name;
                this.values[base] = snapshot.getRotX();
                this.values[base + 1] = snapshot.getRotY();
                this.values[base + 2] = snapshot.getRotZ();
                this.values[base + 3] = snapshot.getTranslateX();
                this.values[base + 4] = snapshot.getTranslateY();
                this.values[base + 5] = snapshot.getTranslateZ();
                this.values[base + 6] = snapshot.getScaleX();
                this.values[base + 7] = snapshot.getScaleY();
                this.values[base + 8] = snapshot.getScaleZ();
                this.flags[index] = (byte) ((snapshot.isHidden() ? FLAG_HIDDEN : 0)
                        | (snapshot.areChildrenHidden() ? FLAG_CHILDREN_HIDDEN : 0));
            }

            private BonePose build() {
                return new BonePose(
                        Arrays.copyOf(this.names, this.size),
                        Arrays.copyOf(this.values, this.size * STRIDE),
                        Arrays.copyOf(this.flags, this.size));
            }
        }
    }
}
