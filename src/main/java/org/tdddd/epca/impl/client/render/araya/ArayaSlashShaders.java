package org.tdddd.epca.impl.client.render.araya;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;

public final class ArayaSlashShaders {

    public static final Identifier SHADER_ID =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/araya_slash");

    public static final float REFRACTION_RATIO = ArayaConstants.SLASH_REFRACTION_RATIO;

    public static final float GLOW_STRENGTH = 1.0F;

    public static final float GLOW_RED = 1.0F;
    public static final float GLOW_GREEN = 1.0F;
    public static final float GLOW_BLUE = 1.0F;

    private ArayaSlashShaders() {
    }
}
