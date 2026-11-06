package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIBeckon;
import org.tdddd.epca.impl.overworld.registry.entities.entity.link.StageIIBeckon;

import java.util.ArrayList;
import java.util.List;

public final class HiveLeaderOrganGate {

    public static final int RADIUS = 3;

    private HiveLeaderOrganGate() {
    }

    public static AABB boxAround(LivingEntity hiveLeader) {
        BlockPos center = hiveLeader.blockPosition();
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        return new AABB(
                x - RADIUS, y - RADIUS, z - RADIUS,
                x + RADIUS + 1, y + RADIUS + 1, z + RADIUS + 1);
    }

    public static boolean hasAnySummonPillar(LivingEntity hiveLeader) {
        return !findAnySummonPillar(hiveLeader).isEmpty();
    }

    public static boolean hasTierTwoSummonPillar(LivingEntity hiveLeader) {
        return !findTierTwoSummonPillars(hiveLeader).isEmpty();
    }

    public static boolean canModifyGroup(LivingEntity hiveLeader, OrganSlotGroup group) {
        if (group == null || hiveLeader == null) return false;
        if (!group.kind().isInnerGrid()) {

            return true;
        }
        if (group == OrganSlotGroup.TORSO_INNER) {
            return hasAnySummonPillar(hiveLeader);
        }
        if (group == OrganSlotGroup.HEAD_INNER) {
            return hasTierTwoSummonPillar(hiveLeader);
        }
        return false;
    }

    public static boolean canModifySlot(LivingEntity hiveLeader, HiveLeaderOrganData data, int index) {
        if (data == null) return false;
        OrganSlotGroup group = data.groupOf(index);
        return canModifyGroup(hiveLeader, group);
    }

    public static Gates scan(LivingEntity hiveLeader) {
        if (hiveLeader == null || hiveLeader.level().isClientSide) {
            return Gates.CLOSED;
        }
        Level level = hiveLeader.level();
        AABB box = boxAround(hiveLeader);

        List<StageIIBeckon> tierTwo =
                level.getEntitiesOfClass(StageIIBeckon.class, box, Entity::isAlive);
        if (!tierTwo.isEmpty()) {

            return new Gates(true, true);
        }

        List<StageIBeckon> tierOne =
                level.getEntitiesOfClass(StageIBeckon.class, box, Entity::isAlive);
        return new Gates(!tierOne.isEmpty(), false);
    }

    public static List<? extends Entity> findAnySummonPillar(LivingEntity hiveLeader) {
        if (hiveLeader == null) return List.of();
        Level level = hiveLeader.level();
        AABB box = boxAround(hiveLeader);

        List<Entity> result = new ArrayList<>();
        result.addAll(level.getEntitiesOfClass(StageIBeckon.class, box, Entity::isAlive));
        result.addAll(level.getEntitiesOfClass(StageIIBeckon.class, box, Entity::isAlive));
        return result;
    }

    public static List<StageIIBeckon> findTierTwoSummonPillars(LivingEntity hiveLeader) {
        if (hiveLeader == null) return List.of();
        return hiveLeader.level().getEntitiesOfClass(
                StageIIBeckon.class, boxAround(hiveLeader), Entity::isAlive);
    }

    public record Gates(boolean torsoInnerOpen, boolean headInnerOpen) {

        public static final Gates CLOSED = new Gates(false, false);

        public boolean openFor(OrganSlotGroup group) {
            if (group == OrganSlotGroup.TORSO_INNER) return torsoInnerOpen;
            if (group == OrganSlotGroup.HEAD_INNER) return headInnerOpen;
            return true;
        }
    }
}
