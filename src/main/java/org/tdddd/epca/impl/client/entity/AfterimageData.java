package org.tdddd.epca.impl.client.entity;

import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable snapshot of an entity's render state at the moment an afterimage is spawned.
 * Records world position, yaw, and the full bone pose so the afterimage renders frozen
 * at its spawn moment regardless of subsequent animation.
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
    //  Bone snapshot — works with CoreGeoBone (GeckoLib interface)
    // ═══════════════════════════════════════════════════════════════

    public static class BoneSnapshot {
        public final float rotX, rotY, rotZ;
        public final float posX, posY, posZ;
        public final float scaleX, scaleY, scaleZ;

        public BoneSnapshot(CoreGeoBone bone) {
            this.rotX = bone.getRotX();
            this.rotY = bone.getRotY();
            this.rotZ = bone.getRotZ();
            this.posX = bone.getPosX();
            this.posY = bone.getPosY();
            this.posZ = bone.getPosZ();
            this.scaleX = bone.getScaleX();
            this.scaleY = bone.getScaleY();
            this.scaleZ = bone.getScaleZ();
        }

        public void applyTo(CoreGeoBone bone) {
            bone.setRotX(rotX);
            bone.setRotY(rotY);
            bone.setRotZ(rotZ);
            bone.setPosX(posX);
            bone.setPosY(posY);
            bone.setPosZ(posZ);
            bone.setScaleX(scaleX);
            bone.setScaleY(scaleY);
            bone.setScaleZ(scaleZ);
        }
    }

    /** Walk the bone tree recursively and capture every bone's current transform. */
    public static void captureRecursive(CoreGeoBone bone, Map<String, BoneSnapshot> out) {
        out.put(bone.getName(), new BoneSnapshot(bone));
        for (CoreGeoBone child : bone.getChildBones()) {
            captureRecursive(child, out);
        }
    }
}
