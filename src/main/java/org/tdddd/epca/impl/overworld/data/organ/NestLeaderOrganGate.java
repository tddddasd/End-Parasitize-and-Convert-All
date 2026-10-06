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

/**
 * SPEC  1  D3 / D4
 *
 * <h2></h2>
 * <ul>
 *   <li><b>39 </b> <b>777</b> <b></b>
 *       </li>
 *   <li><b>33 </b><b></b></li>
 * </ul>
 *
 * <h2></h2>
 * <p> {@code epca:stage_i_beckon}
 * {@link StageIBeckon} {@code ModEntities#STAGE_I_BECKON}
 * {@code epca:stage_ii_beckon}{@link StageIIBeckon}
 *  {@code ModEntities#STAGE_II_BECKON}</p>
 *
 * <p><b>tier </b><code>ILink</code>
 * <code>AbstractLinkEntity</code>  tier//<b></b>
 *  {@code SynchedEntityData}  {@code checkStage*BeckonSpawnRules}
 *  tier{@code StageIIBeckon} =
 * {@code StageIBeckon}  {@code StageIIBeckon} = </p>
 *
 * <h2>777 </h2>
 * <p> 3
 * {@code [c-3, c+3]} 7
 * {@code Level#getEntitiesOfClass}
 *  777 </p>
 */
public final class NestLeaderOrganGate {

    /** 7 = 2 * 3 + 1 */
    public static final int RADIUS = 3;

    private NestLeaderOrganGate() {
    }

    /**
     *  7
     *
     * <p> {@code blockPosition()}  x=0.5
     * {@code [-2.5, 4.5]} 8  SPEC 777
     *  {@code [cx-3, cx+3]} 7
     * Y  3  2.25.0
     *  AABB </p>
     */
    public static AABB boxAround(LivingEntity nestLeader) {
        BlockPos center = nestLeader.blockPosition();
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        return new AABB(
                x - RADIUS, y - RADIUS, z - RADIUS,
                x + RADIUS + 1, y + RADIUS + 1, z + RADIUS + 1);
    }

    /**
     * 777 <b></b>
     *
     * <p> SPEC D339 </p>
     */
    public static boolean hasAnySummonPillar(LivingEntity nestLeader) {
        return !findAnySummonPillar(nestLeader).isEmpty();
    }

    /**
     * 777 <b></b>
     *
     * <p> SPEC D433 </p>
     */
    public static boolean hasTierTwoSummonPillar(LivingEntity nestLeader) {
        return !findTierTwoSummonPillars(nestLeader).isEmpty();
    }

    /**
     *
     *
     * <p>{@code isUnlocked}
     *
     * {@link #canModifyGroup(LivingEntity, OrganSlotGroup)} </p>
     *
     * @param nestLeader
     * @param group       true
     */
    public static boolean canModifyGroup(LivingEntity nestLeader, OrganSlotGroup group) {
        if (group == null || nestLeader == null) return false;
        if (!group.kind().isInnerGrid()) {
            // ""
            return true;
        }
        if (group == OrganSlotGroup.TORSO_INNER) {
            return hasAnySummonPillar(nestLeader);
        }
        if (group == OrganSlotGroup.HEAD_INNER) {
            return hasTierTwoSummonPillar(nestLeader);
        }
        return false;
    }

    /**
     *
     *
     * <p> STAGE 2
     * {@code isUnlocked(index)} </p>
     */
    public static boolean canModifySlot(LivingEntity nestLeader, NestLeaderOrganData data, int index) {
        if (data == null) return false;
        OrganSlotGroup group = data.groupOf(index);
        return canModifyGroup(nestLeader, group);
    }

    /**
     *  tick
     *  GUI
     *
     * <p> {@code nestLeader}  {@code Level#isClientSide}  true
     * " false"
     * </p>
     */
    public static Gates scan(LivingEntity nestLeader) {
        if (nestLeader == null || nestLeader.level().isClientSide) {
            return Gates.CLOSED;
        }
        Level level = nestLeader.level();
        AABB box = boxAround(nestLeader);

        List<StageIIBeckon> tierTwo =
                level.getEntitiesOfClass(StageIIBeckon.class, box, Entity::isAlive);
        if (!tierTwo.isEmpty()) {
            // ""
            return new Gates(true, true);
        }

        List<StageIBeckon> tierOne =
                level.getEntitiesOfClass(StageIBeckon.class, box, Entity::isAlive);
        return new Gates(!tierOne.isEmpty(), false);
    }

    /**
     * 777  +
     *
     * <p> {@code List<? extends Entity>}{@code List<StageIBeckon>}
     * {@code List<Entity>}</p>
     */
    public static List<? extends Entity> findAnySummonPillar(LivingEntity nestLeader) {
        if (nestLeader == null) return List.of();
        Level level = nestLeader.level();
        AABB box = boxAround(nestLeader);
        // StageIBeckon  StageIIBeckon  AbstractLinkEntity AbstractLinkEntity
        // Link "" Link
        List<Entity> result = new ArrayList<>();
        result.addAll(level.getEntitiesOfClass(StageIBeckon.class, box, Entity::isAlive));
        result.addAll(level.getEntitiesOfClass(StageIIBeckon.class, box, Entity::isAlive));
        return result;
    }

    /** 777  */
    public static List<StageIIBeckon> findTierTwoSummonPillars(LivingEntity nestLeader) {
        if (nestLeader == null) return List.of();
        return nestLeader.level().getEntitiesOfClass(
                StageIIBeckon.class, boxAround(nestLeader), Entity::isAlive);
    }

    /**
     *
     *
     * @param torsoInnerOpen 39
     * @param headInnerOpen  33
     */
    public record Gates(boolean torsoInnerOpen, boolean headInnerOpen) {
        /**  */
        public static final Gates CLOSED = new Gates(false, false);

        public boolean openFor(OrganSlotGroup group) {
            if (group == OrganSlotGroup.TORSO_INNER) return torsoInnerOpen;
            if (group == OrganSlotGroup.HEAD_INNER) return headInnerOpen;
            return true;
        }
    }
}

