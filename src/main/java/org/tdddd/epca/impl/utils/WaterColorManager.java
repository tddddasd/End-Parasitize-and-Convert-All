package org.tdddd.epca.impl.utils;

import net.minecraft.world.phys.Vec3;

import java.util.*;

public class WaterColorManager {
    private static final Map<UUID, ContaminatedArea> contaminatedAreas = new HashMap<>();
    private static final float RADIUS = 2.5f; 

    public static void addContaminatedArea(UUID entityId, Vec3 center) {
        contaminatedAreas.put(entityId, new ContaminatedArea(center));
    }

    public static void removeContaminatedArea(UUID entityId) {
        contaminatedAreas.remove(entityId);
    }

    public static boolean isPositionContaminated(Vec3 position) {
        for (ContaminatedArea area : contaminatedAreas.values()) {
            if (area.contains(position)) {
                return true;
            }
        }
        return false;
    }

    private static class ContaminatedArea {
        private final Vec3 center;

        public ContaminatedArea(Vec3 center) {
            this.center = center;
        }

        public boolean contains(Vec3 position) {
            double dx = position.x - center.x;
            double dy = position.y - center.y;
            double dz = position.z - center.z;
            return dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS;
        }
    }
}