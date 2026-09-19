package org.tdddd.epca.impl.overworld.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NestLeaderClientCache {
    private static final Set<UUID> LEADERS = new HashSet<>();

    public static void updateLeaders(Set<UUID> leaders) {
        LEADERS.clear();
        LEADERS.addAll(leaders);
    }

    public static boolean isNestLeader(UUID uuid) {
        return LEADERS.contains(uuid);
    }
}