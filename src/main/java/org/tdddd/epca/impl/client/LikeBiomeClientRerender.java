package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;

import java.util.Collection;

/**
 * Client side helper that schedules a rebuild of the chunk sections whose biome tint changed.
 *
 * <p>Vanilla caches tinted block colours inside the chunk mesh, so a like biome change only becomes
 * visible after the affected 16x16x16 sections are marked dirty again.</p>
 */
public final class LikeBiomeClientRerender {

    /** Hard cap so a huge world edit cannot stall the client for a whole frame. */
    private static final int MAX_SECTIONS_PER_UPDATE = 8192;

    private LikeBiomeClientRerender() {
    }

    public static void invalidateSections(Collection<Long> sectionKeys) {
        if (sectionKeys.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.levelRenderer == null) {
            return;
        }
        int processed = 0;
        for (long key : sectionKeys) {
            if (processed++ >= MAX_SECTIONS_PER_UPDATE) {
                break;
            }
            minecraft.levelRenderer.setSectionDirty(SectionPos.x(key), SectionPos.y(key),
                    SectionPos.z(key));
        }
    }
}
