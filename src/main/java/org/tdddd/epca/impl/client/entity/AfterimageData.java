package org.tdddd.epca.impl.client.entity;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.BoneSnapshots;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

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

    public final Map<String, BoneSnapshot> bonePose;

    public AfterimageData(Vec3 position, float yRot, float yBodyRot, float yHeadRot,
                          float yRotO, float yBodyRotO, float yHeadRotO,
                          int spawnTick, int lifetime,
                          Map<String, BoneSnapshot> bonePose) {
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

    public static void captureRecursive(BoneSnapshots snapshots, GeoBone bone,
                                        Map<String, BoneSnapshot> out) {
        com.geckolib.animation.state.BoneSnapshot snap = snapshots.get(bone);
        if (snap != null) out.put(bone.name(), new BoneSnapshot(snap));
        for (GeoBone child : bone.children()) {
            captureRecursive(snapshots, child, out);
        }
    }
}
