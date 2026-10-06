package org.tdddd.epca.impl.client.organ;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 *  GUI SPEC  1  B1 <b>H</b>
 *
 * <h2></h2>
 * <p>{@code KeyMapping}
 *  {@code ClientSetup#onRegisterKeyMappings(RegisterKeyMappingsEvent)}
 * mod "" {@link NestLeaderOrganClientInput}</p>
 *
 * <p> {@link KeyConflictContext#IN_GAME}
 * /</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2  API </h2>
 * <ul>
 *   <li><b></b>26.1.2  {@code KeyMapping}
 *       {@link KeyMapping.Category} {@link Identifier}
 *        _tmp_26src {@code KeyMapping.java}  98-141 / 301-333
 *        1.20.1  {@code String}{@code "key.categories.epca"}
 *        {@code Category#label()}
 *       {@code Component.translatable(id.toLanguageKey("key.category"))}
 *        {@code epca:keys}  lang  <b>{@code key.category.epca.keys}</b>
 *        zh_cn / en_us  datagen  JSON</li>
 *   <li>NeoForge  26.1.2
 *       {@code (String name, IKeyConflictContext, InputConstants.Type, int, Category)}
 *       IN_GAME + KEYSYM + </li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class NestLeaderOrganKeys {

    /**
     *  - 26.1.2  {@link KeyMapping.Category}
     *
     * <p> lang  {@code key.category.epca.keys}= {@code epca:keys}
     * - / E-PCA</p>
     *
     * <p><b> {@code Category.register}  {@code new Category(...)}</b>
     *  {@code KeyMapping.Category.SORT_ORDER}
     *  {@code KeyMapping#compareTo}  {@code SORT_ORDER.indexOf(category)}
     *  -1
     * 26.1.2  {@code @Deprecated}Neo
     * {@code RegisterKeyMappingsEvent#registerCategory}
     *  SORT_ORDER  NeoForge
     *  _tmp_26src {@code KeyMapping.java}  316-328 </p>
     */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("epca", "keys"));

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
     * <p><b></b> {@code GLFW.GLFW_KEY_UNKNOWN}= -1""
     *  -  - -
     *  SPEC </p>
     *
     * <p>"" {@link NestLeaderOrganClientInput#tick()}
     *  {@code NestLeaderOrganTeleportHandler}</p>
     */
    public static final KeyMapping TELEPORT = new KeyMapping(
            "key.epca.organ_teleport",
            KeyConflictContext.IN_GAME,
            // GLFW_KEY_UNKNOWN == InputConstants.UNKNOWN.getValue() == -1""
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /**
     *
     *
     *
     * <p><b></b> {@link #TELEPORT}
     * "" {@link NestLeaderOrganClientInput#tick()}
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

