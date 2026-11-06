package org.tdddd.epca.impl.client.entity;

import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.Map;

public final class FrozenGhostRender {

    private static boolean active;

    private static Map<String, AfterimageData.BoneSnapshot> pose;

    private FrozenGhostRender() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean begin(Map<String, AfterimageData.BoneSnapshot> frozenPose) {
        boolean previous = active;
        active = true;
        pose = frozenPose;
        return previous;
    }

    public static void end(boolean previous) {
        active = previous;
        if (!previous) {
            pose = null;
        }
    }

    public static void reapplyPose(java.util.List<? extends CoreGeoBone> roots) {
        if (!active || pose == null) {
            return;
        }
        for (CoreGeoBone root : roots) {
            applyRecursive(root, pose);
        }
    }

    private static void applyRecursive(CoreGeoBone bone, Map<String, AfterimageData.BoneSnapshot> target) {
        AfterimageData.BoneSnapshot snap = target.get(bone.getName());
        if (snap != null) {
            snap.applyTo(bone);
        }
        for (CoreGeoBone child : bone.getChildBones()) {
            applyRecursive(child, target);
        }
    }
}
