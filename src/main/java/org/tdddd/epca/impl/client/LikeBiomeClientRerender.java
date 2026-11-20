package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Schedules a rebuild of the chunk sections whose biome tint changed.
 *
 * <p>Vanilla caches tinted block colours inside the chunk mesh, so a like biome change only becomes
 * visible after the affected 16x16x16 sections are marked dirty again.</p>
 *
 * <p>Marking sections dirty is deferred to a per tick queue instead of happening inside the packet
 * handler: a login snapshot or one bulk world edit can touch thousands of sections at once, and
 * rebuilding thousands of chunk meshes inside a single frame is a multi second freeze. Draining a
 * small budget per tick spreads the same work over a few seconds without any visible difference.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LikeBiomeClientRerender {

    /**
     * Sections rebuilt per client tick. Each rebuild is a full 16x16x16 mesh rebuild, so this is the
     * main knob between "tint updates instantly" and "never drop a frame".
     */
    private static final int MAX_SECTIONS_PER_TICK = 64;

    /** Hard cap of the queue, so a pathological stream of updates cannot grow it without bound. */
    private static final int MAX_QUEUED_SECTIONS = 262_144;

    private static final Set<Long> QUEUE = new LinkedHashSet<>();

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
        if (QUEUE.size() >= MAX_QUEUED_SECTIONS) {
            return;
        }
        QUEUE.addAll(sectionKeys);
    }

    /** Drops every queued section, used when the client leaves the level. */
    public static void clear() {
        QUEUE.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.levelRenderer == null) {
            if (!QUEUE.isEmpty()) {
                QUEUE.clear();
            }
            return;
        }
        if (QUEUE.isEmpty()) {
            return;
        }
        int processed = 0;
        Iterator<Long> iterator = QUEUE.iterator();
        while (iterator.hasNext() && processed < MAX_SECTIONS_PER_TICK) {
            long key = iterator.next();
            iterator.remove();
            minecraft.levelRenderer.setSectionDirty(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key));
            processed++;
        }
    }
}
