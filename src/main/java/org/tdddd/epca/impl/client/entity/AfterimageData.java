package org.tdddd.epca.impl.client.entity;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.BoneSnapshots;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Immutable snapshot of an entity's render state at the moment an afterimage is spawned.
 * Records world position, yaw, and (optionally) the bone pose so the afterimage renders
 * frozen at its spawn moment regardless of subsequent animation.
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>GeckoLib 4 exposed the live, mutable {@code CoreGeoBone} transform, so the old code could
 * read {@code getRotX()/getPosX()/getScaleX()} straight off the bone and write them back with
 * {@code setRotX(...)}. GeckoLib 5 makes bones immutable while rendering: the animated pose
 * only exists as a {@link BoneSnapshot}, and it can only be written back through a
 * {@code RenderPassInfo.BoneUpdater}. This class therefore snapshots GeckoLib 5
 * {@link BoneSnapshot}s and applies them via {@link BoneSnapshot#setRotX}/{@code setTranslation}/
 * {@code setScale}.</p>
 */
public class AfterimageData {
    public final Vec3 position;
    public final float yRot;
    public final int spawnTick;
    public final int lifetime;

    /** Frozen bone transforms captured at spawn time. Key = bone name. */
    public final Map<String, BoneSnapshot> bonePose;

    public AfterimageData(Vec3 position, float yRot, int spawnTick, int lifetime,
                          Map<String, BoneSnapshot> bonePose) {
        this.position = position;
        this.yRot = yRot;
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

    // ═══════════════════════════════════════════════════════════════
    //  Bone snapshot — GeckoLib 5 BoneSnapshot adapter
    // ═══════════════════════════════════════════════════════════════

    public static class BoneSnapshot {
        public final float rotX, rotY, rotZ;
        public final float posX, posY, posZ;
        public final float scaleX, scaleY, scaleZ;

        public BoneSnapshot(com.geckolib.animation.state.BoneSnapshot bone) {
            this.rotX = bone.getRotX();
            this.rotY = bone.getRotY();
            this.rotZ = bone.getRotZ();
            this.posX = bone.getTranslateX();
            this.posY = bone.getTranslateY();
            this.posZ = bone.getTranslateZ();
            this.scaleX = bone.getScaleX();
            this.scaleY = bone.getScaleY();
            this.scaleZ = bone.getScaleZ();
        }

        public void applyTo(com.geckolib.animation.state.BoneSnapshot bone) {
            bone.setRotation(rotX, rotY, rotZ);
            bone.setTranslation(posX, posY, posZ);
            bone.setScale(scaleX, scaleY, scaleZ);
        }
    }

    /** Walk the bone tree recursively and capture every bone's current transform. */
    public static void captureRecursive(BoneSnapshots snapshots, GeoBone bone,
                                        Map<String, BoneSnapshot> out) {
        com.geckolib.animation.state.BoneSnapshot snap = snapshots.get(bone);
        if (snap != null) out.put(bone.name(), new BoneSnapshot(snap));
        for (GeoBone child : bone.children()) {
            captureRecursive(snapshots, child, out);
        }
    }
}
