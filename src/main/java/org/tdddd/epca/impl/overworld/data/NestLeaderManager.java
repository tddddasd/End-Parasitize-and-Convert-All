package org.tdddd.epca.impl.overworld.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

public class NestLeaderManager {
    public static NestLeaderSavedData getSavedData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        ServerLevel overworld = server.overworld();
        return NestLeaderSavedData.get(overworld);
    }

    public static boolean isNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        return data != null && data.isLeader(uuid);
    }

    public static boolean addNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        if (data != null) {
            data.addLeader(uuid);
            return true;
        }
        return false;
    }

    public static boolean removeNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        if (data != null) {
            data.removeLeader(uuid);
            return true;
        }
        return false;
    }
}