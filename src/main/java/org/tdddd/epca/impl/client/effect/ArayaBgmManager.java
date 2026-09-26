package org.tdddd.epca.impl.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.Optional;

/**
 * Owns the single live {@link ArayaBgm} instance.
 *
 * <h2>Rules implemented here (all of them from the feature request)</h2>
 * <ul>
 *   <li><b>Start</b>: the holder has an active aura (its 天杀 counter is at or above
 *       {@link ArayaConstants#TIANSA_THRESHOLD}, which is the only case the server reports) and the local
 *       listener is inside {@link ArayaConstants#BGM_RADIUS}.</li>
 *   <li><b>Continue</b>: while the holder is still reported and the listener is still inside
 *       {@link ArayaConstants#BGM_STOP_RADIUS}. The two radii are the hysteresis that keeps the loop from
 *       stuttering for a player standing on the edge.</li>
 *   <li><b>Stop</b> ("距离持有者过远或没有该穷尽灭杖"): the listener is beyond the stop radius, no holder is
 *       reported any more (the staff was dropped, the counter fell below the threshold, or the holder
 *       logged out), or the client level changed. The instance goes to {@code SoundManager#stop}, so the
 *       channel and its streamed buffers are released immediately.</li>
 *   <li><b>Follow</b>: the instance carries the holder's position, refreshed every client tick, so the
 *       volume ramp tracks a moving holder.</li>
 * </ul>
 *
 * <p>Nothing here runs on the server: the server only says <i>who</i> has an aura, this class decides what
 * the local client hears.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>The only changes are {@code ResourceLocation} becoming {@link Identifier} and the resource-manager
 * lookup building that id with {@code fromNamespaceAndPath} instead of the 1.20.1 constructor.</p>
 */
public final class ArayaBgmManager {

    /** The one live track, or {@code null}. */
    private static ArayaBgm current;

    /** Cached answer of "is the OGG installed", re-evaluated on every resource reload. */
    private static Boolean assetPresent;

    private ArayaBgmManager() {
    }

    /** Advances the BGM state; called once per client tick from {@code ClientEvents}. */
    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stop();
            return;
        }

        ArayaClientCache.Holder holder = nearestHolder(minecraft.player.position());
        if (holder == null) {
            stop();
            return;
        }

        if (current == null) {
            if (minecraft.player.position().distanceTo(holder.position()) <= ArayaConstants.BGM_RADIUS) {
                current = ArayaBgm.start(minecraft.getSoundManager(), holder.position());
            }
            return;
        }

        current.follow(holder.position());
        if (current.listenerDistance() > ArayaConstants.BGM_STOP_RADIUS
                || !minecraft.getSoundManager().isActive(current)) {
            stop();
        }
    }

    /**
     * Stops and releases the loop. Safe to call at any time, including when nothing is playing: this is
     * also the hook the level change uses, so a teleport or a dimension change can never leave the track
     * playing behind.
     */
    public static void stop() {
        if (current == null) {
            return;
        }
        ArayaBgm instance = current;
        current = null;
        try {
            Minecraft.getInstance().getSoundManager().stop(instance);
        } catch (Throwable ignored) {
            // A sound engine that is already shut down (main menu, disconnect) must not throw into the
            // client tick; the instance is dropped either way.
        }
    }

    /** Drops the cached asset answer, so a new resource pack is picked up on the next tick. */
    public static void invalidateAssetCache() {
        assetPresent = null;
    }

    /** True while the loop is running; for diagnostics. */
    public static boolean isPlaying() {
        return current != null;
    }

    /** The nearest aura holder to the given position, or {@code null}. */
    private static ArayaClientCache.Holder nearestHolder(Vec3 from) {
        ArayaClientCache.Holder nearest = null;
        double best = Double.MAX_VALUE;
        for (ArayaClientCache.Holder holder : ArayaClientCache.holders()) {
            double distance = from.distanceTo(holder.position());
            if (distance < best) {
                best = distance;
                nearest = holder;
            }
        }
        return nearest;
    }

    /**
     * True when {@code assets/epca/sounds/araya.ogg} is actually installed.
     *
     * <p>{@code sounds.json} ships the entry unconditionally, so without this test a client running a
     * stripped build (or a resource pack that removed the file) would play a missing-sound event every
     * time somebody reached the threshold. The check is one resource-manager lookup, cached until the next
     * reload.</p>
     */
    public static boolean isAssetPresent() {
        if (assetPresent != null) {
            return assetPresent.booleanValue();
        }
        boolean present = false;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                Identifier asset = Identifier.fromNamespaceAndPath(ArayaBgm.ID.getNamespace(),
                        "sounds/" + ArayaBgm.ID.getPath() + ".ogg");
                Optional<Resource> resource = minecraft.getResourceManager().getResource(asset);
                present = resource.isPresent();
                if (!present) {
                    epca.LOGGER.warn("[epca-araya] BGM asset missing, the 天杀 loop stays silent: {}",
                            ArayaBgm.ID);
                }
            }
        } catch (Throwable throwable) {
            epca.LOGGER.warn("[epca-araya] could not test for the BGM asset", throwable);
        }
        assetPresent = Boolean.valueOf(present);
        return present;
    }
}
