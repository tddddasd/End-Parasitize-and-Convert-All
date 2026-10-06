package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.BiomassSyncPacket;
import org.tdddd.epca.impl.overworld.data.BiomassManager;

/**
 * SPEC  1  D2
 *
 * <h2></h2>
 * <p><b></b>
 * {@link BiomassManager} {@code BiomassPoints}
 * {@code Player#getPersistentData()}
 * {@code BiomassEventHandler#onLivingDeath}
 * {@code BiomassEventHandler#onLivingAttack} /
 * {@code BeckonPlacementHandler}</p>
 *
 * <h2></h2>
 * <p> {@link BiomassSyncPacket} HUD
 * {@code BiomassHUD} / {@code BiomassClientData}</p>
 *
 * <h2></h2>
 * <p>SPEC
 * {@link #DEFAULT_UNLOCK_COST} = <b>5</b> / {@code BeckonPlacementHandler}
 *  15  1  1
 *  {@link #unlockCost(OrganSlotGroup)} </p>
 */
public final class OrganSlotUnlock {

    /** SPEC  5 */
    public static final int DEFAULT_UNLOCK_COST = 5;

    private OrganSlotUnlock() {
    }

    /**  0 */
    public static int unlockCost(OrganSlotGroup group) {
        return (group != null && group.kind().needsUnlock()) ? DEFAULT_UNLOCK_COST : 0;
    }

    /**  */
    public static int unlockCost(NestLeaderOrganData data, int index) {
        return unlockCost(data.groupOf(index));
    }

    /**
     *
     *
     * @param player
     * @param data
     * @param index
     * @return {@link Result#success()}  false  {@code reason}
     *
     */
    public static Result tryUnlock(Player player, NestLeaderOrganData data, int index) {
        if (player == null || data == null) {
            return Result.failure("数据不可用");
        }
        OrganSlotGroup group = data.groupOf(index);
        if (group == null) {
            return Result.failure("槽位不存在");
        }
        if (!group.kind().needsUnlock()) {
            return Result.failure(group.displayName() + "槽位本来就已解锁");
        }
        if (data.isUnlocked(index)) {
            return Result.failure("该槽位已解锁");
        }

        int cost = unlockCost(group);
        int points = BiomassManager.getBiomassPoints(player);
        if (points < cost) {
            return Result.failure("生物质点数不足（需要 " + cost + "，当前 " + points + "）");
        }

        //  setUnlocked
        BiomassManager.addBiomassPoints(player, -cost);
        data.setUnlocked(index, true);

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(serverPlayer, new BiomassSyncPacket(true, BiomassManager.getBiomassPoints(player)));
        }
        return Result.success(cost, BiomassManager.getBiomassPoints(player));
    }

    /** STAGE 2  */
    public static void sendFailure(Player player, Result result) {
        if (player != null && result != null && !result.success()) {
            player.displayClientMessage(Component.literal(result.reason()), true);
        }
    }

    /**  */
    public record Result(boolean success, int cost, int remainingPoints, String reason) {

        public static Result success(int cost, int remainingPoints) {
            return new Result(true, cost, remainingPoints, "");
        }

        public static Result failure(String reason) {
            return new Result(false, 0, -1, reason == null ? "解锁失败" : reason);
        }
    }
}

