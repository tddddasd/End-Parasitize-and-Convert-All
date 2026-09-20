package org.tdddd.epca.impl.compat.jade;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;


public final class EPCAJadeIds {

    
    public static final Identifier DAMAGE_ADAPTATION_INFO =
            Identifier.fromNamespaceAndPath(epca.MODID, "damage_adaptation_info");

    
    public static final Identifier KILL_COUNT_INFO =
            Identifier.fromNamespaceAndPath(epca.MODID, "kill_count_info");

    
    public static final String NBT_KILL_COUNT = "EPCA_KillCount";

    
    public static final String NBT_DAMAGE_ADAPTATION = "EPCA_DamageAdaptation";

    private EPCAJadeIds() {
    }
}
