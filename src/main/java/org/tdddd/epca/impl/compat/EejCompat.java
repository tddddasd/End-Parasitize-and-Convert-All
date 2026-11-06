package org.tdddd.epca.impl.compat;

import net.neoforged.fml.ModList;

public final class EejCompat {

    public static final String MOD_ID = "eej";

    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private EejCompat() {
    }

    public static boolean loaded() {
        return LOADED;
    }
}
