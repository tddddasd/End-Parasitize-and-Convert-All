package org.tdddd.epca.impl.client.entity;

import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.Map;

/**
 * Per-render "frozen ghost" state for the afterimage layer.
 *
 * <p><b>Why this exists.</b> The afterimage renders through the OWNER's live animatable, so GeckoLib's
 * normal pass overwrites any pose the layer applies: {@code EpcaGeoModel#setCustomAnimations}
 * recomputes the head bone from the owner's live {@code yBodyRot}, and the animation clips drive the
 * rest. The ghost therefore followed its owner's pose no matter what the captured snapshot held.</p>
 *
 * <p><b>How it is contained.</b> This is the single switch that marks "we are drawing an afterimage".
 * It is set only inside {@link EpcaGeoRenderer#renderModelWithAlpha} - the entry point the afterimage
 * layer alone calls - and cleared in a {@code finally}. Every other render path in the mod never
 * touches it, so non-ghost rendering takes the identical code path it always did. The flag is static
 * only because {@link EpcaGeoModel#setCustomAnimations} is invoked by GeckoLib and receives no
 * renderer reference; rendering is single-threaded on the client, so there is no cross-thread risk.</p>
 */
public final class FrozenGhostRender {

    /** True only for the duration of a ghost render pass. */
    private static boolean active;

    /** The captured bone pose to re-apply after the animation pass; null when inactive. */
    private static Map<String, AfterimageData.BoneSnapshot> pose;

    private FrozenGhostRender() {
    }

    public static boolean isActive() {
        return active;
    }

    /** Begin a ghost pass. Returns the previous state so the caller can nest/restore safely. */
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

    /**
     * Re-apply the captured pose to the model's bones, overwriting whatever the live animation pass
     * just wrote. Called from {@code EpcaGeoRenderer#preRender}, which GeckoLib runs AFTER
     * {@code setCustomAnimations} and BEFORE geometry - exactly the window needed.
     */
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
