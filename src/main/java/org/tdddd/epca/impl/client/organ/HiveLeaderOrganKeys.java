package org.tdddd.epca.impl.client.organ;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class HiveLeaderOrganKeys {

    public static final String CATEGORY = "key.categories.epca";

    public static final KeyMapping OPEN_ORGANS = new KeyMapping(
            "key.epca.open_organs",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    public static final KeyMapping TELEPORT = new KeyMapping(
            "key.epca.organ_teleport",
            KeyConflictContext.IN_GAME,

            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    public static final KeyMapping DECOMPOSE_PARASITE = new KeyMapping(
            "key.epca.decompose_parasite",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    private HiveLeaderOrganKeys() {
    }
}
