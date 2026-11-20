package org.tdddd.epca.impl.overworld.data.command;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * 路线几何工具：巡逻的 ±8 格走廊判定、回到最近路线点都用它。
 *
 * <p>距离按水平面（XZ）计算：寄生体可以上下坡，但“离开路线”指的是平面上的偏移，
 * 这样在山地/多层地形上巡逻不会因为高度差被误判为越界。</p>
 */
public final class HiveRouteMath {

    /** 巡逻允许离开路线的水平距离（格）。 */
    public static final double PATROL_CORRIDOR = 8.0;

    private HiveRouteMath() {
    }

    /** 点 (x,z) 到路线折线的最近距离（水平面）。闭合且点数 &gt; 2 时把末点 -> 首点也算一段。 */
    public static double distanceToRouteXZ(double x, double z, List<BlockPos> route, boolean closed) {
        if (route == null || route.isEmpty()) {
            return Double.MAX_VALUE;
        }
        if (route.size() == 1) {
            BlockPos only = route.get(0);
            return Math.hypot(x - (only.getX() + 0.5), z - (only.getZ() + 0.5));
        }
        double best = Double.MAX_VALUE;
        for (int i = 0; i < route.size() - 1; i++) {
            best = Math.min(best, pointSegmentDistance(x, z, route.get(i), route.get(i + 1)));
        }
        if (closed && route.size() > 2) {
            best = Math.min(best, pointSegmentDistance(x, z, route.get(route.size() - 1), route.get(0)));
        }
        return best;
    }

    /** 路线上离点 (x,z) 最近的路线点下标（用于被击退/追怪越界后回到路线）。 */
    public static int nearestRouteIndex(double x, double z, List<BlockPos> route) {
        if (route == null || route.isEmpty()) {
            return 0;
        }
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < route.size(); i++) {
            BlockPos pos = route.get(i);
            double dist = Math.hypot(x - (pos.getX() + 0.5), z - (pos.getZ() + 0.5));
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    public static BlockPos nearestRoutePoint(double x, double z, List<BlockPos> route) {
        if (route == null || route.isEmpty()) {
            return null;
        }
        return route.get(nearestRouteIndex(x, z, route));
    }

    private static double pointSegmentDistance(double px, double pz, BlockPos a, BlockPos b) {
        double ax = a.getX() + 0.5;
        double az = a.getZ() + 0.5;
        double bx = b.getX() + 0.5;
        double bz = b.getZ() + 0.5;
        double dx = bx - ax;
        double dz = bz - az;
        double lengthSq = dx * dx + dz * dz;
        if (lengthSq <= 1.0E-6) {
            return Math.hypot(px - ax, pz - az);
        }
        double t = ((px - ax) * dx + (pz - az) * dz) / lengthSq;
        t = Math.max(0.0, Math.min(1.0, t));
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }
}
