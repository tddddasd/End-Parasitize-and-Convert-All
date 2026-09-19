package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncAllInfestedSourcesPacket;

public class InfestedBlockManager {
    public static void addInfestedBlock(ServerLevel level, BlockPos pos) {
        InfestedBlockSavedData data = InfestedBlockSavedData.get(level);
        data.add(pos);
        ModNetwork.sendToAll(new InfestedSourcePacket.AddInfestedSourcePacket(pos));
    }

    public static void removeInfestedBlock(ServerLevel level, BlockPos pos) {
        InfestedBlockSavedData data = InfestedBlockSavedData.get(level);
        data.remove(pos);
        ModNetwork.sendToAll(new InfestedSourcePacket.RemoveInfestedSourcePacket(pos));
    }

    public static void syncAllToPlayer(ServerPlayer player) {
        ServerLevel level = player.level();
        InfestedBlockSavedData data = InfestedBlockSavedData.get(level);
        SyncAllInfestedSourcesPacket packet = new SyncAllInfestedSourcesPacket(data.getAll());
        ModNetwork.sendToPlayer(player, packet);
    }
}