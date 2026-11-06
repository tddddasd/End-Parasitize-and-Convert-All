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

public final class ArayaBossBgmManager {

    private static ArayaBossBgm current;

    private static int currentEntityId = -1;

    private static Boolean assetPresent;

    private ArayaBossBgmManager() {
    }

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

        }
    }

    public static void invalidateAssetCache() {
        assetPresent = null;
    }

    public static boolean isPlaying() {
        return current != null;
    }

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
