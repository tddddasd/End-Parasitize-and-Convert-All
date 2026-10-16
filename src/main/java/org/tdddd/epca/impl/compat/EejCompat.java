package org.tdddd.epca.impl.compat;

import net.minecraftforge.fml.ModList;

/**
 * Soft dependency on the eej mod.
 *
 * <p>epca no longer requires eej: the altar integration only engages when eej is
 * present. This class deliberately references nothing from eej so that it can be
 * loaded unconditionally - everything eej-specific lives in
 * {@link EejAltarCompat}, which is only touched after {@link #loaded()} says
 * yes and therefore is never resolved when eej is missing.</p>
 */
public final class EejCompat {

    public static final String MOD_ID = "eej";

    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private EejCompat() {
    }

    public static boolean loaded() {
        return LOADED;
    }
}
