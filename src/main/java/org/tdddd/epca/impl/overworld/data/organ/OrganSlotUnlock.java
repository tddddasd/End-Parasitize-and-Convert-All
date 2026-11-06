package org.tdddd.epca.impl.overworld.data.organ;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.BiomassSyncPacket;
import org.tdddd.epca.impl.overworld.data.BiomassManager;

public final class OrganSlotUnlock {

    public static final int DEFAULT_UNLOCK_COST = 5;

    private OrganSlotUnlock() {
    }

    public static int unlockCost(OrganSlotGroup group) {
        return (group != null && group.kind().needsUnlock()) ? DEFAULT_UNLOCK_COST : 0;
    }

    public static int unlockCost(HiveLeaderOrganData data, int index) {
        return unlockCost(data.groupOf(index));
    }

    public static Result tryUnlock(Player player, HiveLeaderOrganData data, int index) {
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

        BiomassManager.addBiomassPoints(player, -cost);
        data.setUnlocked(index, true);

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(serverPlayer, new BiomassSyncPacket(true, BiomassManager.getBiomassPoints(player)));
        }
        return Result.success(cost, BiomassManager.getBiomassPoints(player));
    }

    public static void sendFailure(Player player, Result result) {
        if (player != null && result != null && !result.success()
                && player instanceof ServerPlayer serverPlayer) {

            serverPlayer.sendSystemMessage(Component.literal(result.reason()), true);
        }
    }

    public record Result(boolean success, int cost, int remainingPoints, String reason) {

        public static Result success(int cost, int remainingPoints) {
            return new Result(true, cost, remainingPoints, "");
        }

        public static Result failure(String reason) {
            return new Result(false, 0, -1, reason == null ? "解锁失败" : reason);
        }
    }
}
