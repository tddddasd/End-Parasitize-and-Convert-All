package org.tdddd.epca.impl.client.organ;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 *  GUI SPEC  1  B1 <b>H</b>
 *
 * <h2></h2>
 * <p>{@code KeyMapping}
 *  {@code ClientSetup#onRegisterKeyMappings(RegisterKeyMappingsEvent)}  Forge
 * mod "" {@link NestLeaderOrganClientInput}</p>
 *
 * <p> {@link KeyConflictContext#IN_GAME}
 * / {@code ClientHandlerI}
 * </p>
 */
@OnlyIn(Dist.CLIENT)
public final class NestLeaderOrganKeys {

    /**  - lang  */
    public static final String CATEGORY = "key.categories.epca";

    /**  GUI {@code H}{@link GLFW#GLFW_KEY_H} */
    public static final KeyMapping OPEN_ORGANS = new KeyMapping(
            "key.epca.open_organs",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    /**
     * SPEC  B  2
     *
     * <p><b></b> {@link InputConstants#UNKNOWN}
     *  {@code GLFW.GLFW_KEY_UNKNOWN} = -1""
     *  -  - - SPEC
     * </p>
     *
     * <p> {@link #OPEN_ORGANS}  {@link KeyConflictContext#IN_GAME}
     * "" {@link NestLeaderOrganClientInput#tick()}
     *  {@code NestLeaderOrganTeleportHandler}</p>
     */
    public static final KeyMapping TELEPORT = new KeyMapping(
            "key.epca.organ_teleport",
            KeyConflictContext.IN_GAME,
            // InputConstants.UNKNOWN == InputConstants.Type.KEYSYM.getOrCreate(-1)
            // == GLFW.GLFW_KEY_UNKNOWNInputConstants  158/237
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /**
     *
     *
     *
     * <p><b></b> {@link #TELEPORT}
     * {@link InputConstants#UNKNOWN}{@code GLFW.GLFW_KEY_UNKNOWN} = -1""
     *  -  - -
     * </p>
     *
     * <p> {@link KeyConflictContext#IN_GAME}
     * /""
     * {@link NestLeaderOrganClientInput#tick()}
     *
     * {@code NestLeaderDecomposeParasiteHandler}</p>
     */
    public static final KeyMapping DECOMPOSE_PARASITE = new KeyMapping(
            "key.epca.decompose_parasite",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    private NestLeaderOrganKeys() {
    }
}

