package org.tdddd.epca.impl.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.Optional;

public final class ArayaBgmManager {

    private static ArayaBgm current;

    private static Boolean assetPresent;

    private ArayaBgmManager() {
    }

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

    public static void stop() {
        if (current == null) {
            return;
        }
        ArayaBgm instance = current;
        current = null;
        try {
            Minecraft.getInstance().getSoundManager().stop(instance);
        } catch (Throwable ignored) {

        }
    }

    public static void invalidateAssetCache() {
        assetPresent = null;
    }

    public static boolean isPlaying() {
        return current != null;
    }

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

    public static boolean isAssetPresent() {
        if (assetPresent != null) {
            return assetPresent.booleanValue();
        }
        boolean present = false;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                net.minecraft.resources.ResourceLocation asset = new net.minecraft.resources.ResourceLocation(
                        ArayaBgm.ID.getNamespace(), "sounds/" + ArayaBgm.ID.getPath() + ".ogg");
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
