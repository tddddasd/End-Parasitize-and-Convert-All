package org.tdddd.epca.impl.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;

import java.util.Optional;

/**
 * Owns the single live {@link ArayaBossBgm} instance for the Araya NAMING EASTER EGG.
 *
 * <p>Mirrors {@link ArayaBgmManager}, but keys off the ENTITY rather than the staff-aura cache: the
 * theme belongs to a renamed infested enderman in Araya mode, and {@code isArayaMode} is a public field
 * that the client can read directly (the mode is entered from the entity's own custom name, which is
 * present on both sides).</p>
 *
 * <p>Started when such an enderman is within {@link ArayaConstants#BGM_RADIUS} of the listener, followed
 * while it stays within {@link ArayaConstants#BGM_STOP_RADIUS} (the two radii are the hysteresis), and
 * stopped on every exit path: entity dead, entity removed, no longer in Araya mode, out of range, level
 * changed, or the sound engine reporting the instance inactive.</p>
 */
public final class ArayaBossBgmManager {

    /** The one live track, or {@code null}. */
    private static ArayaBossBgm current;

    /** The entity the current track belongs to, so a different enderman restarts cleanly. */
    private static int currentEntityId = -1;

    /** Cached answer of "is the OGG installed", re-evaluated on every resource reload. */
    private static Boolean assetPresent;

    private ArayaBossBgmManager() {
    }

    /** Advances the BGM state; called once per client tick from {@code ClientEvents}. */
    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stop();
            return;
        }

        InfestedEnderman holder = nearestArayaEnderman(minecraft.player.position());
        if (holder == null) {
            stop();
            return;
        }

        // A different enderman (or the previous one came back after dying): restart on the new one.
        if (current != null && holder.getId() != currentEntityId) {
            stop();
        }

        Vec3 position = holder.position();
        if (current == null) {
            if (minecraft.player.position().distanceTo(position) <= ArayaConstants.BGM_RADIUS) {
                current = ArayaBossBgm.start(minecraft.getSoundManager(), position);
                currentEntityId = holder.getId();
            }
            return;
        }

        current.follow(position);
        if (current.listenerDistance() > ArayaConstants.BGM_STOP_RADIUS
                || !minecraft.getSoundManager().isActive(current)) {
            stop();
        }
    }

    /**
     * Stops and releases the loop. Safe at any time, including when nothing is playing: this is also the
     * hook the level change uses, so a dimension change or a disconnect can never leave it behind.
     */
    public static void stop() {
        currentEntityId = -1;
        if (current == null) {
            return;
        }
        ArayaBossBgm instance = current;
        current = null;
        try {
            Minecraft.getInstance().getSoundManager().stop(instance);
        } catch (Throwable ignored) {
            // A sound engine already shut down (main menu, disconnect) must not throw into the client
            // tick; the instance is dropped either way.
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

    /** The nearest live Araya-mode enderman to the given position, or {@code null}. */
    private static InfestedEnderman nearestArayaEnderman(Vec3 from) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        InfestedEnderman nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof InfestedEnderman enderman)) {
                continue;
            }
            // Dead, removed, or no longer in Araya mode (e.g. the name was changed back) => not a holder.
            if (!enderman.isAlive() || !enderman.isArayaMode()) {
                continue;
            }
            double distance = from.distanceTo(enderman.position());
            if (distance < best) {
                best = distance;
                nearest = enderman;
            }
        }
        return nearest;
    }

    /**
     * True when {@code assets/epca/sounds/araya_boss.ogg} is actually installed.
     *
     * <p>{@code sounds.json} ships the entry unconditionally, so without this test a client running a
     * stripped build (or a resource pack that removed the file) would play a missing-sound event every
     * time somebody renamed an enderman. One resource-manager lookup, cached until the next reload.</p>
     */
    public static boolean isAssetPresent() {
        if (assetPresent != null) {
            return assetPresent.booleanValue();
        }
        boolean present = false;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                ResourceLocation asset = new ResourceLocation(
                        ArayaBossBgm.ID.getNamespace(), "sounds/" + ArayaBossBgm.ID.getPath() + ".ogg");
                Optional<Resource> resource = minecraft.getResourceManager().getResource(asset);
                present = resource.isPresent();
                if (!present) {
                    epca.LOGGER.warn("[epca-araya] boss BGM asset missing, the naming-egg loop stays silent: {}",
                            ArayaBossBgm.ID);
                }
            }
        } catch (Throwable throwable) {
            epca.LOGGER.warn("[epca-araya] could not test for the boss BGM asset", throwable);
        }
        assetPresent = present;
        return present;
    }
}
